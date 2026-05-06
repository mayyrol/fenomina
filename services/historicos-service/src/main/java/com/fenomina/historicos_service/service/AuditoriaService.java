package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.dto.auditoria.AuditLogResponseDTO;
import com.fenomina.historicos_service.dto.auditoria.SystemAuditLogResponseDTO;
import com.fenomina.historicos_service.entity.auth.AuditLog;
import com.fenomina.historicos_service.entity.historical.SystemAuditLog;
import com.fenomina.historicos_service.repository.auth.AuditLogRepository;
import com.fenomina.historicos_service.repository.historical.SystemAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditLogRepository auditLogRepository;
    private final SystemAuditLogRepository systemAuditLogRepository;

    public Page<AuditLogResponseDTO> getAuthAuditLogs(
            Long usuarioId,
            String username,
            String accion,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable) {

        log.debug("Consultando auth.audit_logs con filtros: usuarioId={}, username={}, accion={}",
                usuarioId, username, accion);

        return auditLogRepository
                .findByFiltros(usuarioId, username, accion, desde, hasta, pageable)
                .map(this::mapAuditLog);
    }

    public Page<SystemAuditLogResponseDTO> getSystemAuditLogs(
            Long usuarioId,
            String username,
            String tablaAfectada,
            String operacion,
            Long empresaId,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable) {

        log.debug("Consultando system_audit_log con filtros: usuarioId={}, tabla={}, operacion={}",
                usuarioId, tablaAfectada, operacion);

        return systemAuditLogRepository
                .findByFiltros(usuarioId, username, tablaAfectada,
                        operacion, empresaId, desde, hasta, pageable)
                .map(this::mapSystemAuditLog);
    }

    private AuditLogResponseDTO mapAuditLog(AuditLog entity) {
        return AuditLogResponseDTO.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .username(entity.getUsername())
                .accion(entity.getAccion())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getTimestamp())
                .detalles(entity.getDetalles())
                .build();
    }

    private SystemAuditLogResponseDTO mapSystemAuditLog(SystemAuditLog entity) {
        return SystemAuditLogResponseDTO.builder()
                .auditId(entity.getAuditId())
                .tablaAfectada(entity.getTablaAfectada())
                .schemaAfectado(entity.getSchemaAfectado())
                .operacion(entity.getOperacion())
                .registroId(entity.getRegistroId())
                .usuarioId(entity.getUsuarioId())
                .username(entity.getUsername())
                .fkIdEmpresa(entity.getFkIdEmpresa())
                .datosAnteriores(entity.getDatosAnteriores())
                .datosNuevos(entity.getDatosNuevos())
                .descripcion(entity.getDescripcion())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
