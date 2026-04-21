package com.fenomina.payroll_engine.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DesprendibleResponseDTO {

    private final Long cabecNominaId;
    private final Long empleadoId;
    private final String nombresEmpleado;
    private final String apellidosEmpleado;
    private final String documentoEmpleado;
    private final BigDecimal salarioBasico;
    private final Integer anio;
    private final Integer periodo;
    private final BigDecimal totalDevengado;
    private final BigDecimal totalDeducciones;
    private final BigDecimal netoAPagar;
    private final List<ConceptoDetalleDTO> conceptos;

    @Getter
    @Builder
    public static class ConceptoDetalleDTO {
        private final Long concepNominaId;
        private final String nombreConcepto;
        private final String categoria;
        private final Integer cantidad;
        private final BigDecimal baseCalculo;
        private final BigDecimal valorResultado;
    }
}