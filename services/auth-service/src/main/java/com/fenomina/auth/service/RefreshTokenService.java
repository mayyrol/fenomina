package com.fenomina.auth.service;

import com.fenomina.auth.entity.RefreshToken;
import com.fenomina.auth.entity.Usuario;

import java.util.Optional;

/**
 * Servicio para gestión de Refresh Tokens.
 * Los refresh tokens permiten renovar access tokens sin requerir login.
 */
public interface RefreshTokenService {

    RefreshToken createRefreshToken(Usuario usuario, String ipAddress, String userAgent);

    Optional<RefreshToken> validateRefreshToken(String token);

    void revokeAllUserTokens(Long usuarioId);

    void revokeToken(String token);

    void deleteExpiredTokens();

    boolean hasReachedMaxSessions(Long usuarioId);
}