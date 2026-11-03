package com.senac.tsi.Formula1Api;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// trata os erros de todos os controllers num lugar so
// todo erro volta no mesmo formato (ProblemDetail, RFC 9457): title, status, detail e instance
// estender o ResponseEntityExceptionHandler faz os erros do proprio Spring (405, 415, versao invalida...)
// tambem sairem nesse formato
@RestControllerAdvice
public class ApiExceptionAdvice extends ResponseEntityExceptionHandler {

    // 404 - qualquer um dos NotFoundException, devolve a mensagem da exception
    @ExceptionHandler({TeamNotFoundException.class, TeamPrincipalNotFoundException.class,
            DriverNotFoundException.class, SponsorNotFoundException.class, RaceResultNotFoundException.class,
            ApiKeyNotFoundException.class})
    ProblemDetail notFoundHandler(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409 - regra de negocio barrou (ex.: apagar equipe que ainda tem piloto)
    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflictHandler(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 - o banco barrou: valor repetido num campo unique ou registro que ainda ta sendo usado
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrityHandler(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Operation conflicts with existing data (duplicated value or record still referenced)");
    }

    // 400 - ordenacao por um campo que nao existe, ex.: /drivers?sort=abc
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail sortHandler(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid sort: " + ex.getMessage());
    }

    // 500 - qualquer erro que escapou dos outros, assim nem ele sai fora do formato
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpectedHandler(Exception ex) {
        logger.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, try again later");
    }

    // 400 - o JSON nao passou nas validacoes, devolve cada campo com o erro dele em "errors"
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // 400 - JSON quebrado, data em formato errado ou valor que nao existe no enum
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "Malformed request body: check the JSON syntax, the dates (yyyy-MM-dd) and the enum values");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // 400 - valor de tipo errado na URL, ex.: /drivers/abc ou /results/status/WON
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "Invalid value '" + ex.getValue() + "' for parameter '" + name + "'");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
