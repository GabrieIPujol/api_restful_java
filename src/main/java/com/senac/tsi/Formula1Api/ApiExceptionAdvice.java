package com.senac.tsi.Formula1Api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

// trata os erros de todos os controllers num lugar so
// quando um controller lanca uma exception, o Spring cai no metodo certo daqui
@RestControllerAdvice
public class ApiExceptionAdvice {

    // 404 - qualquer um dos NotFoundException, devolve a mensagem da exception
    @ExceptionHandler({TeamNotFoundException.class, TeamPrincipalNotFoundException.class,
            DriverNotFoundException.class, SponsorNotFoundException.class, RaceResultNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFoundHandler(RuntimeException ex) {
        return ex.getMessage();
    }

    // 400 - o JSON nao passou nas validacoes, devolve cada campo com o erro dele
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validationHandler(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    // 400 - JSON quebrado ou valor que nao existe no enum
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String unreadableHandler(HttpMessageNotReadableException ex) {
        return "Malformed request body";
    }

    // 409 - o banco barrou: valor repetido num campo unique ou registro que ainda ta sendo usado
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflictHandler(DataIntegrityViolationException ex) {
        return "Operation conflicts with existing data (duplicated value or record still referenced)";
    }
}
