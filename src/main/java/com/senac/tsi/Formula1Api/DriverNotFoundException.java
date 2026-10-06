package com.senac.tsi.Formula1Api;

public class DriverNotFoundException extends RuntimeException {

    DriverNotFoundException(long id) {
        super("Could not find driver with ID: " + id);
    }
}
