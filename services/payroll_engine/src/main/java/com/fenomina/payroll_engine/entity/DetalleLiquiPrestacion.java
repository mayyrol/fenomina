package com.fenomina.payroll_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalle_liqui_prestacion", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleLiquiPrestacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_prestacion_id")
    private Long detallePrestacionId;

    @Column(name = "fk_cabe_liqui_prestacion_id", nullable = false)
    private Long fkCabeLiquiPrestacionId;

    @Column(name = "fk_empleado_id", nullable = false)
    private Long fkEmpleadoId;

    @Column(name = "fk_concep_nomina_id", nullable = false)
    private Long fkConcepNominaId;

    @Column(name = "fecha_inicio_corte_emp", nullable = false)
    private LocalDate fechaInicioCorteEmp;

    @Column(name = "fecha_fin_corte_emp", nullable = false)
    private LocalDate fechaFinCorteEmp;

    @Column(name = "dias_liquidados_int")
    private Integer diasLiquidadosInt;

    @Column(name = "promedio_var_periodo", precision = 15, scale = 2)
    private BigDecimal promedioVarPeriodo;

    @Column(name = "salario_fijo_momento", precision = 15, scale = 2)
    private BigDecimal salarioFijoMomento;

    @Column(name = "base_liqui_total", precision = 15, scale = 2)
    private BigDecimal baseLiquiTotal;

    @Column(name = "valor_neto_presta", precision = 15, scale = 2)
    private BigDecimal valorNetaPresta;

    @Column(name = "valor_int_cesantias", precision = 15, scale = 2)
    private BigDecimal valorIntCesantias;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "promedio_aux_transporte", precision = 12, scale = 2)
    private BigDecimal promedioAuxTransporte;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}