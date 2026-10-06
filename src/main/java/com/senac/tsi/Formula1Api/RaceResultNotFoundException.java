package com.senac.tsi.Formula1Api;

// erro de resultado nao encontrado, o ApiExceptionAdvice transforma em 404
public class RaceResultNotFoundException extends RuntimeException {

    RaceResultNotFoundException(long id) {
        super("Could not find race result with ID: " + id);
    }
}
