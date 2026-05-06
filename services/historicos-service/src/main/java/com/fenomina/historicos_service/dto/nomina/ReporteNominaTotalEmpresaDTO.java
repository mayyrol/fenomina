package com.fenomina.historicos_service.dto.nomina;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteNominaTotalEmpresaDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaInicioPeriodo;
    private LocalDate fechaFinPeriodo;
    private BigDecimal totalNeto;
    private BigDecimal totalDevengado;
    private BigDecimal totalDeducciones;
    private BigDecimal totalCostoEmpresa;
    private Long totalEmpleados;
}