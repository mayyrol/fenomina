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
import java.util.Map;

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
                        .findByEmpresaAndTipoAndAnios(
                                proceso.getFkIdEmpresa(),
                                TipoProceso.INTERESES_CESANTIAS_ANUAL,
                                List.of(proceso.getAnio())
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
                totalIntereses,
                List.of()
        );
    }

    public List<ProcesoLiquidacionResponseDTO> toResponseList(
            List<ProcesoLiquidacion> procesos) {

        if (procesos == null || procesos.isEmpty()) return List.of();

        List<Long> todosLosIds = procesos.stream()
                .map(ProcesoLiquidacion::getProcesoLiquiId)
                .toList();

        List<Long> idsNomina = procesos.stream()
                .filter(p -> p.getTipoProceso() != null &&
                        (p.getTipoProceso().name().equals("NOMINA_MENSUAL") ||
                                p.getTipoProceso().name().equals("NOMINA_QUINCENAL")))
                .map(ProcesoLiquidacion::getProcesoLiquiId)
                .toList();

        Map<Long, Integer> countsPorProceso = new java.util.HashMap<>();
        Map<Long, BigDecimal> netosPorProceso = new java.util.HashMap<>();

        if (!idsNomina.isEmpty()) {
            nominaCabeceraRepository.countByProcesoIds(idsNomina)
                    .forEach(row -> countsPorProceso.put(
                            ((Number) row[0]).longValue(),
                            ((Number) row[1]).intValue()));

            nominaCabeceraRepository.sumNetoByProcesoIds(idsNomina)
                    .forEach(row -> netosPorProceso.put(
                            ((Number) row[0]).longValue(),
                            (BigDecimal) row[1]));
        }

        Map<Long, com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion> cabecerasPorProceso =
                new java.util.HashMap<>(
                        cabeceraLiquiPrestacionRepository.findByProcesoIds(todosLosIds)
                                .stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion
                                                ::getFkProcesoLiquiId,
                                        c -> c)));

        List<Long> cabeceraIds = cabecerasPorProceso.values().stream()
                .map(com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion
                        ::getCabeLiquiPrestacionId)
                .toList();

        Map<Long, List<DetalleLiquiPrestacion>> detallesPorCabecera =
                cabeceraIds.isEmpty() ? new java.util.HashMap<>() :
                        new java.util.HashMap<>(
                                detalleLiquiPrestacionRepository.findByCabeceraIds(cabeceraIds)
                                        .stream()
                                        .collect(java.util.stream.Collectors.groupingBy(
                                                DetalleLiquiPrestacion::getFkCabeLiquiPrestacionId)));

        List<Integer> aniosCesantias = procesos.stream()
                .filter(p -> p.getTipoProceso() == TipoProceso.CESANTIAS_ANUAL)
                .map(ProcesoLiquidacion::getAnio)
                .distinct()
                .toList();

        if (!aniosCesantias.isEmpty()) {
            List<ProcesoLiquidacion> procesosIntereses = procesoLiquidacionRepository
                    .findByEmpresaAndTipoAndAnios(
                            procesos.get(0).getFkIdEmpresa(),
                            TipoProceso.INTERESES_CESANTIAS_ANUAL,
                            aniosCesantias);

            if (!procesosIntereses.isEmpty()) {
                List<Long> idsIntereses = procesosIntereses.stream()
                        .map(ProcesoLiquidacion::getProcesoLiquiId)
                        .toList();

                cabeceraLiquiPrestacionRepository.findByProcesoIds(idsIntereses)
                        .forEach(cab -> cabecerasPorProceso.put(
                                cab.getFkProcesoLiquiId(), cab));

                List<Long> nuevasCabeceraIds = procesosIntereses.stream()
                        .map(pi -> cabecerasPorProceso.get(pi.getProcesoLiquiId()))
                        .filter(cab -> cab != null)
                        .map(com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion
                                ::getCabeLiquiPrestacionId)
                        .toList();

                if (!nuevasCabeceraIds.isEmpty()) {
                    detalleLiquiPrestacionRepository.findByCabeceraIds(nuevasCabeceraIds)
                            .forEach(d -> detallesPorCabecera
                                    .computeIfAbsent(d.getFkCabeLiquiPrestacionId(),
                                            k -> new java.util.ArrayList<>())
                                    .add(d));
                }
            }
        }
        return procesos.stream()
                .map(proceso -> {
                    boolean esNomina = proceso.getTipoProceso() != null &&
                            (proceso.getTipoProceso().name().equals("NOMINA_MENSUAL") ||
                                    proceso.getTipoProceso().name().equals("NOMINA_QUINCENAL"));

                    boolean esCesantias = proceso.getTipoProceso() != null &&
                            proceso.getTipoProceso().name().equals("CESANTIAS_ANUAL");

                    Integer cantidadEmpleados;
                    BigDecimal totalNeto;
                    BigDecimal totalIntereses = BigDecimal.ZERO;

                    if (esNomina) {
                        cantidadEmpleados = countsPorProceso
                                .getOrDefault(proceso.getProcesoLiquiId(), 0);
                        totalNeto = netosPorProceso
                                .getOrDefault(proceso.getProcesoLiquiId(), BigDecimal.ZERO);

                    } else {
                        var cabecera = cabecerasPorProceso
                                .get(proceso.getProcesoLiquiId());

                        if (cabecera != null) {
                            List<DetalleLiquiPrestacion> detalles = detallesPorCabecera
                                    .getOrDefault(cabecera.getCabeLiquiPrestacionId(),
                                            List.of());
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
                            totalIntereses = procesoLiquidacionRepository
                                    .findByEmpresaAndTipoAndAnios(
                                            proceso.getFkIdEmpresa(),
                                            TipoProceso.INTERESES_CESANTIAS_ANUAL,
                                            List.of(proceso.getAnio()))
                                    .stream()
                                    .findFirst()
                                    .map(pi -> cabecerasPorProceso.get(pi.getProcesoLiquiId()))
                                    .map(cab -> cab == null ? BigDecimal.ZERO :
                                            detallesPorCabecera
                                                    .getOrDefault(cab.getCabeLiquiPrestacionId(), List.of())
                                                    .stream()
                                                    .map(d -> d.getValorIntCesantias() != null
                                                            ? d.getValorIntCesantias() : BigDecimal.ZERO)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                                    .orElse(BigDecimal.ZERO);
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
                            totalIntereses,
                            List.of()
                    );
                })
                .toList();
    }
}