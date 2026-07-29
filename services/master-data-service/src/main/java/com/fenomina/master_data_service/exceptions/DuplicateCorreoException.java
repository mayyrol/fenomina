package com.fenomina.master_data_service.exceptions;

public class DuplicateCorreoException extends RuntimeException {

    public DuplicateCorreoException(String message) {
        super(message);
    }
}
