package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteIncapacidadesDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaPeriodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private String pagoPor;                        // traído de novedad
    private Integer diasIncapacidadComun;
    private Integer diasIncapacidadLaboral;
    private BigDecimal totalIncapacidadComun;
    private BigDecimal totalIncapacidadLaboral;
}
