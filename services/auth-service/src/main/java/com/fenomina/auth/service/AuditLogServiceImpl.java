package com.fenomina.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenomina.auth.entity.AuditLog;
import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.enums.TipoAccionAudit;
import com.fenomina.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del servicio de auditoría.
 * Todos los métodos son @Async para no bloquear operaciones críticas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper; // Para convertir objetos a JSON

    @Override
    @Async
    @Transactional
    public void registrarLoginExitoso(Usuario usuario, String ipAddress, String userAgent) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("rol", usuario.getRolUsuario().name());
        detalles.put("empresaId", usuario.getFkIdEmpresa());

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.LOGIN_SUCCESS,
                usuario.getUsuarioId(),
                usuario.getUserName(),
                ipAddress,
                userAgent,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.info("Login exitoso registrado - Usuario: {} desde IP: {}", usuario.getUserName(), ipAddress);
    }

    @Override
    @Async
    @Transactional
    public void registrarLoginFallido(String username, String ipAddress, String userAgent, String motivo) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("motivo", motivo);

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.LOGIN_FAILED,
                null, // No hay ID de usuario si falló el login
                username,
                ipAddress,
                userAgent,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.warn("Login fallido registrado - Username: {} desde IP: {} - Motivo: {}",
                username, ipAddress, motivo);
    }

    @Override
    @Async
    @Transactional
    public void registrarLogout(Usuario usuario, String ipAddress) {
        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.LOGOUT,
                usuario.getUsuarioId(),
                usuario.getUserName(),
                ipAddress,
                null,
                null
        );

        auditLogRepository.save(auditLog);
        log.info("Logout registrado - Usuario: {}", usuario.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarTokenRefresh(Usuario usuario, String ipAddress) {
        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.TOKEN_REFRESHED,
                usuario.getUsuarioId(),
                usuario.getUserName(),
                ipAddress,
                null,
                null
        );

        auditLogRepository.save(auditLog);
        log.debug("Token refresh registrado - Usuario: {}", usuario.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarCreacionUsuario(Usuario usuarioCreador, Usuario usuarioCreado, String ipAddress) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("usuarioCreado", usuarioCreado.getUserName());
        detalles.put("rolAsignado", usuarioCreado.getRolUsuario().name());
        detalles.put("empresaAsignada", usuarioCreado.getFkIdEmpresa());

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.USER_CREATED,
                usuarioCreador.getUsuarioId(),
                usuarioCreador.getUserName(),
                ipAddress,
                null,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.info("Creación de usuario registrada - Creador: {} - Nuevo usuario: {}",
                usuarioCreador.getUserName(), usuarioCreado.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarActualizacionUsuario(Usuario usuarioEditor, Usuario usuarioEditado,
                                              String ipAddress, String detalles) {
        Map<String, Object> detallesMap = new HashMap<>();
        detallesMap.put("usuarioEditado", usuarioEditado.getUserName());
        detallesMap.put("cambios", detalles);

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.USER_UPDATED,
                usuarioEditor.getUsuarioId(),
                usuarioEditor.getUserName(),
                ipAddress,
                null,
                detallesMap
        );

        auditLogRepository.save(auditLog);
        log.info("Actualización de usuario registrada - Editor: {} - Usuario editado: {}",
                usuarioEditor.getUserName(), usuarioEditado.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarEliminacionUsuario(Usuario usuarioEliminador, Usuario usuarioEliminado, String ipAddress) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("usuarioEliminado", usuarioEliminado.getUserName());
        detalles.put("usuarioId", usuarioEliminado.getUsuarioId());

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.USER_DELETED,
                usuarioEliminador.getUsuarioId(),
                usuarioEliminador.getUserName(),
                ipAddress,
                null,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.info("Eliminación de usuario registrada - Eliminador: {} - Usuario eliminado: {}",
                usuarioEliminador.getUserName(), usuarioEliminado.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarBloqueoUsuario(Usuario usuario, String ipAddress, String motivo) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("motivo", motivo);
        detalles.put("fechaBloqueo", LocalDateTime.now().toString());

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.USER_BLOCKED,
                usuario.getUsuarioId(),
                usuario.getUserName(),
                ipAddress,
                null,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.warn("Bloqueo de usuario registrado - Usuario: {} - Motivo: {}",
                usuario.getUserName(), motivo);
    }

    @Override
    @Async
    @Transactional
    public void registrarDesbloqueoUsuario(Usuario usuarioDesbloqueador, Usuario usuarioDesbloqueado, String ipAddress) {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("usuarioDesbloqueado", usuarioDesbloqueado.getUserName());

        AuditLog auditLog = crearAuditLog(
                TipoAccionAudit.USER_UNBLOCKED,
                usuarioDesbloqueador.getUsuarioId(),
                usuarioDesbloqueador.getUserName(),
                ipAddress,
                null,
                detalles
        );

        auditLogRepository.save(auditLog);
        log.info("Desbloqueo de usuario registrado - Desbloqueador: {} - Usuario desbloqueado: {}",
                usuarioDesbloqueador.getUserName(), usuarioDesbloqueado.getUserName());
    }

    @Override
    @Async
    @Transactional
    public void registrarAccion(TipoAccionAudit accion, Long usuarioId, String username,
                                String ipAddress, String detalles) {
        AuditLog auditLog = AuditLog.builder()
                .accion(accion)
                .usuarioId(usuarioId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(null)
                .detalles(detalles)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Acción {} registrada - Usuario: {}", accion, username);
    }

    /**
     * Método privado para crear objetos AuditLog de forma consistente.
     * Convierte el mapa de detalles a JSON.
     */
    private AuditLog crearAuditLog(TipoAccionAudit accion, Long usuarioId, String username,
                                   String ipAddress, String userAgent, Map<String, Object> detalles) {
        String detallesJson = null;
        if (detalles != null && !detalles.isEmpty()) {
            try {
                detallesJson = objectMapper.writeValueAsString(detalles);
            } catch (JsonProcessingException e) {
                log.error("Error al convertir detalles a JSON", e);
                detallesJson = detalles.toString(); // Fallback a toString
            }
        }

        return AuditLog.builder()
                .accion(accion)
                .usuarioId(usuarioId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .detalles(detallesJson)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
