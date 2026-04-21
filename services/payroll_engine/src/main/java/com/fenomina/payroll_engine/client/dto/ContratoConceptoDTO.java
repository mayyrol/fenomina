package com.fenomina.payroll_engine.client.dto;

import java.math.BigDecimal;

public record ContratoConceptoDTO(
        Long contratoConceptId,
        Long empleadoId,
        Long conceptoNominaId,
        String nombreConcepto,
        String categoriaConcepto,
        Boolean esSalario,
        Boolean esIbc,
        BigDecimal valorFijo,
        BigDecimal porcentaje
) {}
