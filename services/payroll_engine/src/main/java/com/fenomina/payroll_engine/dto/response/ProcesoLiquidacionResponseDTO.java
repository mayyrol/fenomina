package com.fenomina.payroll_engine.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProcesoLiquidacionResponseDTO(
        Long procesoLiquiId,
        Long fkIdEmpresa,
        String tipoProceso,
        String estadoProcNomina,
        Integer anio,
        Integer periodo,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer cantidadEmpleados,
        BigDecimal totalNeto
) {}
