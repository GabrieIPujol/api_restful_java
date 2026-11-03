package com.senac.tsi.Formula1Api;

// erro de regra de negocio que bate com o estado atual (ex.: apagar equipe com piloto), vira 409
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
