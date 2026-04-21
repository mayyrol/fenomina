package com.fenomina.payroll_engine.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParametroGeneralDTO(
        Long paramGeneralId,
        String nombreParamGeneral,
        String descripcionParam,
        LocalDate fechaParamGeneral,
        BigDecimal valorParamGeneral,
        BigDecimal porcentajeParamGeneral
) {}
