package com.fenomina.payroll_engine.exception;

public class ValidacionNominaException extends RuntimeException {

    private final String campo;

    public ValidacionNominaException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}