package com.senac.tsi.Formula1Api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// idempotencia nos POST: se o cliente mandar o mesmo Idempotency-Key de novo (ex.: clicou duas vezes
// ou a internet caiu e ele reenviou), a api devolve a resposta da primeira vez em vez de criar outro registro
// ordem 3: roda depois do ApiKeyFilter, entao so guarda requisicao de quem ja foi autenticado
@Component
@Order(3)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 255;
    private static final Duration TTL = Duration.ofHours(24);

    // uma entrada por (caminho, versao, cliente, chave). response == null quer dizer que a primeira ainda ta rodando
    private record Entry(byte[] requestHash, Instant createdAt, StoredResponse response) {
        boolean expired() {
            return createdAt.plus(TTL).isBefore(Instant.now());
        }
    }

    private record StoredResponse(int status, String contentType, String location, byte[] body) {
    }

    // ponytail: fica na memoria e some quando a api reinicia, se precisar durar mais vai pro banco ou Redis
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    // so POST, porque PUT e DELETE ja sao idempotentes por natureza
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER).trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            ProblemResponse.write(request, response, 400, "Idempotency-Key must have between 1 and " + MAX_KEY_LENGTH + " characters");
            return;
        }
        // a mesma chave em outra versao ou de outro cliente e outra requisicao
        String storeKey = request.getRequestURI() + "|" + apiVersion(request) + "|" + client(request) + "|" + key;

        Entry existing = entries.get(storeKey);
        if (existing != null && existing.expired()) {
            entries.remove(storeKey, existing);
            existing = null;
        }

        if (existing != null) {
            // chave repetida: nao roda o controller de novo, responde do que ta guardado
            byte[] hash = sha256(request.getInputStream().readAllBytes());
            if (!Arrays.equals(hash, existing.requestHash())) {
                ProblemResponse.write(request, response, 422, "Idempotency-Key was already used with a different request body");
            } else if (existing.response() == null) {
                ProblemResponse.write(request, response, 409, "A request with this Idempotency-Key is still being processed");
            } else {
                replay(response, existing.response());
            }
            return;
        }

        // le o corpo antes pra conseguir comparar se a proxima vier diferente
        byte[] body = request.getInputStream().readAllBytes();
        Entry inProgress = new Entry(sha256(body), Instant.now(), null);

        // putIfAbsent garante que, se duas chegarem juntas, so uma passa
        Entry raced = entries.putIfAbsent(storeKey, inProgress);
        if (raced != null) {
            ProblemResponse.write(request, response, 409, "A request with this Idempotency-Key is still being processed");
            return;
        }

        evictExpired();

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(new BodyReplayingRequest(request, body), cachedResponse);

            int status = cachedResponse.getStatus();
            // so guarda sucesso: se deu erro nada foi criado, entao o cliente pode tentar de novo
            if (status >= 200 && status < 300) {
                StoredResponse result = new StoredResponse(
                        status,
                        cachedResponse.getContentType(),
                        cachedResponse.getHeader(HttpHeaders.LOCATION),
                        cachedResponse.getContentAsByteArray());
                entries.replace(storeKey, inProgress, new Entry(inProgress.requestHash(), inProgress.createdAt(), result));
                stored = true;
            }
        } finally {
            if (!stored) {
                entries.remove(storeKey, inProgress);
            }
            cachedResponse.copyBodyToResponse();
        }
    }

    // devolve a resposta guardada e avisa com o header Idempotent-Replayed
    private void replay(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        if (stored.contentType() != null) {
            response.setContentType(stored.contentType());
        }
        if (stored.location() != null) {
            response.setHeader(HttpHeaders.LOCATION, stored.location());
        }
        response.setHeader("Idempotent-Replayed", "true");
        response.getOutputStream().write(stored.body());
    }

    private static String apiVersion(HttpServletRequest request) {
        String version = request.getHeader(ApiVersionConfig.HEADER);
        if (version == null) {
            version = request.getParameter(ApiVersionConfig.QUERY_PARAM);
        }
        return version == null ? "default" : version.trim();
    }

    // o ApiKeyFilter ja colocou a chave autenticada na requisicao, usa o id dela pra separar os clientes
    private static String client(HttpServletRequest request) {
        Object apiKey = request.getAttribute(ApiKeyFilter.CURRENT_KEY);
        return apiKey instanceof ApiKey current ? String.valueOf(current.getId()) : "anonymous";
    }

    private void evictExpired() {
        entries.values().removeIf(entry -> entry.response() != null && entry.expired());
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // o corpo ja foi lido pra calcular o hash, entao entrega uma copia dele pro controller
    private static class BodyReplayingRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BodyReplayingRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
