package com.fenomina.auth.scheduler;

import com.fenomina.auth.repository.AuditLogRepository;
import com.fenomina.auth.repository.RefreshTokenRepository;
import com.fenomina.auth.repository.UsuarioRepository;
import com.fenomina.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    // ── 1. Tokens expirados ──────────────────────────────────────────────────
    // Todos los días a las 3:00 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpieza de tokens expirados");
        try {
            refreshTokenService.deleteExpiredTokens();
            log.info("Limpieza de tokens completada");
        } catch (Exception e) {
            log.error("Error durante la limpieza de tokens", e);
        }
    }

    // ── 2. Tokens revocados antiguos ─────────────────────────────────────────
    // Tokens revocados hace más de 10 días — ya no tienen ningún uso
    // Todos los días a las 3:05 AM
    @Scheduled(cron = "0 5 3 * * *")
    @Transactional
    public void cleanupRevokedTokens() {
        log.info("Iniciando limpieza de tokens revocados");
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(10);
            int eliminados = refreshTokenRepository  // ← corrección aquí
                    .deleteRevokedTokensOlderThan(limite);
            log.info("Tokens revocados eliminados: {}", eliminados);
        } catch (Exception e) {
            log.error("Error durante la limpieza de tokens revocados", e);
        }
    }

    // ── 3. Audit logs antiguos ───────────────────────────────────────────────
    // Logs con más de 90 días
    // Todos los domingos a las 3:10 AM — no hace falta diario
    @Scheduled(cron = "0 10 3 * * SUN")
    @Transactional
    public void cleanupOldAuditLogs() {
        log.info("Iniciando limpieza de audit logs antiguos");
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(90);
            int eliminados = auditLogRepository.deleteLogsOlderThan(limite);
            log.info("Audit logs eliminados: {}", eliminados);
        } catch (Exception e) {
            log.error("Error durante la limpieza de audit logs", e);
        }
    }

    // ── 4. Usuarios con soft delete antiguos ─────────────────────────────────
    // Usuarios con deleted_at hace más de 60 días
    // El día 1 de cada mes a las 3:15 AM
    @Scheduled(cron = "0 15 3 1 * *")
    @Transactional
    public void cleanupSoftDeletedUsers() {
        log.info("Iniciando limpieza de usuarios eliminados definitivamente");
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(60);

            auditLogRepository.nullifyUsuarioIdForDeletedUsers(limite);

            refreshTokenRepository.deleteTokensForDeletedUsers(limite);

            int eliminados = usuarioRepository.deleteUsersWithDeletedAtBefore(limite);
            log.info("Usuarios eliminados definitivamente: {}", eliminados);

        } catch (Exception e) {
            log.error("Error durante la limpieza de usuarios", e);
        }
    }
}
