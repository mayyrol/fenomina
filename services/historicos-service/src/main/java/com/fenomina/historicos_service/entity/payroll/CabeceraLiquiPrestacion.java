package com.fenomina.historicos_service.entity.payroll;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "cabecera_liqui_prestacion", schema = "payroll")
@Where(clause = "deleted_at IS NULL")
public class CabeceraLiquiPrestacion {

    @Id
    @Column(name = "cabe_liqui_prestacion_id")
    private Long cabeLiquiPrestacionId;

    @Column(name = "fk_proceso_liqui_id")
    private Long fkProcesoLiquiId;

    @Column(name = "anio_liqui_prestacion")
    private Integer anioLiquiPrestacion;

    @Column(name = "periodo_liqui_prestacion")
    private Integer periodoLiquiPrestacion;

    @Column(name = "finicio_general_liqui_prest")
    private LocalDate finicioGeneralLiquiPrest;

    @Column(name = "ffinal_general_liqui_prest")
    private LocalDate ffinalGeneralLiquiPrest;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
