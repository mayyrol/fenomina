package com.fenomina.payroll_engine.entity;

import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proceso_liquidacion", schema = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoLiquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proceso_liqui_id")
    private Long procesoLiquiId;

    @Column(name = "fk_usuario_id", nullable = false)
    private Long fkUsuarioId;

    @Column(name = "fk_id_empresa", nullable = false)
    private Long fkIdEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proceso", nullable = false, length = 30)
    private TipoProceso tipoProceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_proc_nomina", nullable = false, length = 30)
    private EstadoProceso estadoProcNomina;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    @Column(name = "fecha_fin_periodo")
    private LocalDate fechaFinPeriodo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.estadoProcNomina = EstadoProceso.BORRADOR;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
