package com.fenomina.payroll_engine.domain.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DeduccionCalculada {

    private final String nombreConcepto;
    private final BigDecimal baseCalculo;
    private final BigDecimal porcentaje;
    private final BigDecimal valorResultado;
    private final boolean esAporteLicenciaNoRemunerada;
    private final Long novedadId;
    private final String textoInformativo;
    private final String observacion;
    private final BigDecimal cantidad;
}
