package com.senac.tsi.Formula1Api;

public class RaceResultNotFoundException extends RuntimeException {

    RaceResultNotFoundException(long id) {
        super("Could not find race result with ID: " + id);
    }
}
