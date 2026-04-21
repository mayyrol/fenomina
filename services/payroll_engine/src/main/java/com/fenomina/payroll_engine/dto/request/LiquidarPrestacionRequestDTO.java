package com.fenomina.payroll_engine.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LiquidarPrestacionRequestDTO(
        @NotNull List<Long> empleadosSeleccionados
) {}