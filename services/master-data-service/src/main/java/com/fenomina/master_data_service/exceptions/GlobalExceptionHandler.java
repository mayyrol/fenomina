package com.fenomina.master_data_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Error de validación");
        response.put("errors", fieldErrors);
        response.put("path", request.getDescription(false).replace("uri=", ""));

        log.warn("Errores de validación: {}", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler({
            EmpresaNotFoundException.class,
            EmpleadoNotFoundException.class,
            ParametroGeneralNotFoundException.class,
            ConceptoNominaNotFoundException.class,
            ContratoConceptoNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFoundExceptions(
            RuntimeException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );

        log.warn("Recurso no encontrado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    @ExceptionHandler({
            DuplicateNitException.class,
            DuplicateDocumentException.class,
            DuplicateContratoConceptoException.class,
            DuplicateCorreoException.class
    })
    public ResponseEntity<Map<String, Object>> handleDuplicateExceptions(
            RuntimeException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );

        log.warn("Conflicto de duplicado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }


    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStateTransition(
            InvalidStateTransitionException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

        log.warn("Transición de estado inválida: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorageException(
            FileStorageException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request
        );

        log.error("Error de almacenamiento de archivo: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo excede el tamaño máximo permitido (5MB)",
                request
        );

        log.warn("Archivo excede tamaño máximo");

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación",
                request
        );

        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

        log.warn("Argumento ilegal: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

        log.warn("Estado ilegal: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                request
        );

        log.error("Error no manejado: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private Map<String, Object> buildErrorResponse(
            HttpStatus status,
            String message,
            WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return response;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        String message = "Error de integridad de datos";

        if (ex.getMessage() != null && ex.getMessage().contains("uq_documento_empresa")) {
            message = "Ya existe un empleado con ese tipo y número de documento en esta empresa";
        }

        log.warn("Violación de integridad: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.CONFLICT,
                message,
                request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }


}
