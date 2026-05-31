package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteVacacionesTotalDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaPeriodo;
    private BigDecimal totalVacacionesCompensadas;
    private BigDecimal totalVacacionesDisfrutadas;
    private BigDecimal totalVacaciones;
}
