package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteAportesParafXEmpleadoDTO {

    private Integer anio;
    private Integer periodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private LocalDate fechaIngresoEmp;
    private BigDecimal apFiscaSena;        // puede ser 0
    private BigDecimal apFiscaIcbf;        // puede ser 0
    private BigDecimal apFiscaCajaComp;
    private BigDecimal totalAportesParafEmpleador;
}