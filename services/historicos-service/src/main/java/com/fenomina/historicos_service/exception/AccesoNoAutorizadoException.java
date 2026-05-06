package com.fenomina.historicos_service.exception;

public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException(String message) {
        super(message);
    }
}