package com.fenomina.master_data_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContratoConceptoResponseDTO(
        Long contratoConceptId,
        Long empleadoId,
        String empleadoNombreCompleto,
        String empleadoDocumento,
        Long conceptoNominaId,
        String conceptoNombre,
        String conceptoCategoria,
        BigDecimal valorFijo,
        LocalDateTime createdAt
) {
}
