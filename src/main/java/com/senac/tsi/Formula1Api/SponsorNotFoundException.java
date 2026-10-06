package com.senac.tsi.Formula1Api;

public class SponsorNotFoundException extends RuntimeException {

    SponsorNotFoundException(long id) {
        super("Could not find sponsor with ID: " + id);
    }
}
