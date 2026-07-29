package com.fenomina.payroll_engine.dto.response;

import java.util.List;

public record PreviewEnvioResponseDTO(
        List<String> correosDestino,
        String asunto,
        String cuerpo
) {}