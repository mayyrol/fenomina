package com.fenomina.payroll_engine.dto.request;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoProcesoRequestDTO(
        @NotNull String nuevoEstado
) {}