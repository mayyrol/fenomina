package com.fenomina.payroll_engine.client.dto;

import java.util.List;

public record EmpresaDTO(
        Long empresaId,
        String empresaNit,
        String razonSocial,
        String nombreEmpresa,
        Boolean esExoneradaLey1607,
        Boolean aplicaNomina,
        Boolean aplicaPrima,
        Boolean aplicaCesantias,
        List<EmpresaCorreoDTO> correos
) {}
