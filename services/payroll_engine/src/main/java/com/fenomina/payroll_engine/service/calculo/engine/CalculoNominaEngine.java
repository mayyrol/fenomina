package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.entity.NominaCabecera;
import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.entity.ReporteNominaDetalle;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.*;
import com.fenomina.payroll_engine.exception.CalculoNominaException;
import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.*;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import com.fenomina.payroll_engine.repository.NovedadRepository;
import com.fenomina.payroll_engine.repository.ReporteNominaDetalleRepository;
import com.fenomina.payroll_engine.service.calculo.concepto.DevengosCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalculoNominaEngine {

    private static final int ESCALA = 2;
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String LICENCIA_NO_REMUNERADA = "Licencias no remuneradas";

    private final IBCCalculator ibcCalculator;
    private final DevengosCalculator devengosCalculator;
    private final DeduccionesCalculator deduccionesCalculator;
    private final SeguridadSocialCalculator seguridadSocialCalculator;
    private final ProvisionesCalculator provisionesCalculator;
    private final ParametroGeneralHelper parametroHelper;
    private final MasterDataClientWrapper masterDataClient;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final NovedadRepository novedadRepository;

    @Transactional
    public void liquidar(
            ProcesoLiquidacion proceso,
            List<Long> empleadosSeleccionados,
            Map<Long, Integer> diasLaboradosPorEmpleado

    ) {
        log.info("Iniciando liquidación nómina - proceso: {}, empleados: {}",
                proceso.getProcesoLiquiId(), empleadosSeleccionados.size());

        // --- 1. Cargar datos compartidos una sola vez ---
        List<ParametroGeneralDTO> todosParametros = masterDataClient.findAllParametros();
        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();
        EmpresaDTO empresa = masterDataClient.findEmpresaById(proceso.getFkIdEmpresa());

        Map<Long, ConceptoNominaDTO> conceptosPorId = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::concepNominaId, c -> c));

        Map<String, ConceptoNominaDTO> conceptosPorNombre = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::nombreConcepNomina, c -> c));

        // Parámetros resueltos por fecha del periodo
        LocalDate fechaFin = proceso.getFechaFinPeriodo();
        Map<String, ParametroGeneralDTO> parametrosPorNombre =
                resolverParametrosPorFecha(todosParametros, fechaFin);

        // Novedades del proceso agrupadas por empleado
        List<Novedad> todasNovedades = novedadRepository
                .findByProcesoLiquid(proceso.getProcesoLiquiId());

        Map<Long, List<Novedad>> novedadesPorEmpleado = todasNovedades.stream()
                .collect(Collectors.groupingBy(Novedad::getFkEmpleadoId));

        // Dentro de liquidar(), junto a la carga de datos compartidos
        List<EmpleadoDTO> empleadosActivos = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleadosActivos.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        // --- 2. Procesar cada empleado ---
        for (Long empleadoId : empleadosSeleccionados) {
            try {
                procesarEmpleado(
                        proceso,
                        empleadoId,
                        diasLaboradosPorEmpleado.getOrDefault(empleadoId, 30),
                        novedadesPorEmpleado.getOrDefault(empleadoId, List.of()),
                        conceptosPorId,
                        conceptosPorNombre,
                        parametrosPorNombre,
                        empresa,
                        empleadosPorId
                );
            } catch (Exception e) {
                log.error("Error al liquidar empleado {} en proceso {}: {}",
                        empleadoId, proceso.getProcesoLiquiId(), e.getMessage());
                throw new CalculoNominaException(
                        String.format("Error al liquidar empleado %d: %s",
                                empleadoId, e.getMessage())
                );
            }
        }

        log.info("Liquidación completada - proceso: {}", proceso.getProcesoLiquiId());
    }

    private void procesarEmpleado(
            ProcesoLiquidacion proceso,
            Long empleadoId,
            Integer diasLaborados,
            List<Novedad> novedades,
            Map<Long, ConceptoNominaDTO> conceptosPorId,
            Map<String, ConceptoNominaDTO> conceptosPorNombre,
            Map<String, ParametroGeneralDTO> parametrosPorNombre,
            EmpresaDTO empresa,
            Map<Long, EmpleadoDTO> empleadosPorId
    ) {
        log.debug("Procesando empleado: {}", empleadoId);

        // --- a. Cargar datos del empleado ---
        EmpleadoDTO empleado = empleadosPorId.get(empleadoId);
        if (empleado == null) {
            throw new CalculoNominaException(
                    String.format("Empleado %d no encontrado o no activo", empleadoId)
            );
        }

        List<ContratoConceptoDTO> conceptosFijos = masterDataClient
                .findConceptosFijosByEmpleado(empleadoId);

        // --- b. IBC histórico del periodo anterior ---
        NominaCabecera ultimaCabecera = nominaCabeceraRepository
                .findUltimoIbcByEmpleado(empleadoId,
                        com.fenomina.payroll_engine.enums.EstadoProceso.PAGADO)
                .orElse(null);

        BigDecimal ibcSaludAnterior = ultimaCabecera != null
                ? ultimaCabecera.getIbcSalud() : null;
        BigDecimal ibcPensionAnterior = ultimaCabecera != null
                ? ultimaCabecera.getIbcPension() : null;

        // --- c. Días de licencia no remunerada ---
        Integer diasLicenciaNoRemunerada = calcularDiasLicenciaNoRemunerada(
                novedades, conceptosPorNombre);

        // --- d. Construir contexto ---
        ContextoLiquidacion ctx = ContextoLiquidacion.builder()
                .empleado(empleado)
                .procesoId(proceso.getProcesoLiquiId())
                .anio(proceso.getAnio())
                .periodo(proceso.getPeriodo())
                .fechaInicioPeriodo(proceso.getFechaInicioPeriodo())
                .fechaFinPeriodo(proceso.getFechaFinPeriodo())
                .esQuincenal(proceso.getTipoProceso() ==
                        com.fenomina.payroll_engine.enums.TipoProceso.NOMINA_QUINCENAL)
                .diasLaborados(diasLaborados)
                .diasLicenciaNoRemunerada(diasLicenciaNoRemunerada)
                .novedades(novedades)
                .conceptosFijos(conceptosFijos)
                .conceptosPorId(conceptosPorId)
                .conceptosPorNombre(conceptosPorNombre)
                .parametrosPorNombre(parametrosPorNombre)
                .ibcSaludAnterior(ibcSaludAnterior)
                .ibcPensionAnterior(ibcPensionAnterior)
                .esEmpresaExoneradaParafiscales(empresa.esExoneradaLey1607())
                .build();

        // --- e. Ejecutar calculators en orden ---
        List<DevengoCalculado> devengos = devengosCalculator.calcular(ctx);

        BigDecimal totalDevengadoSalarial = devengos.stream()
                .filter(d -> !d.isEsInformativo())
                .filter(DevengoCalculado::isEsSalario)
                .map(DevengoCalculado::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        IBCCalculado ibc = ibcCalculator.calcular(ctx, totalDevengadoSalarial);

        List<DeduccionCalculada> deducciones = deduccionesCalculator.calcular(ctx, ibc);

        List<AportePatronalCalculado> aportesPatronales =
                seguridadSocialCalculator.calcular(ctx, ibc);

        List<ProvisionCalculada> provisiones = provisionesCalculator.calcular(ctx, devengos);

        // --- f. Consolidar totales ---
        BigDecimal totalDevengado = devengos.stream()
                .filter(d -> !d.isEsInformativo())
                .map(DevengoCalculado::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeducciones = deducciones.stream()
                .filter(d -> !d.isEsAporteLicenciaNoRemunerada())
                .map(DeduccionCalculada::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netoNomina = totalDevengado
                .subtract(totalDeducciones)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal totalAportesPatronales = aportesPatronales.stream()
                .filter(a -> !a.isEsAporteLicenciaNoRemunerada())
                .map(AportePatronalCalculado::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Provisiones: excluye cesantías informativas del costo empresa
        BigDecimal totalProvisiones = provisiones.stream()
                .filter(p -> !p.isEsInformativo())
                .map(ProvisionCalculada::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoTotalEmpresa = netoNomina
                .add(totalAportesPatronales)
                .add(totalProvisiones)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        // --- g. Persistir cabecera ---
        NominaCabecera cabecera = NominaCabecera.builder()
                .fkEmpleadoId(empleadoId)
                .fkProcesoLiquiId(proceso.getProcesoLiquiId())
                .anioCabecNomina(proceso.getAnio())
                .periodoCotiNomina(proceso.getPeriodo())
                .totalDevengadoEmp(totalDevengado)
                .totalDeduccionEmp(totalDeducciones)
                .netoNominaEmp(netoNomina)
                .costoTotalEmpresa(costoTotalEmpresa)
                .totalApPatronales(totalAportesPatronales)
                .totalProvisiones(totalProvisiones)
                .ibcSalud(ibc.getIbcSalud())
                .ibcPension(ibc.getIbcPension())
                .build();

        cabecera = nominaCabeceraRepository.save(cabecera);

        // --- h. Persistir detalles ---
        List<ReporteNominaDetalle> detalles = new ArrayList<>();

        detalles.addAll(mapearDevengos(devengos, cabecera.getCabecNominaId()));
        detalles.addAll(mapearDeducciones(deducciones, cabecera.getCabecNominaId(),
                conceptosPorNombre));
        detalles.addAll(mapearAportesPatronales(aportesPatronales,
                cabecera.getCabecNominaId(), conceptosPorNombre));
        detalles.addAll(mapearProvisiones(provisiones, cabecera.getCabecNominaId(),
                conceptosPorNombre));

        reporteNominaDetalleRepository.saveAll(detalles);

        log.debug("Empleado {} liquidado. Neto: {}", empleadoId, netoNomina);
    }

    // --- Resolución de parámetros por fecha ---

    private Map<String, ParametroGeneralDTO> resolverParametrosPorFecha(
            List<ParametroGeneralDTO> parametros,
            LocalDate fecha
    ) {
        // Para cada nombre de parámetro, toma el registro más reciente
        // cuya fecha sea <= fecha del periodo
        return parametros.stream()
                .filter(p -> !p.fechaParamGeneral().isAfter(fecha))
                .collect(Collectors.toMap(
                        ParametroGeneralDTO::nombreParamGeneral,
                        p -> p,
                        (existing, replacement) ->
                                existing.fechaParamGeneral()
                                        .isAfter(replacement.fechaParamGeneral())
                                        ? existing : replacement
                ));
    }

    // --- Días de licencia no remunerada ---

    private Integer calcularDiasLicenciaNoRemunerada(
            List<Novedad> novedades,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        ConceptoNominaDTO conceptoLnr = conceptosPorNombre.get(LICENCIA_NO_REMUNERADA);
        if (conceptoLnr == null) return 0;

        return novedades.stream()
                .filter(n -> n.getFkConcepNominaId()
                        .equals(conceptoLnr.concepNominaId()))
                .mapToInt(n -> n.getCantidadDiasNovedad() != null
                        ? n.getCantidadDiasNovedad() : 0)
                .sum();
    }

    // --- Mapeo a ReporteNominaDetalle ---

    private List<ReporteNominaDetalle> mapearDevengos(
            List<DevengoCalculado> devengos,
            Long cabecNominaId
    ) {
        return devengos.stream()
                .map(d -> ReporteNominaDetalle.builder()
                        .fkCabecNominaId(cabecNominaId)
                        .fkConcepNominaId(d.getConcepNominaId())
                        .fkNovedadId(d.getNovedadId())
                        .cantidadConcept(d.getCantidad())
                        .baseCalculoConcept(d.getBaseCalculo())
                        .valorResultConcept(d.getValorResultado())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ReporteNominaDetalle> mapearDeducciones(
            List<DeduccionCalculada> deducciones,
            Long cabecNominaId,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        return deducciones.stream()
                .map(d -> {
                    ConceptoNominaDTO concepto = conceptosPorNombre
                            .get(d.getNombreConcepto());
                    return ReporteNominaDetalle.builder()
                            .fkCabecNominaId(cabecNominaId)
                            .fkConcepNominaId(concepto != null
                                    ? concepto.concepNominaId() : null)
                            .fkNovedadId(d.getNovedadId())
                            .cantidadConcept(null)
                            .baseCalculoConcept(d.getBaseCalculo())
                            .valorResultConcept(d.getValorResultado())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ReporteNominaDetalle> mapearAportesPatronales(
            List<AportePatronalCalculado> aportes,
            Long cabecNominaId,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        return aportes.stream()
                .map(a -> {
                    ConceptoNominaDTO concepto = conceptosPorNombre
                            .get(a.getNombreConcepto());
                    return ReporteNominaDetalle.builder()
                            .fkCabecNominaId(cabecNominaId)
                            .fkConcepNominaId(concepto != null
                                    ? concepto.concepNominaId() : null)
                            .cantidadConcept(null)
                            .baseCalculoConcept(a.getBaseCalculo())
                            .valorResultConcept(a.getValorResultado())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ReporteNominaDetalle> mapearProvisiones(
            List<ProvisionCalculada> provisiones,
            Long cabecNominaId,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        return provisiones.stream()
                .map(p -> {
                    ConceptoNominaDTO concepto = conceptosPorNombre
                            .get(p.getNombreConcepto());
                    return ReporteNominaDetalle.builder()
                            .fkCabecNominaId(cabecNominaId)
                            .fkConcepNominaId(concepto != null
                                    ? concepto.concepNominaId() : null)
                            .cantidadConcept(null)
                            .baseCalculoConcept(p.getBaseCalculo())
                            .valorResultConcept(p.getValorResultado())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
