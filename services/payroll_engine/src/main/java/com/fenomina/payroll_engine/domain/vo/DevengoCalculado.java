package com.fenomina.payroll_engine.domain.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DevengoCalculado {

    private final Long concepNominaId;
    private final String nombreConcepto;
    private final Integer cantidad;
    private final BigDecimal cantidadHoras;
    private final BigDecimal baseCalculo;
    private final BigDecimal valorResultado;
    private final boolean esSalario;
    private final boolean esIbc;
    private final boolean esAuxilioTransporte;
    private final boolean esInformativo;
    private final Long novedadId;
    private final String textoInformativo;
}
