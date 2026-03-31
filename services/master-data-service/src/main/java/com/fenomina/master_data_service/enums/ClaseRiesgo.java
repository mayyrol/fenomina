package com.fenomina.master_data_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum ClaseRiesgo {

    CLASE_I("I", "Riesgo Mínimo", new BigDecimal("0.00522")),
    CLASE_II("II", "Riesgo Bajo", new BigDecimal("0.01044")),
    CLASE_III("III", "Riesgo Medio", new BigDecimal("0.02436")),
    CLASE_IV("IV", "Riesgo Alto", new BigDecimal("0.04350")),
    CLASE_V("V", "Riesgo Máximo", new BigDecimal("0.06960"));

    private final String codigo;
    private final String descripcion;
    private final BigDecimal porcentaje;  // Porcentaje de cotización ARL
}
