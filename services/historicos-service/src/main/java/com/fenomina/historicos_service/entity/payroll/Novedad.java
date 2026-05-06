package com.fenomina.historicos_service.entity.payroll;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "novedad", schema = "payroll")
public class Novedad {

    @Id
    @Column(name = "novedad_id")
    private Long novedadId;

    @Column(name = "fk_empleado_id")
    private Long fkEmpleadoId;

    @Column(name = "fk_concep_nomina_id")
    private Long fkConcepNominaId;

    @Column(name = "fecha_novedad")
    private LocalDate fechaNovedad;

    @Column(name = "fecha_inicio_ausen")
    private LocalDate fechaInicioAusen;

    @Column(name = "fecha_fin_ausen")
    private LocalDate fechaFinAusen;

    @Column(name = "tipo_vacacion")
    private String tipoVacacion;

    @Column(name = "cantidad_dias_novedad")
    private Integer cantidadDiasNovedad;

    @Column(name = "cantidad_horas_novedad")
    private BigDecimal cantidadHorasNovedad;

    @Column(name = "valor_ref_novedad")
    private BigDecimal valorRefNovedad;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "periodo")
    private Integer periodo;

    @Column(name = "proceso_liquid")
    private Long procesoLiquid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
