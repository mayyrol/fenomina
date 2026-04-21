package com.fenomina.payroll_engine.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProcesoLiquidacionRequestDTO(
        @NotNull Long empresaId,
        @NotNull String tipoProceso,
        @NotNull Integer anio,
        @NotNull Integer periodo,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin
) {}