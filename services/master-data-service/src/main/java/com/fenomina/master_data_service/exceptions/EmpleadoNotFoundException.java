package com.fenomina.master_data_service.exceptions;

public class EmpleadoNotFoundException extends RuntimeException {

    public EmpleadoNotFoundException(String message) {
        super(message);
    }
}