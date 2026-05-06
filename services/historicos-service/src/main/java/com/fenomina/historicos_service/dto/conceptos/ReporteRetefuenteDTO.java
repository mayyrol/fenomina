package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReporteRetefuenteDTO {

    private Integer anio;
    private Integer periodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private BigDecimal totalRetefuente;
}