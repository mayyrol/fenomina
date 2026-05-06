package com.fenomina.historicos_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoNoAutorizado(
            AccesoNoAutorizadoException ex) {
        log.warn("Acceso no autorizado: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Spring lanza esta excepción cuando @PreAuthorize rechaza la petición
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        log.warn("Acceso denegado por rol: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN,
                "No tiene permisos para acceder a este recurso");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String mensaje = "Parámetro '" + ex.getName() + "' con valor '"
                + ex.getValue() + "' no tiene el tipo esperado";
        log.warn("Error de tipo en parámetro: {}", mensaje);
        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
