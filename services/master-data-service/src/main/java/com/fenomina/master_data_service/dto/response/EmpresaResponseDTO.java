package com.fenomina.master_data_service.dto.response;

import com.fenomina.master_data_service.dto.request.EmpresaCorreoDTO;

import java.time.LocalDateTime;
import java.util.List;

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
        List<EmpresaCorreoDTO> correos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}