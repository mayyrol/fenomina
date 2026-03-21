package com.fenomina.auth.config;

public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("Esta es una clase de constantes");
    }

    // Bloqueo de usuario
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    // Sesiones
    public static final int MAX_ACTIVE_SESSIONS = 5;

    // Refresh token rotation
    public static final boolean REFRESH_TOKEN_ROTATION_ENABLED = true;
}
