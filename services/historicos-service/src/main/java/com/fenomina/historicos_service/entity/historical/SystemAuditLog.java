package com.fenomina.historicos_service.entity.historical;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "system_audit_log", schema = "historical")
public class SystemAuditLog {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "tabla_afectada")
    private String tablaAfectada;

    @Column(name = "schema_afectado")
    private String schemaAfectado;

    @Column(name = "operacion")
    private String operacion;

    @Column(name = "registro_id")
    private Long registroId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "username")
    private String username;

    @Column(name = "fk_id_empresa")
    private Long fkIdEmpresa;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}