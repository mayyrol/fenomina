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
                    String.format("Empleado %d no encontrado o no activo", empleadoId));
        }

        if (Boolean.TRUE.equals(empleado.esSalarioIntegral())) {
            log.debug("Empleado {} tiene salario integral, se omite prima ordinaria", empleadoId);
            return;
        }

        LocalDate fechaIngreso = empleado.fechaIngresoEmp();
        LocalDate fechaInicioReal = fechaIngreso.isAfter(fechaInicioPeriodo)
                ? fechaIngreso : fechaInicioPeriodo;

        int periodoInicio = fechaInicioPeriodo.getMonthValue();
        int periodoFin = fechaFinPeriodo.getMonthValue();

        // Nóminas pagadas del semestre
        List<NominaCabecera> nominasSemestre = nominaCabeceraRepository
                .findByEmpleadoAndRangoPeriodo(
                        empleadoId,
                        proceso.getAnio(),
                        periodoInicio,
                        periodoFin,
                        EstadoProceso.PAGADO
                );

        // Días laborados: suma de cantidad_concept del concepto salario (ID=1)
        int diasLiquidados;
        if (!nominasSemestre.isEmpty()) {
            diasLiquidados = nominasSemestre.stream()
                    .mapToInt(nc -> reporteNominaDetalleRepository
                            .findByFkCabecNominaId(nc.getCabecNominaId())
                            .stream()
                            .filter(d -> d.getFkConcepNominaId() != null &&
                                    d.getFkConcepNominaId().equals(1L))
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept().intValue() : 0)
                            .sum())
                    .sum();
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        } else {
            diasLiquidados = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        }

        // Calcular base y aux promedio
        BigDecimal[] resultado = calcularBaseYAux(
                nominasSemestre, empleado, periodoInicio, periodoFin);
        BigDecimal baseLiquidacion = resultado[0];
        BigDecimal auxPromedio = resultado[1];
        BigDecimal salarioPromedio = baseLiquidacion.subtract(auxPromedio);

        // Fórmula prima: base × días / 360
        BigDecimal valorPrima = baseLiquidacion
                .multiply(BigDecimal.valueOf(diasLiquidados))
                .divide(BigDecimal.valueOf(DIAS_ANIO), ESCALA, RoundingMode.HALF_UP);

        ConceptoNominaDTO conceptoPrima = conceptosPorNombre.get("Prima de servicios");

        DetalleLiquiPrestacion detalle = DetalleLiquiPrestacion.builder()
                .fkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                .fkEmpleadoId(empleadoId)
                .fkConcepNominaId(conceptoPrima != null
                        ? conceptoPrima.concepNominaId() : null)
                .fechaInicioCorteEmp(fechaInicioReal)
                .fechaFinCorteEmp(fechaFinPeriodo)
                .diasLiquidadosInt(diasLiquidados)
                .salarioFijoMomento(salarioPromedio)
                .promedioAuxTransporte(auxPromedio)
                .baseLiquiTotal(baseLiquidacion)
                .valorNetaPresta(valorPrima)
                .build();

        detalleLiquiPrestacionRepository.save(detalle);

        log.debug("Prima empleado {}: meses={}, base={}, aux={}, días={}, valor={}",
                empleadoId, diasLiquidados, baseLiquidacion, auxPromedio,
                diasLiquidados, valorPrima);
    }

    // --- Cálculo de salario base ---

    private BigDecimal[] calcularBaseYAux(
            List<NominaCabecera> nominas,
            EmpleadoDTO empleado,
            int periodoInicio,
            int periodoFin
    ) {
        if (nominas.isEmpty()) {
            return new BigDecimal[]{
                    empleado.salarioBascMensual(),
                    BigDecimal.ZERO
            };
        }

        // Agrupar por período (mes) para manejar quincenales
        Map<Integer, BigDecimal> devengadoPorMes = new java.util.LinkedHashMap<>();
        Map<Integer, BigDecimal> auxPorMes = new java.util.LinkedHashMap<>();

        for (NominaCabecera nc : nominas) {
            int mes = nc.getPeriodoCotiNomina();

            // Solo meses dentro del semestre
            if (mes < periodoInicio || mes > periodoFin) continue;

            // Sumar total devengado por mes
            devengadoPorMes.merge(mes,
                    nc.getTotalDevengadoEmp() != null
                            ? nc.getTotalDevengadoEmp() : BigDecimal.ZERO,
                    BigDecimal::add);

            // Sumar aux transporte del mes desde reporte_nomina_detalle
            BigDecimal auxMes = reporteNominaDetalleRepository
                    .findByFkCabecNominaId(nc.getCabecNominaId())
                    .stream()
                    .filter(d -> d.getFkConcepNominaId() != null
                            && d.getFkConcepNominaId().equals(22L))
                    .map(d -> d.getValorResultConcept() != null
                            ? d.getValorResultConcept() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            auxPorMes.merge(mes, auxMes, BigDecimal::add);
        }

        int mesesConNomina = devengadoPorMes.size();
        if (mesesConNomina == 0) {
            return new BigDecimal[]{empleado.salarioBascMensual(), BigDecimal.ZERO};
        }

        BigDecimal sumaDevengados = devengadoPorMes.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumaAux = auxPorMes.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal divisor = BigDecimal.valueOf(mesesConNomina);

        BigDecimal baseLiquidacion = sumaDevengados
                .divide(divisor, ESCALA, RoundingMode.HALF_UP);
        BigDecimal auxPromedio = sumaAux
                .divide(divisor, ESCALA, RoundingMode.HALF_UP);

        return new BigDecimal[]{baseLiquidacion, auxPromedio};
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