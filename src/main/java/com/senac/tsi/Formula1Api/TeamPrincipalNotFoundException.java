package com.senac.tsi.Formula1Api;

public class TeamPrincipalNotFoundException extends RuntimeException {

    TeamPrincipalNotFoundException(long id) {
        super("Could not find team principal with ID: " + id);
    }
}
