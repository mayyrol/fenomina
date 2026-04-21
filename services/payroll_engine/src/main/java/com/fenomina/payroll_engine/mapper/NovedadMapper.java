package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.dto.request.NovedadRequestDTO;
import com.fenomina.payroll_engine.dto.response.NovedadResponseDTO;
import com.fenomina.payroll_engine.entity.Novedad;
import org.springframework.stereotype.Component;

@Component
public class NovedadMapper {

    public Novedad toEntity(NovedadRequestDTO request, Long usuarioId) {
        Novedad novedad = new Novedad();
        novedad.setFkEmpleadoId(request.fkEmpleadoId());
        novedad.setFkConcepNominaId(request.fkConcepNominaId());
        novedad.setProcesoLiquid(request.procesoLiquid());
        novedad.setAnio(request.anio());
        novedad.setPeriodo(request.periodo());
        novedad.setFechaNovedad(request.fechaNovedad());
        novedad.setFechaInicioAusen(request.fechaInicioAusen());
        novedad.setFechaFinAusen(request.fechaFinAusen());
        novedad.setCantidadDiasNovedad(request.cantidadDiasNovedad());
        novedad.setCantidadHorasNovedad(request.cantidadHorasNovedad());
        novedad.setValorRefNovedad(request.valorRefNovedad());
        novedad.setObservaciones(request.observaciones());
        novedad.setCreatedBy(usuarioId);
        return novedad;
    }

    public NovedadResponseDTO toResponse(Novedad novedad) {
        return new NovedadResponseDTO(
                novedad.getNovedadId(),
                novedad.getFkEmpleadoId(),
                novedad.getFkConcepNominaId(),
                novedad.getProcesoLiquid(),
                novedad.getAnio(),
                novedad.getPeriodo(),
                novedad.getFechaNovedad(),
                novedad.getFechaInicioAusen(),
                novedad.getFechaFinAusen(),
                novedad.getCantidadDiasNovedad(),
                novedad.getCantidadHorasNovedad(),
                novedad.getValorRefNovedad(),
                novedad.getObservaciones(),
                novedad.getCreatedAt(),
                novedad.getUpdatedAt()
        );
    }
}
