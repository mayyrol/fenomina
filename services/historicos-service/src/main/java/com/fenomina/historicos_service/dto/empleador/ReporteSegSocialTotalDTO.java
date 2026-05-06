package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteSegSocialTotalDTO {

    private Integer anio;
    private Integer periodo;
    private BigDecimal segSocialSalud;     // puede ser 0 si empresa exonerada ley 1607
    private BigDecimal segSocialPension;
    private BigDecimal segSocialArl;
    private BigDecimal totalSegSocialEmpr;
}
