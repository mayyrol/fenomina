package com.fenomina.auth.service;

import com.fenomina.auth.config.JwtConfig;
import com.fenomina.auth.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementación del servicio JWT.
 * Usa la librería jjwt (io.jsonwebtoken) para crear y validar tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(Usuario usuario) {
        return generateToken(new HashMap<>(), usuario);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, Usuario usuario) {
        long currentTimeMillis = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("userId", usuario.getUsuarioId());
        claims.put("rol", usuario.getRolUsuario().name());
        claims.put("empresaId", usuario.getFkIdEmpresa()); // Puede ser null
        claims.put("nombreCompleto", usuario.getNombresUsuario() + " " + usuario.getApellidosUsuario());

        return Jwts.builder()
                .claims(claims)
                .subject(usuario.getUserName()) // Subject = username
                .issuedAt(new Date(currentTimeMillis)) // Fecha de emisión
                .expiration(new Date(currentTimeMillis + jwtConfig.getExpiration())) // Expiración
                .issuer(jwtConfig.getIssuer()) // Emisor
                .signWith(getSigningKey()) // Firma con HMAC-SHA256
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public Long extractEmpresaId(String token) {
        return extractClaim(token, claims -> claims.get("empresaId", Long.class));
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Verifica la firma
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public boolean isTokenValid(String token, Usuario usuario) {
        final String username = extractUsername(token);
        return (username.equals(usuario.getUserName())) && !isTokenExpired(token);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}