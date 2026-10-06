package com.senac.tsi.Formula1Api;

// erro de equipe nao encontrada, o ApiExceptionAdvice transforma em 404
public class TeamNotFoundException extends RuntimeException {

    TeamNotFoundException(long id) {
        super("Could not find team with ID: " + id);
    }
}
