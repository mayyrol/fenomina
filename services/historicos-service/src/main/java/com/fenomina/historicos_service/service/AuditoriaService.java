package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.dto.auditoria.AuditLogResponseDTO;
import com.fenomina.historicos_service.dto.auditoria.SystemAuditLogResponseDTO;
import com.fenomina.historicos_service.entity.auth.AuditLog;
import com.fenomina.historicos_service.entity.historical.SystemAuditLog;
import com.fenomina.historicos_service.repository.auth.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    public List<SystemAuditLogResponseDTO> getSystemAuditLogs(
            Long usuarioId, String username, String tablaAfectada,
            String operacion, Long empresaId,
            LocalDateTime desde, LocalDateTime hasta) {

        log.debug("Consultando últimos 50 registros de system_audit_log");

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM historical.system_audit_log s WHERE 1=1");

        List<Object> params = new ArrayList<>();
        int idx = 1;

        if (usuarioId != null) {
            sql.append(" AND s.usuario_id = ?").append(idx);
            params.add(usuarioId); idx++;
        }
        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(CAST(s.username AS text)) LIKE LOWER(?").append(idx).append(")");
            params.add("%" + username + "%"); idx++;
        }
        if (tablaAfectada != null && !tablaAfectada.isBlank()) {
            sql.append(" AND s.tabla_afectada = ?").append(idx);
            params.add(tablaAfectada); idx++;
        }
        if (operacion != null && !operacion.isBlank()) {
            sql.append(" AND s.operacion = ?").append(idx);
            params.add(operacion); idx++;
        }
        if (empresaId != null) {
            sql.append(" AND s.fk_id_empresa = ?").append(idx);
            params.add(empresaId); idx++;
        }
        if (desde != null) {
            sql.append(" AND s.timestamp >= ?").append(idx);
            params.add(desde); idx++;
        }
        if (hasta != null) {
            sql.append(" AND s.timestamp <= ?").append(idx);
            params.add(hasta); idx++;
        }

        sql.append(" ORDER BY s.timestamp DESC LIMIT 50");

        jakarta.persistence.Query query = entityManager
                .createNativeQuery(sql.toString(), SystemAuditLog.class);

        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        @SuppressWarnings("unchecked")
        List<SystemAuditLog> resultList = query.getResultList();

        return resultList.stream()
                .map(this::mapSystemAuditLog)
                .toList();
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