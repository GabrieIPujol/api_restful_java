package com.senac.tsi.Formula1Api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

// os filtros (rate limit, chave de api, idempotencia) rodam antes dos controllers, entao o
// ApiExceptionAdvice nao pega os erros deles. aqui eles escrevem o erro no mesmo formato do advice
final class ProblemResponse {

    private ProblemResponse() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        // sem o "type", igual o Spring faz quando ele e o padrao (about:blank)
        response.getWriter().write("{\"title\":\"" + HttpStatus.valueOf(status).getReasonPhrase() + "\""
                + ",\"status\":" + status
                + ",\"detail\":\"" + detail + "\""
                + ",\"instance\":\"" + request.getRequestURI() + "\"}");
    }
}
