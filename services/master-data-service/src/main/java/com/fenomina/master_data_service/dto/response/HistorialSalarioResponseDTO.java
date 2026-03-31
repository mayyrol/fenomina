package com.fenomina.master_data_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistorialSalarioResponseDTO(
        Long histSalarioId,
        Long empleadoId,
        String empleadoNombreCompleto,
        BigDecimal salarioAnterior,
        BigDecimal salarioActual,
        BigDecimal diferencia,
        LocalDateTime createdAt,
        String creadoPor
) {
    public HistorialSalarioResponseDTO {
        if (salarioAnterior != null && salarioActual != null) {
            diferencia = salarioActual.subtract(salarioAnterior);
        } else {
            diferencia = BigDecimal.ZERO;
        }
    }
}
