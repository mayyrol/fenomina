package com.fenomina.payroll_engine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProcesoLiquidacionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProcesoNotFound(
            ProcesoLiquidacionNotFoundException ex
    ) {
        log.warn("Proceso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NovedadNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNovedadNotFound(
            NovedadNotFoundException ex
    ) {
        log.warn("Novedad no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidStateTransitionException ex
    ) {
        log.warn("Transición de estado inválida: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProcesoYaCerradoException.class)
    public ResponseEntity<Map<String, Object>> handleProcesoYaCerrado(
            ProcesoYaCerradoException ex
    ) {
        log.warn("Operación no permitida sobre proceso: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmpleadoNoElegibleException.class)
    public ResponseEntity<Map<String, Object>> handleEmpleadoNoElegible(
            EmpleadoNoElegibleException ex
    ) {
        log.warn("Empleado no elegible: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ParametroNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleParametroNotFound(
            ParametroNoEncontradoException ex
    ) {
        log.error("Parámetro no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(CalculoNominaException.class)
    public ResponseEntity<Map<String, Object>> handleCalculoNomina(
            CalculoNominaException ex
    ) {
        log.error("Error en cálculo de nómina: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ValidacionNominaException.class)
    public ResponseEntity<Map<String, Object>> handleValidacionNomina(
            ValidacionNominaException ex
    ) {
        log.warn("Validación fallida - campo: {}, mensaje: {}",
                ex.getCampo(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Error de validación",
                "campo", ex.getCampo(),
                "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex
    ) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }

    @ExceptionHandler(SinCorreosRegistradosException.class)
    public ResponseEntity<Map<String, Object>> handleSinCorreosRegistrados(
            SinCorreosRegistradosException ex
    ) {
        log.warn("Envío bloqueado, sin correos registrados: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String mensaje
    ) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "mensaje", mensaje
        ));
    }
}