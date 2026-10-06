package com.senac.tsi.Formula1Api;

// erro de chefe de equipe nao encontrado, o ApiExceptionAdvice transforma em 404
public class TeamPrincipalNotFoundException extends RuntimeException {

    TeamPrincipalNotFoundException(long id) {
        super("Could not find team principal with ID: " + id);
    }
}
