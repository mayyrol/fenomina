package com.fenomina.historicos_service.dto.prestaciones;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteTotalesCesantiasDTO {

    private Integer anio;
    private Integer periodo;
    private BigDecimal totalCesantias;
    private BigDecimal totalInteresesCesantias;
    private Long totalEmpleados;
    private String estadoProceso;
}
