package com.fenomina.historicos_service.dto.nomina;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteNominaEmpleadosDTO {

    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private BigDecimal salarioBascMensual;
    private Integer anio;
    private Integer periodo;
    private BigDecimal totalDevengado;
    private BigDecimal totalDeducciones;
    private BigDecimal netoNomina;
}
