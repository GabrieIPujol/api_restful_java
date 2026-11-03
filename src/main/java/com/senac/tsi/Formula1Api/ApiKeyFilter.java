package com.senac.tsi.Formula1Api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// autenticacao por chave de API (header X-API-Key), roda antes de qualquer controller
//   - GET e publico, qualquer um le (mas uma chave errada da 401)
//   - POST, PUT e DELETE precisam de uma chave USER ou ADMIN
//   - /api-keys precisa de chave ADMIN (menos o /api-keys/me, que aceita qualquer chave)
// ordem 2: depois do rate limit (1) e antes da idempotencia (3)
@Component
@Order(2)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    // a chave encontrada fica guardada na requisicao pros controllers usarem
    public static final String CURRENT_KEY = "currentApiKey";

    private final ApiKeyService service;

    public ApiKeyFilter(ApiKeyService service) {
        this.service = service;
    }

    // swagger, console do H2 e preflight do CORS nao passam por aqui
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean keysEndpoint = uri.startsWith("/api-keys");
        boolean read = "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());

        String rawKey = request.getHeader(HEADER);
        boolean hasKey = rawKey != null && !rawKey.isBlank();

        // leitura dos dados e livre, mas se mandou chave ela tem que valer
        // senao quem errou a chave achava que estava no plano USER e caia no de visitante sem aviso
        if (read && !keysEndpoint && !hasKey) {
            chain.doFilter(request, response);
            return;
        }

        if (!hasKey) {
            ProblemResponse.write(request, response, 401, keysEndpoint
                    ? "Missing X-API-Key header: /api-keys requires an API key"
                    : "Missing X-API-Key header: POST, PUT and DELETE require an API key");
            return;
        }

        Optional<ApiKey> apiKey = service.authenticate(rawKey.trim());
        if (apiKey.isEmpty()) {
            ProblemResponse.write(request, response, 401, "Invalid or revoked API key");
            return;
        }

        // gerenciar chaves e coisa de ADMIN, o /me qualquer um pode ver
        if (keysEndpoint && !uri.equals("/api-keys/me") && apiKey.get().getRole() != ApiKeyRole.ADMIN) {
            ProblemResponse.write(request, response, 403, "This operation requires an ADMIN API key");
            return;
        }

        request.setAttribute(CURRENT_KEY, apiKey.get());
        chain.doFilter(request, response);
    }
}
