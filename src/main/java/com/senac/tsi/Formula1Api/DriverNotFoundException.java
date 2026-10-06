package com.senac.tsi.Formula1Api;

// erro de piloto nao encontrado, o ApiExceptionAdvice transforma em 404
public class DriverNotFoundException extends RuntimeException {

    DriverNotFoundException(long id) {
        super("Could not find driver with ID: " + id);
    }
}
