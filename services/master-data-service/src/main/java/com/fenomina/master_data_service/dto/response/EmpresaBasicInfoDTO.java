package com.fenomina.master_data_service.dto.response;

public record EmpresaBasicInfoDTO(
        Long empresaId,
        String empresaNit,
        String nombreEmpresa,
        String logoEmpresaUrl
) {
}