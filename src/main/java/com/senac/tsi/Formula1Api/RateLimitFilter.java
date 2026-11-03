package com.senac.tsi.Formula1Api;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// rate limit por plano, passou do limite volta 429:
//   - visitante (sem chave ou com chave invalida): 20 por minuto, contando por IP
//   - chave USER: 100 por minuto e chave ADMIN: 1000 por minuto, contando por chave (vale em qualquer IP)
// ordem 1: e o primeiro filtro. ele so olha a chave pra saber o plano, quem barra escrita sem chave e o ApiKeyFilter (2)
// chave invalida cai no limite de visitante, entao ficar chutando chave tambem e barrado
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ApiKeyService apiKeys;
    private final long visitorLimit;
    private final long userLimit;
    private final long adminLimit;

    // um balde de fichas por IP (visitante) ou por chave, cada requisicao gasta uma ficha
    // ponytail: fica na memoria, se um dia rodar mais de uma instancia precisa ir pro Redis
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ApiKeyService apiKeys,
                           @Value("${app.rate-limit.requests-per-minute}") long visitorLimit,
                           @Value("${app.rate-limit.user-requests-per-minute}") long userLimit,
                           @Value("${app.rate-limit.admin-requests-per-minute}") long adminLimit) {
        this.apiKeys = apiKeys;
        this.visitorLimit = visitorLimit;
        this.userLimit = userLimit;
        this.adminLimit = adminLimit;
    }

    private static Bucket newBucket(long requestsPerWindow) {
        // refillIntervally = janela fixa: as fichas voltam todas de uma vez quando o minuto acaba
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(requestsPerWindow)
                        .refillIntervally(requestsPerWindow, WINDOW))
                .build();
    }

    // preflight do CORS, swagger e console do H2 nao contam
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // descobre o plano pela chave; o nivel entra no nome do balde, assim mudar o nivel da chave muda o limite
        String rawKey = request.getHeader(ApiKeyFilter.HEADER);
        Optional<ApiKey> apiKey = rawKey == null || rawKey.isBlank() ? Optional.empty() : apiKeys.authenticate(rawKey.trim());
        String client = apiKey.map(key -> "key:" + key.getId() + ":" + key.getRole()).orElse("ip:" + clientIp(request));
        long limit = apiKey.map(key -> key.getRole() == ApiKeyRole.ADMIN ? adminLimit : userLimit).orElse(visitorLimit);

        Bucket bucket = buckets.computeIfAbsent(client, c -> newBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // toda resposta avisa o limite do plano e quanto ainda sobra
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(limit));

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            // Retry-After diz em quantos segundos da pra tentar de novo
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            ProblemResponse.write(request, response, 429, "Too many requests, try again in " + retryAfter + "s");
        }
    }

    // no render a api fica atras do cloudflare, que poe o IP de verdade no CF-Connecting-IP
    // o X-Forwarded-For nao serve: o primeiro valor dele quem escreve e o proprio cliente, ai era so mandar um IP falso por requisicao
    // ponytail: so e confiavel atras do cloudflare, num host sem ele troque pelo header que o proxy de la preenche
    private String clientIp(HttpServletRequest request) {
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (cloudflare != null && !cloudflare.isBlank()) {
            return cloudflare.trim();
        }
        return request.getRemoteAddr();
    }
}
