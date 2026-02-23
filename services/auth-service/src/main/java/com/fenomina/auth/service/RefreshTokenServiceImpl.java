package com.fenomina.auth.service;

import com.fenomina.auth.config.JwtConfig;
import com.fenomina.auth.entity.RefreshToken;
import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    /**
     * Límite de sesiones activas por usuario.
     */
    private static final int MAX_ACTIVE_SESSIONS = 5;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Usuario usuario, String ipAddress, String userAgent) {
        // Verificar límite de sesiones
        if (hasReachedMaxSessions(usuario.getUsuarioId())) {
            log.warn("Usuario {} alcanzó el límite de {} sesiones activas",
                    usuario.getUserName(), MAX_ACTIVE_SESSIONS);
            // Revocar el token más antiguo
            revokeOldestToken(usuario.getUsuarioId());
        }

        // Crear nuevo refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .usuario(usuario)
                .expiresAt(calculateExpirationDate())
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        log.info("Refresh token creado para usuario: {} desde IP: {}",
                usuario.getUserName(), ipAddress);

        return savedToken;
    }

    @Override
    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            log.warn("Intento de usar refresh token inexistente: {}", token);
            return Optional.empty();
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        // Validar que no esté revocado
        if (refreshToken.getRevoked()) {
            log.warn("Intento de usar refresh token revocado: {} - Usuario: {}",
                    token, refreshToken.getUsuario().getUserName());
            return Optional.empty();
        }

        // Validar que no haya expirado
        if (refreshToken.isExpired()) {
            log.warn("Intento de usar refresh token expirado: {} - Usuario: {}",
                    token, refreshToken.getUsuario().getUserName());
            return Optional.empty();
        }

        log.debug("Refresh token validado correctamente para usuario: {}",
                refreshToken.getUsuario().getUserName());

        return Optional.of(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long usuarioId) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(usuarioId);
        log.info("Revocados {} refresh tokens del usuario ID: {}", revokedCount, usuarioId);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);

            log.info("Refresh token revocado: {} - Usuario: {}",
                    token, refreshToken.getUsuario().getUserName());
        } else {
            log.warn("Intento de revocar refresh token inexistente: {}", token);
        }
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        try {
            refreshTokenRepository.deleteExpiredTokens();
            log.info("Tokens expirados eliminados de la base de datos");
        } catch (Exception e) {
            log.error("Error al eliminar tokens expirados", e);
        }
    }

    @Override
    public boolean hasReachedMaxSessions(Long usuarioId) {
        long activeTokens = refreshTokenRepository.countActiveTokensByUsuario(usuarioId);
        return activeTokens >= MAX_ACTIVE_SESSIONS;
    }

    private LocalDateTime calculateExpirationDate() {
        long expirationMillis = jwtConfig.getRefreshExpiration();
        long expirationSeconds = expirationMillis / 1000;
        return LocalDateTime.now().plusSeconds(expirationSeconds);
    }

    private void revokeOldestToken(Long usuarioId) {
        // Buscar el token más antiguo activo
        refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUsuario().getUsuarioId().equals(usuarioId))
                .filter(token -> !token.getRevoked())
                .min((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                .ifPresent(oldestToken -> {
                    oldestToken.setRevoked(true);
                    refreshTokenRepository.save(oldestToken);
                    log.info("Token más antiguo revocado para usuario ID: {}", usuarioId);
                });
    }
}
