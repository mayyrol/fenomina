package com.fenomina.payroll_engine.service.liquidacion;

import com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion;
import com.fenomina.payroll_engine.entity.DetalleLiquiPrestacion;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.exception.CalculoNominaException;
import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.repository.CabeceraLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.DetalleLiquiPrestacionRepository;
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
public class LiquidacionInteresesServiceImpl implements LiquidacionInteresesService {

    private static final int ESCALA = 2;
    private static final int DIAS_ANIO = 360;
    private static final BigDecimal TASA_INTERESES = new BigDecimal("0.12");

    private final MasterDataClientWrapper masterDataClient;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    @Override
    @Transactional
    public void liquidar(ProcesoLiquidacion proceso, List<Long> empleadosSeleccionados) {
        log.info("Iniciando liquidación intereses cesantías - proceso: {}",
                proceso.getProcesoLiquiId());

        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();

        Map<String, ConceptoNominaDTO> conceptosPorNombre = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::nombreConcepNomina, c -> c));

        List<EmpleadoDTO> empleadosActivos = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleadosActivos.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        LocalDate fechaInicio = proceso.getFechaInicioPeriodo();
        LocalDate fechaFin = proceso.getFechaFinPeriodo();

        // Buscar el proceso de cesantías del mismo año para leer las cesantías acumuladas
        List<DetalleLiquiPrestacion> cesantiasAnio = detalleLiquiPrestacionRepository
                .findHistoricoByEmpleadoAndAnio(
                        empleadosSeleccionados.get(0),
                        proceso.getAnio()
                );

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
                        conceptosPorNombre
                );
            } catch (Exception e) {
                log.error("Error liquidando intereses empleado {}: {}",
                        empleadoId, e.getMessage());
                throw new CalculoNominaException(
                        String.format("Error al liquidar intereses empleado %d: %s",
                                empleadoId, e.getMessage())
                );
            }
        }

        log.info("Liquidación intereses completada - proceso: {}",
                proceso.getProcesoLiquiId());
    }

    private void procesarEmpleado(
            ProcesoLiquidacion proceso,
            Long empleadoId,
            Map<Long, EmpleadoDTO> empleadosPorId,
            CabeceraLiquiPrestacion cabecera,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo,
            Map<String, ConceptoNominaDTO> conceptosPorNombre
    ) {
        EmpleadoDTO empleado = empleadosPorId.get(empleadoId);
        if (empleado == null) {
            throw new CalculoNominaException(
                    String.format("Empleado %d no encontrado o no activo", empleadoId)
            );
        }

        if (Boolean.TRUE.equals(empleado.esSalarioIntegral())) {
            log.debug("Empleado {} tiene salario integral, se omiten intereses ordinarios",
                    empleadoId);
            return;
        }

        // Leer cesantías acumuladas del proceso de cesantías del mismo año
        List<DetalleLiquiPrestacion> detallesCesantias = detalleLiquiPrestacionRepository
                .findHistoricoByEmpleadoAndAnio(empleadoId, proceso.getAnio());

        BigDecimal cesantiasAcumuladas = detallesCesantias.stream()
                .filter(d -> {
                    ConceptoNominaDTO c = conceptosPorNombre.get("Cesantías");
                    return c != null && c.concepNominaId().equals(d.getFkConcepNominaId());
                })
                .map(DetalleLiquiPrestacion::getValorNetaPresta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (cesantiasAcumuladas.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Empleado {} sin cesantías acumuladas, se omiten intereses", empleadoId);
            return;
        }

        LocalDate fechaIngreso = empleado.fechaIngresoEmp();
        LocalDate fechaInicioReal = fechaIngreso.isAfter(fechaInicioPeriodo)
                ? fechaIngreso
                : fechaInicioPeriodo;

        int diasLiquidados = LiquidacionFechaUtils.calcularDias(fechaInicioReal, fechaFinPeriodo);
        diasLiquidados = Math.min(diasLiquidados, DIAS_ANIO);

        // Fórmula: (cesantíasAcumuladas * diasLiquidados * 0.12) / 360
        BigDecimal valorIntereses = cesantiasAcumuladas
                .multiply(BigDecimal.valueOf(diasLiquidados))
                .multiply(TASA_INTERESES)
                .divide(BigDecimal.valueOf(DIAS_ANIO), ESCALA, RoundingMode.HALF_UP);

        ConceptoNominaDTO conceptoIntereses = conceptosPorNombre
                .get("Intereses sobre las cesantías");

        DetalleLiquiPrestacion detalle = DetalleLiquiPrestacion.builder()
                .fkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                .fkEmpleadoId(empleadoId)
                .fkConcepNominaId(conceptoIntereses != null
                        ? conceptoIntereses.concepNominaId() : null)
                .fechaInicioCorteEmp(fechaInicioReal)
                .fechaFinCorteEmp(fechaFinPeriodo)
                .diasLiquidadosInt(diasLiquidados)
                .baseLiquiTotal(cesantiasAcumuladas)
                .valorNetaPresta(BigDecimal.ZERO)
                .valorIntCesantias(valorIntereses)
                .build();

        detalleLiquiPrestacionRepository.save(detalle);

        log.debug("Intereses cesantías empleado {}: días={}, cesantías={}, intereses={}",
                empleadoId, diasLiquidados, cesantiasAcumuladas, valorIntereses);
    }


}
