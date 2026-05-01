package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.dto.response.ProcesoLiquidacionResponseDTO;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProcesoLiquidacionMapper {

    private final NominaCabeceraRepository nominaCabeceraRepository;

    public ProcesoLiquidacionResponseDTO toResponse(ProcesoLiquidacion proceso) {
        Integer cantidadEmpleados = nominaCabeceraRepository
                .countByFkProcesoLiquiId(proceso.getProcesoLiquiId());

        BigDecimal totalNeto = nominaCabeceraRepository
                .sumNetoByFkProcesoLiquiId(proceso.getProcesoLiquiId());

        return new ProcesoLiquidacionResponseDTO(
                proceso.getProcesoLiquiId(),
                proceso.getFkIdEmpresa(),
                proceso.getTipoProceso() != null
                        ? proceso.getTipoProceso().name() : null,
                proceso.getEstadoProcNomina() != null
                        ? proceso.getEstadoProcNomina().name() : null,
                proceso.getAnio(),
                proceso.getPeriodo(),
                proceso.getFechaInicioPeriodo(),
                proceso.getFechaFinPeriodo(),
                proceso.getCreatedAt(),
                proceso.getUpdatedAt(),
                cantidadEmpleados != null ? cantidadEmpleados : 0,
                totalNeto != null ? totalNeto : BigDecimal.ZERO
        );
    }
}