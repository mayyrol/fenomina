package com.fenomina.payroll_engine.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CambiarEstadoProcesoRequestDTO(
        @NotNull String nuevoEstado,
        Map<Long, Integer>diasLaborados
) {}