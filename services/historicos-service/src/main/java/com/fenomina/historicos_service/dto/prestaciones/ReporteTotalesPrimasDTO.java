package com.fenomina.historicos_service.dto.prestaciones;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteTotalesPrimasDTO {

    private Integer anio;
    private Integer periodo;           // semestre
    private BigDecimal totalNetoPrimas;
    private Long totalEmpleados;
    private String estadoProceso;
}