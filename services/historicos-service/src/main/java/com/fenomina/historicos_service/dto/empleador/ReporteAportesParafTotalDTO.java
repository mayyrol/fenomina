package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteAportesParafTotalDTO {

    private Integer anio;
    private Integer periodo;
    private BigDecimal apFiscaSena;        // puede ser 0 si exonerado
    private BigDecimal apFiscaIcbf;        // puede ser 0 si exonerado
    private BigDecimal apFiscaCajaComp;
    private BigDecimal totalAportesParafEmpr;
}
