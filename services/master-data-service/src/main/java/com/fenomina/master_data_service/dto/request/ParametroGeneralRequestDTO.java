package com.fenomina.master_data_service.dto.request;

import com.fenomina.master_data_service.enums.ParametroNombre;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParametroGeneralRequestDTO(

        @NotNull(message = "El nombre del parámetro es obligatorio")
        ParametroNombre nombreParamGeneral,

        String descripcionParam,

        @NotNull(message = "La fecha de vigencia del parámetro es obligatoria")
        @FutureOrPresent(message = "La fecha de vigencia no puede ser pasada")
        LocalDate fechaParamGeneral,

        @Digits(integer = 12, fraction = 3, message = "El valor del parámetro tiene formato inválido")
        BigDecimal valorParamGeneral,

        @DecimalMin(value = "0.0001", message = "El porcentaje debe ser mayor a cero")
        @DecimalMax(value = "1.0", message = "El porcentaje no puede ser mayor a 1 (100%)")
        @Digits(integer = 1, fraction = 4, message = "El porcentaje tiene formato inválido")
        BigDecimal porcentajeParamGeneral
) {
    public ParametroGeneralRequestDTO {
        // Validación: debe tener exactamente uno de los dos (valor o porcentaje)
        boolean tieneValor = valorParamGeneral != null;
        boolean tienePorcentaje = porcentajeParamGeneral != null;

        if ((tieneValor && tienePorcentaje) || (!tieneValor && !tienePorcentaje)) {
            throw new IllegalArgumentException(
                    "Debe proporcionar exactamente uno: valorParamGeneral o porcentajeParamGeneral"
            );
        }
    }
}
