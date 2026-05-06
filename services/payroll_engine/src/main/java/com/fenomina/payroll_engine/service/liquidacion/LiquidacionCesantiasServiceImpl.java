package com.fenomina.payroll_engine.service.liquidacion;

import com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion;
import com.fenomina.payroll_engine.entity.DetalleLiquiPrestacion;
import com.fenomina.payroll_engine.entity.NominaCabecera;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.exception.CalculoNominaException;
import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.client.dto.ParametroGeneralDTO;
import com.fenomina.payroll_engine.repository.CabeceraLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.DetalleLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import com.fenomina.payroll_engine.repository.ReporteNominaDetalleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiquidacionCesantiasServiceImpl implements LiquidacionCesantiasService {

    private static final int ESCALA = 2;
    private static final int DIAS_ANIO = 360;

    private final MasterDataClientWrapper masterDataClient;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    @Override
    @Transactional
    public void liquidar(ProcesoLiquidacion proceso, List<Long> empleadosSeleccionados) {
        log.info("Iniciando liquidación cesantías - proceso: {}", proceso.getProcesoLiquiId());

        List<ParametroGeneralDTO> todosParametros = masterDataClient.findAllParametros();
        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();

        Map<String, ConceptoNominaDTO> conceptosPorNombre = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::nombreConcepNomina, c -> c));

        LocalDate fechaFin = proceso.getFechaFinPeriodo();
        LocalDate fechaInicio = proceso.getFechaInicioPeriodo();

        BigDecimal auxTransporte = resolverValorParametro(
                todosParametros, "AUXILIO_TRANSPORTE", fechaFin);

        List<EmpleadoDTO> empleadosActivos = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleadosActivos.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        CabeceraLiquiPrestacion cabecera = new CabeceraLiquiPrestacion();
        cabecera.setFkProcesoLiquiId(proceso.getProcesoLiquiId());
        cabecera.setAnioLiquiPrestacion(proceso.getAnio());
        cabecera.setPeriodoLiquiPrestacion(1);
        cabecera.setFinicioGeneralLiquiPrest(fechaInicio);
        cabecera.setFfinalGeneralLiquiPrest(fechaFin);
        cabecera = cabeceraLiquiPrestacionRepository.save(cabecera);

        for (Long empleadoId : empleadosSeleccionados) {
            try {
                procesarEmpleado(
                        proceso,
                        empleadoId,
                        empleadosPorId,
                        cabecera,
                        fechaInicio,
                        fechaFin,
                        auxTransporte,
                        conceptosPorNombre
                );
            } catch (Exception e) {
                log.error("Error liquidando cesantías empleado {}: {}",
                        empleadoId, e.getMessage());
                throw new CalculoNominaException(
                        String.format("Error al liquidar cesantías empleado %d: %s",
                                empleadoId, e.getMessage())
                );
            }
        }

        log.info("Liquidación cesantías completada - proceso: {}", proceso.getProcesoLiquiId());
    }

    private void procesarEmpleado(
            ProcesoLiquidacion proceso,
            Long empleadoId,
            Map<Long, EmpleadoDTO> empleadosPorId,
            CabeceraLiquiPrestacion cabecera,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo,
            BigDecimal auxTransporte,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        EmpleadoDTO empleado = empleadosPorId.get(empleadoId);
        if (empleado == null) {
            throw new CalculoNominaException(
                    String.format("Empleado %d no encontrado o no activo", empleadoId)
            );
        }

        if (Boolean.TRUE.equals(empleado.esSalarioIntegral())) {
            log.debug("Empleado {} tiene salario integral, se omiten cesantías ordinarias",
                    empleadoId);
            return;
        }

        // --- Fechas reales del empleado en el año ---
        LocalDate fechaIngreso = empleado.fechaIngresoEmp();
        LocalDate fechaInicioReal = fechaIngreso.isAfter(fechaInicioPeriodo)
                ? fechaIngreso
                : fechaInicioPeriodo;

        // --- Nóminas pagadas del año ---
        List<NominaCabecera> nominasAnio = nominaCabeceraRepository
                .findByEmpleadoAndRangoPeriodo(
                        empleadoId,
                        proceso.getAnio(),
                        1,
                        12,
                        EstadoProceso.PAGADO
                );

        int diasLiquidados;
        if (!nominasAnio.isEmpty()) {
            diasLiquidados = nominasAnio.stream()
                    .mapToInt(nc -> reporteNominaDetalleRepository
                            .findByFkCabecNominaId(nc.getCabecNominaId())
                            .stream()
                            .filter(d -> d.getFkConcepNominaId() != null &&
                                    d.getFkConcepNominaId().equals(1L))
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept() : 0)
                            .sum())
                    .sum();
            diasLiquidados = Math.min(diasLiquidados, DIAS_ANIO);
        } else {
            diasLiquidados = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
            diasLiquidados = Math.min(diasLiquidados, DIAS_ANIO);
        }

        // --- Base de liquidación ---
        // Si salario no varió en últimos 3 meses: último salario mensual devengado
        // Si varió o es variable: promedio del último año
        BigDecimal salarioBase = calcularSalarioBaseCesantias(
                nominasAnio, empleado, conceptosPorNombre);

        BigDecimal auxTransporteEmpleado = Boolean.TRUE.equals(empleado.tieneAuxTransporte())
                ? auxTransporte : BigDecimal.ZERO;

        // --- Fórmula cesantías: salarioBase * diasLiquidados / 360 ---
        BigDecimal baseLiquidacion = salarioBase.add(auxTransporteEmpleado);
        BigDecimal valorCesantias = baseLiquidacion
                .multiply(BigDecimal.valueOf(diasLiquidados))
                .divide(BigDecimal.valueOf(DIAS_ANIO), ESCALA, RoundingMode.HALF_UP);

        // --- Intereses sobre cesantías ---
        // Fórmula: (cesantías * días * 0.12) / 360
        BigDecimal valorIntereses = valorCesantias
                .multiply(BigDecimal.valueOf(diasLiquidados))
                .multiply(new BigDecimal("0.12"))
                .divide(BigDecimal.valueOf(DIAS_ANIO), ESCALA, RoundingMode.HALF_UP);

        // --- Persistir detalle cesantías ---
        ConceptoNominaDTO conceptoCesantias = conceptosPorNombre.get("Cesantías");
        ConceptoNominaDTO conceptoIntereses = conceptosPorNombre
                .get("Intereses sobre las cesantías");

        DetalleLiquiPrestacion detalleCesantias = DetalleLiquiPrestacion.builder()
                .fkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                .fkEmpleadoId(empleadoId)
                .fkConcepNominaId(conceptoCesantias != null
                        ? conceptoCesantias.concepNominaId() : null)
                .fechaInicioCorteEmp(fechaInicioReal)
                .fechaFinCorteEmp(fechaFinPeriodo)
                .diasLiquidadosInt(diasLiquidados)
                .salarioFijoMomento(salarioBase)
                .baseLiquiTotal(baseLiquidacion)
                .valorNetaPresta(valorCesantias)
                .valorIntCesantias(valorIntereses)
                .build();

        detalleLiquiPrestacionRepository.save(detalleCesantias);

        log.debug("Cesantías empleado {}: días={}, base={}, cesantías={}, intereses={}",
                empleadoId, diasLiquidados, baseLiquidacion, valorCesantias, valorIntereses);
    }

    // --- Cálculo de salario base cesantías ---

    private BigDecimal calcularSalarioBaseCesantias(
            List<NominaCabecera> nominasAnio,
            EmpleadoDTO empleado,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        if (nominasAnio.isEmpty()) {
            return empleado.salarioBascMensual();
        }

        List<Long> conceptosSalarialesIds = conceptosPorNombre.values().stream()
                .filter(c -> Boolean.TRUE.equals(c.esSalario())
                        || "Auxilio de transporte".equals(c.nombreConcepNomina()))
                .filter(c -> !"Vacaciones compensadas en dinero"
                        .equals(c.nombreConcepNomina()))
                .map(ConceptoNominaDTO::concepNominaId)
                .collect(Collectors.toList());

        List<BigDecimal> devengadosPorPeriodo = nominasAnio.stream()
                .map(nc -> {
                    List<com.fenomina.payroll_engine.entity.ReporteNominaDetalle> detalles =
                            reporteNominaDetalleRepository
                                    .findByFkCabecNominaId(nc.getCabecNominaId());

                    return detalles.stream()
                            .filter(d -> conceptosSalarialesIds
                                    .contains(d.getFkConcepNominaId()))
                            .map(d -> d.getValorResultConcept() != null
                                    ? d.getValorResultConcept()
                                    : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (devengadosPorPeriodo.isEmpty()) {
            return empleado.salarioBascMensual();
        }

        BigDecimal sumaDevengados = devengadosPorPeriodo.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sumaDevengados
                .divide(BigDecimal.valueOf(devengadosPorPeriodo.size()),
                        ESCALA, RoundingMode.HALF_UP);
    }

    // --- Helpers ---

    private BigDecimal resolverValorParametro(
            List<ParametroGeneralDTO> parametros,
            String nombre,
            LocalDate fecha
    ) {
        return parametros.stream()
                .filter(p -> p.nombreParamGeneral().equals(nombre))
                .filter(p -> !p.fechaParamGeneral().isAfter(fecha))
                .max(java.util.Comparator.comparing(ParametroGeneralDTO::fechaParamGeneral))
                .map(ParametroGeneralDTO::valorParamGeneral)
                .orElseThrow(() -> new com.fenomina.payroll_engine.exception
                        .ParametroNoEncontradoException(
                        String.format("Parámetro %s no encontrado para fecha %s",
                                nombre, fecha)
                ));
    }
}
