package com.fenomina.payroll_engine.service.liquidacion;

import com.fenomina.payroll_engine.entity.*;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.exception.CalculoNominaException;
import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.client.dto.ParametroGeneralDTO;
import com.fenomina.payroll_engine.repository.*;
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
    // IDs de conceptos que cuentan para días base de prima/cesantías
    private static final java.util.Set<Long> IDS_DIAS_BASE = java.util.Set.of(
            1L,   // Salario días trabajados
            2L,   // Vacaciones disfrutadas
            4L,   // Incapacidad por enfermedad general
            5L,   // Incapacidad por origen laboral
            6L,   // Licencia de maternidad
            7L,   // Licencia de paternidad
            8L,   // Licencia por calamidad doméstica
            9L,   // Licencia por matrimonio
            10L,  // Licencia Ley ISAAC
            11L,  // Licencia por sufragio
            12L,  // Cargos transitorios
            13L,  // Citaciones judiciales
            14L   // Otros permisos remunerados pactados
            // 15L excluido: Licencias no remuneradas
            // 3L excluido: Vacaciones compensadas en dinero (pago, no días)
    );

    private static final java.util.Set<Long> IDS_AUSENCIAS = java.util.Set.of(
            2L,   // Vacaciones disfrutadas
            4L,   // Incapacidad por enfermedad general
            5L,   // Incapacidad por origen laboral
            6L,   // Licencia de maternidad
            7L,   // Licencia de paternidad
            8L,   // Licencia por calamidad doméstica
            9L,   // Licencia por matrimonio
            10L,  // Licencia Ley ISAAC
            11L,  // Licencia por sufragio
            12L,  // Cargos transitorios
            13L,  // Citaciones judiciales
            14L,  // Otros permisos remunerados pactados
            15L   // Licencias no remuneradas
    );

    private final MasterDataClientWrapper masterDataClient;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;
    private final NovedadRepository novedadRepository;

    @Override
    @Transactional
    public void liquidar(ProcesoLiquidacion proceso, List<Long> empleadosSeleccionados) {
        log.info("Iniciando liquidación prima - proceso: {}", proceso.getProcesoLiquiId());

        // --- 1. Datos compartidos ---
        List<ParametroGeneralDTO> todosParametros = masterDataClient.findAllParametros();
        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();

        Map<String, ConceptoNominaDTO> conceptosPorNombre = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::nombreConcepNomina, c -> c));

        java.util.Set<Long> idsNoSalariales = java.util.stream.Stream.of(
                        "Beneficios o extralegales no salariales",
                        "Otro concepto a devenir no salarial",
                        "Otros pagos que no constituyen salario permanente",
                        "Bonificaciones ocasionales o por mera liberalidad"
                ).map(nombre -> conceptosPorNombre.get(nombre))
                .filter(c -> c != null)
                .map(ConceptoNominaDTO::concepNominaId)
                .collect(java.util.stream.Collectors.toSet());

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
                        todosParametros,
                        idsNoSalariales
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
            List<ParametroGeneralDTO> todosParametros,
            java.util.Set<Long> idsNoSalariales
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

        // Días laborados: suma de cantidad_concept
        int diasLiquidados;
        if (!nominasSemestre.isEmpty()) {
            diasLiquidados = nominasSemestre.stream()
                    .mapToInt(nc -> reporteNominaDetalleRepository
                            .findByFkCabecNominaId(nc.getCabecNominaId())
                            .stream()
                            .filter(d -> d.getFkConcepNominaId() != null &&
                                    IDS_DIAS_BASE.contains(d.getFkConcepNominaId()))
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept().intValue() : 0)
                            .sum())
                    .sum();
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        } else {
            diasLiquidados = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
            diasLiquidados = Math.min(diasLiquidados, DIAS_SEMESTRE);
        }

        int diasCalendarioVinculado = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
        diasCalendarioVinculado = Math.min(diasCalendarioVinculado, DIAS_SEMESTRE);

        // Calcular base y aux promedio pasándole diasCalendarioVinculado
        BigDecimal[] resultado = calcularBaseYAux(
                nominasSemestre, empleado, periodoInicio, periodoFin,
                idsNoSalariales, auxTransporte, diasLiquidados, diasCalendarioVinculado);
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
            int periodoFin,
            java.util.Set<Long> idsNoSalariales,
            BigDecimal auxTransporteCompleto,
            int diasLiquidados,
            int diasCalendarioVinculado
    ) {
        if (nominas.isEmpty()) {
            return new BigDecimal[]{
                    empleado.salarioBascMensual(),
                    BigDecimal.ZERO
            };
        }

        boolean empleadoTieneAux = Boolean.TRUE.equals(empleado.tieneAuxTransporte());

        BigDecimal totalDevengadoAcumulado = BigDecimal.ZERO;
        BigDecimal totalAuxAcumuladoSinNovedades = BigDecimal.ZERO;
        BigDecimal totalAuxAcumuladoConNovedades = BigDecimal.ZERO;
        java.util.Set<Integer> todosMesesConNomina = new java.util.HashSet<>();

        for (NominaCabecera nc : nominas) {
            int mes = nc.getPeriodoCotiNomina();

            if (mes < periodoInicio || mes > periodoFin) continue;

            todosMesesConNomina.add(mes);

            List<com.fenomina.payroll_engine.entity.ReporteNominaDetalle> detalles =
                    reporteNominaDetalleRepository.findByFkCabecNominaId(nc.getCabecNominaId());

            List<com.fenomina.payroll_engine.entity.Novedad> novedadesMes = novedadRepository
                    .findByEmpleadoAnioPeriodoActivas(
                            empleado.empleadoId(), nc.getAnioCabecNomina(), mes);

            boolean sinNovedades = novedadesMes.isEmpty();
            boolean soloNovedadesNoSalariales = !novedadesMes.isEmpty()
                    && novedadesMes.stream()
                    .allMatch(n -> idsNoSalariales.contains(n.getFkConcepNominaId()));

            boolean aplicaSalarioBase = sinNovedades || soloNovedadesNoSalariales;

            if (aplicaSalarioBase) {
                int diasDelProceso = detalles.stream()
                        .filter(d -> d.getFkConcepNominaId() != null
                                && d.getFkConcepNominaId().equals(1L))
                        .mapToInt(d -> d.getCantidadConcept() != null
                                ? d.getCantidadConcept().intValue() : 0)
                        .sum();

                BigDecimal salarioHistoricoMes = detalles.stream()
                        .filter(d -> d.getFkConcepNominaId() != null
                                && d.getFkConcepNominaId().equals(1L))
                        .map(com.fenomina.payroll_engine.entity.ReporteNominaDetalle::getBaseCalculoConcept)
                        .filter(v -> v != null)
                        .findFirst()
                        .orElse(empleado.salarioBascMensual());

                boolean tuvoAuxEsteProceso = detalles.stream()
                        .anyMatch(d -> d.getFkConcepNominaId() != null
                                && d.getFkConcepNominaId().equals(22L));

                BigDecimal auxCompleto = (empleadoTieneAux && tuvoAuxEsteProceso)
                        ? auxTransporteCompleto : BigDecimal.ZERO;

                BigDecimal factor = BigDecimal.valueOf(diasDelProceso)
                        .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);

                totalDevengadoAcumulado = totalDevengadoAcumulado
                        .add(salarioHistoricoMes.multiply(factor));

                totalAuxAcumuladoSinNovedades = totalAuxAcumuladoSinNovedades
                        .add(auxCompleto.multiply(factor));

            } else {
                BigDecimal devengadoProceso = nc.getTotalDevengadoEmp() != null
                        ? nc.getTotalDevengadoEmp() : BigDecimal.ZERO;

                BigDecimal noSalarialProceso = detalles.stream()
                        .filter(d -> d.getFkConcepNominaId() != null
                                && idsNoSalariales.contains(d.getFkConcepNominaId()))
                        .map(d -> d.getValorResultConcept() != null
                                ? d.getValorResultConcept() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal auxProceso = detalles.stream()
                        .filter(d -> d.getFkConcepNominaId() != null
                                && d.getFkConcepNominaId().equals(22L))
                        .map(d -> d.getValorResultConcept() != null
                                ? d.getValorResultConcept() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalDevengadoAcumulado = totalDevengadoAcumulado
                        .add(devengadoProceso.subtract(noSalarialProceso).subtract(auxProceso));

                // Verificar si este proceso tuvo novedades de ausencia
                // (únicas que reducen el aux de transporte)
                boolean tuvoAusencia = novedadesMes.stream()
                        .anyMatch(n -> IDS_AUSENCIAS.contains(n.getFkConcepNominaId()));

                if (tuvoAusencia) {
                    // Aux real reducido por ausencia → acumular para promediar entre meses
                    totalAuxAcumuladoConNovedades = totalAuxAcumuladoConNovedades.add(auxProceso);
                } else {
                    // Sin ausencias → aux completo, tratarlo igual que rama sin novedades
                    boolean tuvoAuxEsteProceso = detalles.stream()
                            .anyMatch(d -> d.getFkConcepNominaId() != null
                                    && d.getFkConcepNominaId().equals(22L));
                    BigDecimal auxCompleto = (empleadoTieneAux && tuvoAuxEsteProceso)
                            ? auxTransporteCompleto : BigDecimal.ZERO;
                    int diasDelProceso = detalles.stream()
                            .filter(d -> d.getFkConcepNominaId() != null
                                    && d.getFkConcepNominaId().equals(1L))
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept().intValue() : 0)
                            .sum();
                    BigDecimal factor = BigDecimal.valueOf(diasDelProceso)
                            .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
                    totalAuxAcumuladoSinNovedades = totalAuxAcumuladoSinNovedades
                            .add(auxCompleto.multiply(factor));
                }
            }
        }

        if (diasLiquidados <= 0) {
            return new BigDecimal[]{empleado.salarioBascMensual(), BigDecimal.ZERO};
        }

        BigDecimal promedioMensualSalario = totalDevengadoAcumulado
                .divide(BigDecimal.valueOf(diasLiquidados), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(30))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        int totalMeses = todosMesesConNomina.isEmpty() ? 1 : todosMesesConNomina.size();

        BigDecimal promedioAuxSinNovedades = totalAuxAcumuladoSinNovedades
                .divide(BigDecimal.valueOf(diasLiquidados), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(30))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal promedioAuxConNovedades = totalAuxAcumuladoConNovedades
                .divide(BigDecimal.valueOf(totalMeses), ESCALA, RoundingMode.HALF_UP);

        BigDecimal promedioMensualAux = promedioAuxSinNovedades.add(promedioAuxConNovedades);

        return new BigDecimal[]{
                promedioMensualSalario.add(promedioMensualAux),
                promedioMensualAux
        };
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