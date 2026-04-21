package com.fenomina.payroll_engine.dto.request;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record LiquidarNominaRequestDTO(
        @NotNull List<Long> empleadosSeleccionados,
        @NotNull Map<Long, Integer> diasLaborados
) {}
