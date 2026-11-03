package com.senac.tsi.Formula1Api;

// erro de chave de API nao encontrada, o ApiExceptionAdvice transforma em 404
public class ApiKeyNotFoundException extends RuntimeException {

    ApiKeyNotFoundException(long id) {
        super("Could not find API key with ID: " + id);
    }
}
