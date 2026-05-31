package com.fenomina.historicos_service.dto.empleador;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteCargasPrestTotalDTO {

    private Integer anio;
    private Integer periodo;
    private BigDecimal cargPresCesantiasInformativo;
    private BigDecimal cargPresPrimas;
    private BigDecimal cargPresIntCesantias;
}
