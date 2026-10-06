package com.senac.tsi.Formula1Api;

public class TeamNotFoundException extends RuntimeException {

    TeamNotFoundException(long id) {
        super("Could not find team with ID: " + id);
    }
}
