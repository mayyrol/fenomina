package com.fenomina.auth.service;

import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.enums.TipoAccionAudit;

/**
 * Servicio para registro de auditoría de seguridad.
 * Registra todas las acciones importantes del sistema.
 */
public interface AuditLogService {

    void registrarLoginExitoso(Usuario usuario, String ipAddress, String userAgent);

    void registrarLoginFallido(String username, String ipAddress, String userAgent, String motivo);

    void registrarLogout(Usuario usuario, String ipAddress);

    void registrarTokenRefresh(Usuario usuario, String ipAddress);

    void registrarCreacionUsuario(Usuario usuarioCreador, Usuario usuarioCreado, String ipAddress);

    void registrarActualizacionUsuario(Usuario usuarioEditor, Usuario usuarioEditado, String ipAddress, String detalles);

    void registrarEliminacionUsuario(Usuario usuarioEliminador, Usuario usuarioEliminado, String ipAddress);

    void registrarBloqueoUsuario(Usuario usuario, String ipAddress, String motivo);

    void registrarDesbloqueoUsuario(Usuario usuarioDesbloqueador, Usuario usuarioDesbloqueado, String ipAddress);

    void registrarAccion(TipoAccionAudit accion, Long usuarioId, String username, String ipAddress, String detalles);
}

