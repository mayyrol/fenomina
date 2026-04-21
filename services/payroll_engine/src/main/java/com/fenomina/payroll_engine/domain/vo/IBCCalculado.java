package com.fenomina.payroll_engine.domain.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class IBCCalculado {

    private final BigDecimal ibcSalud;
    private final BigDecimal ibcPension;
    private final BigDecimal ibcArl;
    private final BigDecimal ibcParafiscales;
    private final boolean cotizaSalud;
    private final boolean cotizaPension;
    private final boolean cotizaArl;
}