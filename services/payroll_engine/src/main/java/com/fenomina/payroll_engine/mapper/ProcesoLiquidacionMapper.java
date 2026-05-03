package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.dto.response.ProcesoLiquidacionResponseDTO;
import com.fenomina.payroll_engine.entity.DetalleLiquiPrestacion;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.repository.CabeceraLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.DetalleLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcesoLiquidacionMapper {

    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    public ProcesoLiquidacionResponseDTO toResponse(ProcesoLiquidacion proceso) {
        Integer cantidadEmpleados;
        BigDecimal totalNeto;

        boolean esNomina = proceso.getTipoProceso() != null &&
                (proceso.getTipoProceso().name().equals("NOMINA_MENSUAL") ||
                        proceso.getTipoProceso().name().equals("NOMINA_QUINCENAL"));

        if (esNomina) {
            cantidadEmpleados = nominaCabeceraRepository
                    .countByFkProcesoLiquiId(proceso.getProcesoLiquiId());
            totalNeto = nominaCabeceraRepository
                    .sumNetoByFkProcesoLiquiId(proceso.getProcesoLiquiId());
        } else {
            // Prima, cesantías, intereses
            var cabecera = cabeceraLiquiPrestacionRepository
                    .findByFkProcesoLiquiId(proceso.getProcesoLiquiId());

            if (cabecera.isPresent()) {
                Long cabeceraId = cabecera.get().getCabeLiquiPrestacionId();
                List<DetalleLiquiPrestacion> detalles = detalleLiquiPrestacionRepository
                        .findByFkCabeLiquiPrestacionId(cabeceraId);
                cantidadEmpleados = detalles.size();
                totalNeto = detalles.stream()
                        .map(d -> d.getValorNetaPresta() != null
                                ? d.getValorNetaPresta() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                cantidadEmpleados = 0;
                totalNeto = BigDecimal.ZERO;
            }
        }

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