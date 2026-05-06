package com.fenomina.historicos_service.dto.nomina;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ConceptoNominaLineaDTO {

    private Long concepNominaId;
    private String nombreConcepto;
    private String categoria;          // DEVENGO o DEDUCCION
    private Boolean esSalario;
    private Boolean esInformativo;
    private Integer cantidad;          // días u horas según el tipo
    private BigDecimal baseCalculo;
    private BigDecimal valorResultado;
}
