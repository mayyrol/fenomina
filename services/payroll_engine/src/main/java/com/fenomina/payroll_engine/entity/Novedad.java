package com.fenomina.payroll_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "novedad", schema = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Novedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "novedad_id")
    private Long novedadId;

    @Column(name = "fk_empleado_id", nullable = false)
    private Long fkEmpleadoId;

    @Column(name = "fk_concep_nomina_id", nullable = false)
    private Long fkConcepNominaId;

    @Column(name = "fecha_novedad")
    private LocalDate fechaNovedad;

    @Column(name = "fecha_inicio_ausen")
    private LocalDate fechaInicioAusen;

    @Column(name = "fecha_fin_ausen")
    private LocalDate fechaFinAusen;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    @Column(name = "proceso_liquid")
    private Long procesoLiquid;

    @Column(name = "tipo_vacacion", length = 60)
    private String tipoVacacion;

    @Column(name = "cantidad_dias_novedad")
    private Integer cantidadDiasNovedad;

    @Column(name = "cantidad_horas_novedad", precision = 5, scale = 2)
    private BigDecimal cantidadHorasNovedad;

    @Column(name = "valor_ref_novedad", precision = 12, scale = 2)
    private BigDecimal valorRefNovedad;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
