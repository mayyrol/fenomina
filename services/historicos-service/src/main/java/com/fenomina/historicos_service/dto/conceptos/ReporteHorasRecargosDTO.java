package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteHorasRecargosDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaPeriodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;

    // Recargos nocturnos lunes-sabado
    private BigDecimal horasRecargoNocturnoLunSab;
    private BigDecimal valorRecargoNocturnoLunSab;

    // Recargos diurnos domingo/festivo
    private BigDecimal horasRecargoDiurnoDomFest;
    private BigDecimal valorRecargoDiurnoDomFest;

    // Recargos nocturnos domingo/festivo
    private BigDecimal horasRecargoNocturnoDomFest;
    private BigDecimal valorRecargoNocturnoDomFest;

    // Horas extra diurnas lunes-sabado
    private BigDecimal horasExtraDiurnaLunSab;
    private BigDecimal valorExtraDiurnaLunSab;

    // Horas extra nocturnas lunes-sabado
    private BigDecimal horasExtraNocturnaLunSab;
    private BigDecimal valorExtraNocturnaLunSab;

    // Horas extra diurnas domingo/festivo
    private BigDecimal horasExtraDiurnaDomFest;
    private BigDecimal valorExtraDiurnaDomFest;

    // Horas extra nocturnas domingo/festivo
    private BigDecimal horasExtraNocturnaDomFest;
    private BigDecimal valorExtraNocturnaDomFest;

    private BigDecimal totalHorasExtraYRecargos;
}