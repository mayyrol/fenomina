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
@Table(name = "detalle_liqui_prestacion", schema = "payroll")
public class DetalleLiquiPrestacion {

    @Id
    @Column(name = "detalle_prestacion_id")
    private Long detallePrestacionId;

    @Column(name = "fk_cabe_liqui_prestacion_id")
    private Long fkCabeLiquiPrestacionId;

    @Column(name = "fk_empleado_id")
    private Long fkEmpleadoId;

    @Column(name = "fk_concep_nomina_id")
    private Long fkConcepNominaId;

    @Column(name = "fecha_inicio_corte_emp")
    private LocalDate fechaInicioCorteEmp;

    @Column(name = "fecha_fin_corte_emp")
    private LocalDate fechaFinCorteEmp;

    @Column(name = "dias_liquidados_int")
    private Integer diasLiquidadosInt;

    @Column(name = "promedio_var_periodo")
    private BigDecimal promedioVarPeriodo;

    @Column(name = "salario_fijo_momento")
    private BigDecimal salarioFijoMomento;

    @Column(name = "base_liqui_total")
    private BigDecimal baseLiquiTotal;

    @Column(name = "valor_neto_presta")
    private BigDecimal valorNetoPresta;

    @Column(name = "valor_int_cesantias")
    private BigDecimal valorIntCesantias;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
