package com.fenomina.master_data_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContratoConceptoRequestDTO(

        @NotNull(message = "El ID del empleado es obligatorio")
        Long empleadoId,

        @NotNull(message = "El ID del concepto de nómina es obligatorio")
        Long conceptoNominaId,

        @DecimalMin(value = "0.01", message = "El valor fijo debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El valor fijo tiene formato inválido")
        BigDecimal valorFijo
) {
}
