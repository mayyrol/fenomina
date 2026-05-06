package com.fenomina.historicos_service.dto.auditoria;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponseDTO {

    private Long id;
    private Long usuarioId;
    private String username;
    private String accion;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private String detalles;
}
