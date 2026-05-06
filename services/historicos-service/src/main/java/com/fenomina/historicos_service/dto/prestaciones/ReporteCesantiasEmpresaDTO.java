package com.fenomina.historicos_service.dto.prestaciones;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteCesantiasEmpresaDTO {

    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private Integer anioLiqui;
    private Integer periodoLiqui;
    private LocalDate finicioGeneral;
    private LocalDate ffinalGeneral;
    private LocalDate fechaIngresoEmp;
    private LocalDate fechaInicioCorte;
    private LocalDate fechaFinCorte;
    private Integer diasLiquidados;
    private BigDecimal salarioBase;
    private Boolean tieneAuxTransporte;
    private BigDecimal salarioFijoMomento;
    private BigDecimal baseLiquiTotal;
    private BigDecimal cesantias;
    private BigDecimal interesesCesantias;
    private String fondoPensionEmp;
}
