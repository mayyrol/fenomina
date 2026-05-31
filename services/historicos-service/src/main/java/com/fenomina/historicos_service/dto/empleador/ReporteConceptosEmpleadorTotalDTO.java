package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteConceptosEmpleadorTotalDTO {

    private Integer anio;
    private Integer periodo;
    private BigDecimal totalSegSocialEmpr;
    private BigDecimal totalAportesParafEmpr;
    private BigDecimal cargPresPrimas;
    private BigDecimal cargPresIntCesantias;
}
