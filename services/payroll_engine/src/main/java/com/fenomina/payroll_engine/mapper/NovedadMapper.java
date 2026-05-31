package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.dto.request.NovedadRequestDTO;
import com.fenomina.payroll_engine.dto.response.NovedadResponseDTO;
import com.fenomina.payroll_engine.entity.Novedad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NovedadMapper {

    private final MasterDataClientWrapper masterDataClient;

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
        novedad.setTipoVacacion(request.tipoVacacion());
        return novedad;
    }

    public NovedadResponseDTO toResponse(Novedad novedad, List<ConceptoNominaDTO> conceptos) {
        String nombreConcepto = conceptos.stream()
                .filter(c -> c.concepNominaId().equals(novedad.getFkConcepNominaId()))
                .map(ConceptoNominaDTO::nombreConcepNomina)
                .findFirst()
                .orElse(null);

        return new NovedadResponseDTO(
                novedad.getNovedadId(),
                novedad.getFkEmpleadoId(),
                novedad.getFkConcepNominaId(),
                nombreConcepto,
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
                novedad.getTipoVacacion(),
                novedad.getCreatedAt(),
                novedad.getUpdatedAt()
        );
    }
}
