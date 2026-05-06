package com.fenomina.historicos_service.entity.auth;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "audit_logs", schema = "auth")
public class AuditLog {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "username")
    private String username;

    @Column(name = "accion")
    private String accion;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "detalles")
    private String detalles;
}
