package com.fenomina.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de propiedades JWT leídas desde application.properties.
 * Centraliza todas las configuraciones relacionadas con tokens.
 */
@Configuration
@Getter
public class JwtConfig {

    /**
     * Secreto usado para firmar los JWT.
     * CRÍTICO: Debe ser un string largo y aleatorio (256+ caracteres).
     * En producción, usar variable de entorno: ${JWT_SECRET}
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Tiempo de expiración del access token en milisegundos.
     * Default: 3600000 ms = 1 hora
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Tiempo de expiración del refresh token en milisegundos.
     * Default: 604800000 ms = 7 días
     */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Nombre del header HTTP donde se envía el token.
     * Standard: "Authorization"
     */
    public String getTokenHeader() {
        return "Authorization";
    }

    /**
     * Prefijo del token en el header.
     * Standard: "Bearer "
     */
    public String getTokenPrefix() {
        return "Bearer ";
    }

    /**
     * Issuer del token (emisor).
     * Identifica quién generó el token.
     */
    public String getIssuer() {
        return "fenomina-auth-service";
    }
}
