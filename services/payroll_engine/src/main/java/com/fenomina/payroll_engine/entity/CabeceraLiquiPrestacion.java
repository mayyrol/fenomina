package com.fenomina.payroll_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cabecera_liqui_prestacion", schema = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CabeceraLiquiPrestacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cabe_liqui_prestacion_id")
    private Long cabeLiquiPrestacionId;

    @Column(name = "fk_proceso_liqui_id", nullable = false)
    private Long fkProcesoLiquiId;

    @Column(name = "anio_liqui_prestacion", nullable = false)
    private Integer anioLiquiPrestacion;

    @Column(name = "periodo_liqui_prestacion", nullable = false)
    private Integer periodoLiquiPrestacion;

    @Column(name = "finicio_general_liqui_prest")
    private LocalDate finicioGeneralLiquiPrest;

    @Column(name = "ffinal_general_liqui_prest")
    private LocalDate ffinalGeneralLiquiPrest;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
