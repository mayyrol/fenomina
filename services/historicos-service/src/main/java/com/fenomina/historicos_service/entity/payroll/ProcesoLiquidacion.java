package com.fenomina.historicos_service.entity.payroll;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "proceso_liquidacion", schema = "payroll")
public class ProcesoLiquidacion {

    @Id
    @Column(name = "proceso_liqui_id")
    private Long procesoLiquiId;

    @Column(name = "fk_usuario_id")
    private Long fkUsuarioId;

    @Column(name = "fk_id_empresa")
    private Long fkIdEmpresa;

    @Column(name = "tipo_proceso")
    private String tipoProceso;

    @Column(name = "estado_proc_nomina")
    private String estadoProcNomina;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "periodo")
    private Integer periodo;

    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    @Column(name = "fecha_fin_periodo")
    private LocalDate fechaFinPeriodo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}