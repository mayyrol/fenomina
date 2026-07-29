package com.fenomina.payroll_engine.entity;

import com.fenomina.payroll_engine.enums.EstadoEnvio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "envio_desprendible", schema = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioDesprendible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "envio_desprendible_id")
    private Long envioDesprendibleId;

    @Column(name = "fk_proceso_liqui_id", nullable = false)
    private Long fkProcesoLiquiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 20)
    private EstadoEnvio estadoEnvio;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fk_usuario_id", nullable = false)
    private Long fkUsuarioId;

    @Column(name = "asunto_correo", length = 255)
    private String asuntoCorreo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
