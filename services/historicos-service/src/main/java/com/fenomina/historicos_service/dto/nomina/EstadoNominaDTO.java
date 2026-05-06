package com.fenomina.historicos_service.dto.nomina;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class EstadoNominaDTO {

    private Long cabecNominaId;
    private Long empleadoId;
    private String nombresEmp;
    private String apellidosEmp;
    private String documentoEmp;
    private LocalDate fechaIngresoEmp;
    private Integer anio;
    private Integer periodo;
    private BigDecimal netoNominaEmp;
    private String estadoProceso;
    private LocalDateTime fechaCierreNomina;
}
