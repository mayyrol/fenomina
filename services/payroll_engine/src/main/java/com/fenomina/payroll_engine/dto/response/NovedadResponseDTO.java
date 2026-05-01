package com.fenomina.payroll_engine.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NovedadResponseDTO(
        Long novedadId,
        Long fkEmpleadoId,
        Long fkConcepNominaId,
        String nombreConcepto,
        Long procesoLiquid,
        Integer anio,
        Integer periodo,
        LocalDate fechaNovedad,
        LocalDate fechaInicioAusen,
        LocalDate fechaFinAusen,
        Integer cantidadDiasNovedad,
        BigDecimal cantidadHorasNovedad,
        BigDecimal valorRefNovedad,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
