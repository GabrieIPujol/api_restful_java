package com.senac.tsi.Formula1Api;

// erro de patrocinador nao encontrado, o ApiExceptionAdvice transforma em 404
public class SponsorNotFoundException extends RuntimeException {

    SponsorNotFoundException(long id) {
        super("Could not find sponsor with ID: " + id);
    }
}
