package com.fenomina.auth.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción base para errores de autenticación y autorización.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AuthException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Factory methods para excepciones comunes

    public static AuthException invalidCredentials() {
        return new AuthException(
                "Credenciales inválidas. Username o contraseña incorrectos.",
                HttpStatus.UNAUTHORIZED,
                "AUTH_001"
        );
    }

    public static AuthException usuarioNoEncontrado(String username) {
        return new AuthException(
                "Usuario no encontrado: " + username,
                HttpStatus.NOT_FOUND,
                "AUTH_002"
        );
    }

    public static AuthException usuarioBloqueado() {
        return new AuthException(
                "Usuario bloqueado por intentos fallidos. Intente nuevamente en 15 minutos o contacte al administrador.",
                HttpStatus.FORBIDDEN,
                "AUTH_003"
        );
    }

    public static AuthException tokenInvalido() {
        return new AuthException(
                "Token inválido o expirado. Por favor, inicie sesión nuevamente.",
                HttpStatus.UNAUTHORIZED,
                "AUTH_004"
        );
    }

    public static AuthException passwordPolicy(String mensaje) {
        return new AuthException(
                "Política de contraseñas no cumplida: " + mensaje,
                HttpStatus.BAD_REQUEST,
                "AUTH_005"
        );
    }

    public static AuthException usuarioYaExiste(String username) {
        return new AuthException(
                "El username '" + username + "' ya está en uso.",
                HttpStatus.CONFLICT,
                "AUTH_006"
        );
    }

    public static AuthException numeroIdentificacionYaExiste(String numIdenti) {
        return new AuthException(
                "El número de identificación '" + numIdenti + "' ya está registrado.",
                HttpStatus.CONFLICT,
                "AUTH_007"
        );
    }

    public static AuthException accesoNoAutorizado() {
        return new AuthException(
                "No tiene permisos para realizar esta acción.",
                HttpStatus.FORBIDDEN,
                "AUTH_008"
        );
    }
}
