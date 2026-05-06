package com.fenomina.historicos_service.entity.payroll;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "nomina_cabecera", schema = "payroll")
@Where(clause = "deleted_at IS NULL")
public class NominaCabecera {

    @Id
    @Column(name = "cabec_nomina_id")
    private Long cabecNominaId;

    @Column(name = "fk_empleado_id")
    private Long fkEmpleadoId;

    @Column(name = "fk_proceso_liqui_id")
    private Long fkProcesoLiquiId;

    @Column(name = "anio_cabec_nomina")
    private Integer anioCabecNomina;

    @Column(name = "periodo_coti_nomina")
    private Integer periodoCotiNomina;

    @Column(name = "total_devengado_emp")
    private BigDecimal totalDevengadoEmp;

    @Column(name = "total_deduccion_emp")
    private BigDecimal totalDeduccionEmp;

    @Column(name = "neto_nomina_emp")
    private BigDecimal netoNominaEmp;

    @Column(name = "fecha_cierre_nomina")
    private LocalDateTime fechaCierreNomina;

    @Column(name = "costo_total_empresa")
    private BigDecimal costoTotalEmpresa;

    @Column(name = "total_ap_patronales")
    private BigDecimal totalApPatronales;

    @Column(name = "total_provisiones")
    private BigDecimal totalProvisiones;

    @Column(name = "ibc_salud")
    private BigDecimal ibcSalud;

    @Column(name = "ibc_pension")
    private BigDecimal ibcPension;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
