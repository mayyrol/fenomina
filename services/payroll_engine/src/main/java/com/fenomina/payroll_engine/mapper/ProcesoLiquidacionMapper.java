package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.dto.response.ProcesoLiquidacionResponseDTO;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import org.springframework.stereotype.Component;

@Component
public class ProcesoLiquidacionMapper {

    public ProcesoLiquidacionResponseDTO toResponse(ProcesoLiquidacion proceso) {
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
                proceso.getUpdatedAt()
        );
    }
}