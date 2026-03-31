package com.fenomina.master_data_service.dto.response;

import com.fenomina.master_data_service.enums.ParametroNombre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ParametroGeneralResponseDTO(
        Long paramGeneralId,
        ParametroNombre nombreParamGeneral,
        String descripcionParam,
        LocalDate fechaParamGeneral,
        BigDecimal valorParamGeneral,
        BigDecimal porcentajeParamGeneral,
        LocalDateTime createdAt
) {
}