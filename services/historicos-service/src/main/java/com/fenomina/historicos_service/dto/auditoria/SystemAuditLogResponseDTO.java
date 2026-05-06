package com.fenomina.historicos_service.dto.auditoria;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemAuditLogResponseDTO {

    private Long auditId;
    private String tablaAfectada;
    private String schemaAfectado;
    private String operacion;
    private Long registroId;
    private Long usuarioId;
    private String username;
    private Long fkIdEmpresa;
    private String datosAnteriores;
    private String datosNuevos;
    private String descripcion;
    private String ipAddress;
    private LocalDateTime timestamp;
}
