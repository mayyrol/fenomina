package com.fenomina.payroll_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nomina_cabecera", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NominaCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cabec_nomina_id")
    private Long cabecNominaId;

    @Column(name = "fk_empleado_id", nullable = false)
    private Long fkEmpleadoId;

    @Column(name = "fk_proceso_liqui_id", nullable = false)
    private Long fkProcesoLiquiId;

    @Column(name = "anio_cabec_nomina", nullable = false)
    private Integer anioCabecNomina;

    @Column(name = "periodo_coti_nomina", nullable = false)
    private Integer periodoCotiNomina;

    @Column(name = "total_devengado_emp", precision = 12, scale = 2)
    private BigDecimal totalDevengadoEmp;

    @Column(name = "total_deduccion_emp", precision = 12, scale = 2)
    private BigDecimal totalDeduccionEmp;

    @Column(name = "neto_nomina_emp", precision = 15, scale = 2)
    private BigDecimal netoNominaEmp;

    @Column(name = "costo_total_empresa", precision = 15, scale = 2)
    private BigDecimal costoTotalEmpresa;

    @Column(name = "total_ap_patronales", precision = 12, scale = 2)
    private BigDecimal totalApPatronales;

    @Column(name = "total_provisiones", precision = 15, scale = 2)
    private BigDecimal totalProvisiones;

    @Column(name = "ibc_salud", precision = 15, scale = 2)
    private BigDecimal ibcSalud;

    @Column(name = "ibc_pension", precision = 15, scale = 2)
    private BigDecimal ibcPension;

    @Column(name = "fecha_cierre_nomina")
    private LocalDateTime fechaCierreNomina;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
