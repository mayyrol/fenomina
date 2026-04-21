package com.fenomina.payroll_engine.domain.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProvisionCalculada {

    private final String nombreConcepto;
    private final BigDecimal baseCalculo;
    private final BigDecimal porcentaje;
    private final BigDecimal valorResultado;
    private final boolean esInformativo;
    private final String textoInformativo;
}
