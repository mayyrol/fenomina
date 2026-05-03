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
public class LiquidacionPrimaServiceImpl implements LiquidacionPrimaService {

    private static final int ESCALA = 2;
    private static final int DIAS_SEMESTRE = 180;
    private static final int DIAS_ANIO = 360;

    private final MasterDataClientWrapper masterDataClient;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    @Override
    @Transactional
    public void liquidar(ProcesoLiquidacion proceso, List<Long> empleadosSeleccionados) {
        log.info("Iniciando liquidación prima - proceso: {}", proceso.getProcesoLiquiId());

        // --- 1. Datos compartidos ---
        List<ParametroGeneralDTO> todosParametros = masterDataClient.findAllParametros();
        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();

        Map<String, ConceptoNominaDTO> conceptosPorNombre = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::nombreConcepNomina, c -> c));

        LocalDate fechaFin = proceso.getFechaFinPeriodo();
        LocalDate fechaInicio = proceso.getFechaInicioPeriodo();

        BigDecimal smmlv = resolverValorParametro(todosParametros, "SMMLV", fechaFin);
        BigDecimal auxTransporte = resolverValorParametro(
                todosParametros, "AUXILIO_TRANSPORTE", fechaFin);

        List<EmpleadoDTO> empleadosActivos = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleadosActivos.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        // --- 2. Crear cabecera del proceso de prima ---
        CabeceraLiquiPrestacion cabecera = new CabeceraLiquiPrestacion();
        cabecera.setFkProcesoLiquiId(proceso.getProcesoLiquiId());
        cabecera.setAnioLiquiPrestacion(proceso.getAnio());
        cabecera.setPeriodoLiquiPrestacion(proceso.getPeriodo());
        cabecera.setFinicioGeneralLiquiPrest(fechaInicio);
        cabecera.setFfinalGeneralLiquiPrest(fechaFin);
        cabecera = cabeceraLiquiPrestacionRepository.save(cabecera);

        // --- 3. Procesar cada empleado ---
        for (Long empleadoId : empleadosSeleccionados) {
            try {
                procesarEmpleado(
                        proceso,
                        empleadoId,
                        empleadosPorId,
                        cabecera,
                        fechaInicio,
                        fechaFin,
                        smmlv,
                        auxTransporte,
                        conceptosPorNombre,
                        todosParametros
                );
            } catch (Exception e) {
                log.error("Error liquidando prima empleado {}: {}", empleadoId, e.getMessage());
                throw new CalculoNominaException(
                        String.format("Error al liquidar prima empleado %d: %s",
                                empleadoId, e.getMessage())
                );
            }
        }

        log.info("Liquidación prima completada - proceso: {}", proceso.getProcesoLiquiId());
    }

    private void procesarEmpleado(
            ProcesoLiquidacion proceso,
            Long empleadoId,
            Map<Long, EmpleadoDTO> empleadosPorId,
            CabeceraLiquiPrestacion cabecera,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo,
            BigDecimal smmlv,
            BigDecimal auxTransporte,
            Map<String, ConceptoNominaDTO> conceptosPorNombre,
            List<ParametroGeneralDTO> todosParametros
    ) {
        EmpleadoDTO empleado = empleadosPorId.get(empleadoId);
        if (empleado == null) {
            throw new CalculoNominaException(
                    String.format("Empleado %d no encontrado o no activo", empleadoId)
            );
        }

        // Salario integral no genera prima ordinaria
        if (Boolean.TRUE.equals(empleado.esSalarioIntegral())) {
            log.debug("Empleado {} tiene salario integral, se omite prima ordinaria", empleadoId);
            return;
        }

        // --- Fechas reales del empleado en el semestre ---
        LocalDate fechaIngreso = empleado.fechaIngresoEmp();
        LocalDate fechaInicioReal = fechaIngreso.isAfter(fechaInicioPeriodo)
                ? fechaIngreso
                : fechaInicioPeriodo;

        int periodoInicio = fechaInicioPeriodo.getMonthValue();
        int periodoFin = fechaFinPeriodo.getMonthValue();

        List<NominaCabecera> nominasSemestre = nominaCabeceraRepository
                .findByEmpleadoAndRangoPeriodo(
                        empleadoId,
                        proceso.getAnio(),
                        periodoInicio,
                        periodoFin,
                        EstadoProceso.PAGADO
                );

        int diasLiquidados;
        if (!nominasSemestre.isEmpty()) {
            // Sumar días laborados reales desde reporte_nomina_detalle (concepto ID=1)
            diasLiquidados = nominasSemestre.stream()
                    .mapToInt(nc -> reporteNominaDetalleRepository
                            .findByFkCabecNominaId(nc.getCabecNominaId())
                            .stream()
                            .filter(d -> d.getFkConcepNominaId() != null &&
                                    d.getFkConcepNominaId() == 1L)
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept() : 0)
                            .sum())
                    .sum();
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        } else {
            // Sin nóminas pagadas, calcular por fechas
            diasLiquidados = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        }

        // --- Base de liquidación ---
        // Para salario fijo: salario del último mes del semestre
        // Para salario variable: promedio de devengados salariales del semestre
        BigDecimal salarioBase;
        BigDecimal auxTransporteEmpleado = Boolean.TRUE.equals(empleado.tieneAuxTransporte())
                ? auxTransporte : BigDecimal.ZERO;

        if (nominasSemestre.isEmpty()) {
            // Sin nóminas pagadas en el semestre, se usa el salario registrado
            salarioBase = empleado.salarioBascMensual();
        } else {
            salarioBase = calcularSalarioBase(nominasSemestre, empleado, conceptosPorNombre);
        }

        // --- Fórmula prima: (salarioBase + auxTransporte) * diasLiquidados / 360 ---
        BigDecimal baseLiquidacion = salarioBase.add(auxTransporteEmpleado);
        BigDecimal valorPrima = baseLiquidacion
                .multiply(BigDecimal.valueOf(diasLiquidados))
                .divide(BigDecimal.valueOf(DIAS_ANIO), ESCALA, RoundingMode.HALF_UP);

        // --- Persistir detalle ---
        ConceptoNominaDTO conceptoPrima = conceptosPorNombre.get("Prima de servicios");

        DetalleLiquiPrestacion detalle = DetalleLiquiPrestacion.builder()
                .fkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                .fkEmpleadoId(empleadoId)
                .fkConcepNominaId(conceptoPrima != null
                        ? conceptoPrima.concepNominaId() : null)
                .fechaInicioCorteEmp(fechaInicioReal)
                .fechaFinCorteEmp(fechaFinPeriodo)
                .diasLiquidadosInt(diasLiquidados)
                .salarioFijoMomento(salarioBase)
                .baseLiquiTotal(baseLiquidacion)
                .valorNetaPresta(valorPrima)
                .build();

        detalleLiquiPrestacionRepository.save(detalle);

        log.debug("Prima empleado {}: días={}, base={}, valor={}",
                empleadoId, diasLiquidados, baseLiquidacion, valorPrima);
    }

    // --- Cálculo de salario base ---

    private BigDecimal calcularSalarioBase(
            List<NominaCabecera> nominasSemestre,
            EmpleadoDTO empleado,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        if (nominasSemestre.isEmpty()) {
            return empleado.salarioBascMensual();
        }

        List<NominaCabecera> ultimas3 = nominasSemestre.stream()
                .sorted((a, b) -> b.getPeriodoCotiNomina()
                        .compareTo(a.getPeriodoCotiNomina()))
                .limit(3)
                .collect(Collectors.toList());

        ConceptoNominaDTO conceptoSalario = conceptosPorNombre.get("Salario días trabajados");
        if (conceptoSalario == null) {
            return empleado.salarioBascMensual();
        }

        List<BigDecimal> salariosUltimas3 = ultimas3.stream()
                .map(nc -> reporteNominaDetalleRepository
                        .findByFkCabecNominaId(nc.getCabecNominaId())
                        .stream()
                        .filter(d -> conceptoSalario.concepNominaId()
                                .equals(d.getFkConcepNominaId()))
                        .map(d -> d.getBaseCalculoConcept() != null
                                ? d.getBaseCalculoConcept()
                                : BigDecimal.ZERO)
                        .findFirst()
                        .orElse(BigDecimal.ZERO))
                .collect(Collectors.toList());

        boolean salarioVario = salariosUltimas3.stream()
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .distinct()
                .count() > 1;

        if (!salarioVario && !salariosUltimas3.isEmpty()
                && salariosUltimas3.get(0).compareTo(BigDecimal.ZERO) > 0) {
            return salariosUltimas3.get(0);
        }

        List<BigDecimal> devengadosPorPeriodo = nominasSemestre.stream()
                .map(nc -> {
                    List<com.fenomina.payroll_engine.entity.ReporteNominaDetalle> detalles =
                            reporteNominaDetalleRepository
                                    .findByFkCabecNominaId(nc.getCabecNominaId());

                    return detalles.stream()
                            .filter(d -> {
                                ConceptoNominaDTO concepto = conceptosPorNombre.values()
                                        .stream()
                                        .filter(c -> c.concepNominaId()
                                                .equals(d.getFkConcepNominaId()))
                                        .findFirst()
                                        .orElse(null);
                                return concepto != null
                                        && Boolean.TRUE.equals(concepto.esSalario())
                                        && !"Vacaciones compensadas en dinero"
                                        .equals(concepto.nombreConcepNomina());
                            })
                            .map(d -> d.getValorResultConcept() != null
                                    ? d.getValorResultConcept()
                                    : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .collect(Collectors.toList());

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
                .orElseThrow(() -> new com.fenomina.payroll_engine.exception.ParametroNoEncontradoException(
                        String.format("Parámetro %s no encontrado para fecha %s", nombre, fecha)
                ));
    }
}