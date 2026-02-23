package com.fenomina.auth.service;

import com.fenomina.auth.entity.Usuario;
import io.jsonwebtoken.Claims;

import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para generación y validación de JSON Web Tokens (JWT).
 * Maneja access tokens con información del usuario.
 */
public interface JwtService {

    /**
     * Genera un access token para un usuario.
     *
     * @param usuario Usuario autenticado
     * @return JWT firmado como String
     */
    String generateToken(Usuario usuario);

    String generateToken(Map<String, Object> extraClaims, Usuario usuario);

    String extractUsername(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    boolean isTokenValid(String token, Usuario usuario);

    boolean isTokenExpired(String token);
}
