package com.fenomina.auth.scheduler;

import com.fenomina.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    // Limpia tokens expirados todos los días a las 3:00 AM: 0 0 3 * * *.
    @Scheduled(cron = "0 0 3 * * *") // -> Para pruebas cada 30 segundos: */30 * * * * *
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpieza programada de tokens expirados");
        try {
            refreshTokenService.deleteExpiredTokens();
            log.info("Limpieza de tokens completada exitosamente");
        } catch (Exception e) {
            log.error("Error durante la limpieza de tokens", e);
        }
    }
}
