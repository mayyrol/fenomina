package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteIncapacidadesTotalDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaPeriodo;
    private BigDecimal totalIncapacidadComun;
    private BigDecimal totalIncapacidadLaboral;
}
