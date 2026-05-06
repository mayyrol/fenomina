package com.fenomina.payroll_engine.mapper;

import com.fenomina.payroll_engine.dto.response.ProcesoLiquidacionResponseDTO;
import com.fenomina.payroll_engine.entity.DetalleLiquiPrestacion;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.repository.CabeceraLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.DetalleLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import com.fenomina.payroll_engine.repository.ProcesoLiquidacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcesoLiquidacionMapper {

    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;
    private final ProcesoLiquidacionRepository procesoLiquidacionRepository;

    public ProcesoLiquidacionResponseDTO toResponse(ProcesoLiquidacion proceso) {
        Integer cantidadEmpleados;
        BigDecimal totalNeto;
        BigDecimal totalIntereses = BigDecimal.ZERO;

        boolean esNomina = proceso.getTipoProceso() != null &&
                (proceso.getTipoProceso().name().equals("NOMINA_MENSUAL") ||
                        proceso.getTipoProceso().name().equals("NOMINA_QUINCENAL"));

        boolean esCesantias = proceso.getTipoProceso() != null &&
                proceso.getTipoProceso().name().equals("CESANTIAS_ANUAL");

        if (esNomina) {
            cantidadEmpleados = nominaCabeceraRepository
                    .countByFkProcesoLiquiId(proceso.getProcesoLiquiId());
            totalNeto = nominaCabeceraRepository
                    .sumNetoByFkProcesoLiquiId(proceso.getProcesoLiquiId());

        } else {
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

            if (esCesantias && proceso.getAnio() != null) {
                List<ProcesoLiquidacion> interesesList = procesoLiquidacionRepository
                        .findByEmpresaAndTipoAndAnio(
                                proceso.getFkIdEmpresa(),
                                TipoProceso.INTERESES_CESANTIAS_ANUAL,
                                proceso.getAnio()
                        );
                log.info("Procesos intereses encontrados para empresa {} año {}: {}",
                        proceso.getFkIdEmpresa(), proceso.getAnio(), interesesList.size());

                totalIntereses = interesesList.stream()
                        .findFirst()
                        .flatMap(pi -> cabeceraLiquiPrestacionRepository
                                .findByFkProcesoLiquiId(pi.getProcesoLiquiId()))
                        .map(cab -> detalleLiquiPrestacionRepository
                                .findByFkCabeLiquiPrestacionId(cab.getCabeLiquiPrestacionId())
                                .stream()
                                .map(d -> d.getValorIntCesantias() != null
                                        ? d.getValorIntCesantias() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .orElse(BigDecimal.ZERO);

                log.info("Total intereses calculado: {}", totalIntereses);
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
                totalNeto != null ? totalNeto : BigDecimal.ZERO,
                totalIntereses
        );
    }
}