package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteSegSocialXEmpleadoDTO {

    private Integer anio;
    private Integer periodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private LocalDate fechaIngresoEmp;
    private BigDecimal empleadorSalud;     // puede ser 0
    private BigDecimal empleadorPension;
    private BigDecimal empleadorArl;
    private BigDecimal totalSocialEmpleador;
}