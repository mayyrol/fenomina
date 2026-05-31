package com.fenomina.payroll_engine.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NovedadRequestDTO(
        @NotNull Long fkEmpleadoId,
        @NotNull Long fkConcepNominaId,
        @NotNull Long procesoLiquid,
        @NotNull Integer anio,
        @NotNull Integer periodo,
        LocalDate fechaNovedad,
        LocalDate fechaInicioAusen,
        LocalDate fechaFinAusen,
        Integer cantidadDiasNovedad,
        BigDecimal cantidadHorasNovedad,
        BigDecimal valorRefNovedad,
        String observaciones,
        String tipoVacacion
) {}
