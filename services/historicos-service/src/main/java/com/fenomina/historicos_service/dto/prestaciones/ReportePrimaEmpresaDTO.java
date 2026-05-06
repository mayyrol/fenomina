package com.fenomina.historicos_service.dto.prestaciones;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReportePrimaEmpresaDTO {

    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private Integer anioLiqui;
    private Integer periodoLiqui;        // semestre
    private LocalDate finicioGeneral;
    private LocalDate ffinalGeneral;
    private LocalDate fechaInicioCorte;
    private LocalDate fechaFinCorte;
    private Integer diasLiquidados;
    private BigDecimal salarioBase;
    private Boolean tieneAuxTransporte;
    private BigDecimal promedioVarPeriodo; // otros: horas extra + bonificaciones
    private BigDecimal baseLiquiTotal;
    private BigDecimal valorNetoPrima;
}