package com.fenomina.master_data_service.dto.response;

import java.time.LocalDateTime;

public record EmpresaResponseDTO(
        Long empresaId,
        String empresaNit,
        String razonSocial,
        String nombreEmpresa,
        Boolean esExoneradaLey1607,
        String logoEmpresaUrl,
        Boolean aplicaNomina,
        Boolean aplicaPrima,
        Boolean aplicaCesantias,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}