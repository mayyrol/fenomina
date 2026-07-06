package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.exception.ProcesoLiquidacionNotFoundException;
import com.fenomina.payroll_engine.exception.ProcesoYaCerradoException;
import com.fenomina.payroll_engine.exception.ValidacionNominaException;
import com.fenomina.payroll_engine.client.dto.ContratoConceptoDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.repository.*;
import com.fenomina.payroll_engine.validator.NovedadValidator;
import com.fenomina.payroll_engine.validator.ProcesoLiquidacionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcesoLiquidacionServiceImpl implements ProcesoLiquidacionService {

    private static final List<EstadoProceso> ESTADOS_ACTIVOS = List.of(
            EstadoProceso.BORRADOR,
            EstadoProceso.CERRADO,
            EstadoProceso.PENDIENTE_PAGO,
            EstadoProceso.PAGADO
    );

    private static final String ESTADO_ACTIVO_EMPLEADO = "ACTIVO";
    private static final BigDecimal LIMITE_NO_SALARIAL = new BigDecimal("0.40");

    private final ProcesoLiquidacionRepository procesoRepository;
    private final NovedadRepository novedadRepository;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final EstadoProcesoValidator estadoValidator;
    private final MasterDataClientWrapper masterDataClient;
    private final ProcesoLiquidacionValidator procesoValidator;
    private final NovedadValidator novedadValidator;

    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;


    @Override
    @Transactional
    public ProcesoLiquidacion crear(
            Long empresaId,
            Long usuarioId,
            TipoProceso tipoProceso,
            Integer anio,
            Integer periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {

        // Detectar automáticamente si es quincenal o mensual según días del periodo
        if (tipoProceso == TipoProceso.NOMINA_MENSUAL ||
                tipoProceso == TipoProceso.NOMINA_QUINCENAL) {
            long diasPeriodo = java.time.temporal.ChronoUnit.DAYS
                    .between(fechaInicio, fechaFin) + 1;
            tipoProceso = diasPeriodo <= 16
                    ? TipoProceso.NOMINA_QUINCENAL
                    : TipoProceso.NOMINA_MENSUAL;
        }

        log.info("Creando proceso de liquidación - empresa: {}, tipo: {}, año: {}, periodo: {}",
                empresaId, tipoProceso, anio, periodo);

        procesoValidator.validarCreacion(
                tipoProceso, anio, periodo, fechaInicio, fechaFin
        );

        boolean existeActivo;
        if (tipoProceso == TipoProceso.NOMINA_QUINCENAL) {
            existeActivo = procesoRepository.existsProcesoActivoConFecha(
                    empresaId, tipoProceso, anio, periodo, fechaInicio, ESTADOS_ACTIVOS
            );
        } else {
            existeActivo = procesoRepository.existsProcesoActivo(
                    empresaId, tipoProceso, anio, periodo, ESTADOS_ACTIVOS
            );
        }

        if (existeActivo) {
            throw new ProcesoYaCerradoException(
                    String.format(
                            "Ya existe un proceso activo para empresa %d, tipo %s, " +
                                    "año %d, periodo %d",
                            empresaId, tipoProceso, anio, periodo)
            );
        }

        ProcesoLiquidacion proceso = new ProcesoLiquidacion();
        proceso.setFkIdEmpresa(empresaId);
        proceso.setFkUsuarioId(usuarioId);
        proceso.setTipoProceso(tipoProceso);
        proceso.setAnio(anio);
        proceso.setPeriodo(periodo);
        proceso.setFechaInicioPeriodo(fechaInicio);
        proceso.setFechaFinPeriodo(fechaFin);
        proceso.setCreatedBy(usuarioId);

        return procesoRepository.save(proceso);
    }

    @Override
    @Transactional
    public ResultadoCambioEstado cambiarEstado(
            Long procesoId, EstadoProceso nuevoEstado, Long usuarioId) {

        ProcesoLiquidacion proceso = findById(procesoId);
        estadoValidator.validarTransicion(proceso.getEstadoProcNomina(), nuevoEstado);

        if (nuevoEstado == EstadoProceso.ANULADO &&
                proceso.getTipoProceso() == TipoProceso.CESANTIAS_ANUAL) {
            procesoRepository
                    .findByEmpresaAndTipoAndAnio(
                            proceso.getFkIdEmpresa(),
                            TipoProceso.INTERESES_CESANTIAS_ANUAL,
                            proceso.getAnio()
                    )
                    .stream()
                    .findFirst()
                    .ifPresent(procesoIntereses -> {
                        estadoValidator.validarTransicion(
                                procesoIntereses.getEstadoProcNomina(),
                                EstadoProceso.ANULADO
                        );
                        procesoIntereses.setEstadoProcNomina(EstadoProceso.ANULADO);
                        procesoIntereses.setUpdatedBy(usuarioId);
                        procesoRepository.save(procesoIntereses);
                        log.info("Proceso de intereses {} anulado junto con cesantías",
                                procesoIntereses.getProcesoLiquiId());
                    });
        }

        List<String> advertencias = new ArrayList<>();
        if (nuevoEstado == EstadoProceso.CERRADO) {
            advertencias = validarCierre(proceso);
        }

        proceso.setEstadoProcNomina(nuevoEstado);
        proceso.setUpdatedBy(usuarioId);

        if (nuevoEstado == EstadoProceso.CERRADO) {
            nominaCabeceraRepository.findByFkProcesoLiquiId(procesoId)
                    .forEach(cabecera -> {
                        cabecera.setFechaCierreNomina(java.time.LocalDateTime.now());
                        nominaCabeceraRepository.save(cabecera);
                    });
        }

        return new ResultadoCambioEstado(procesoRepository.save(proceso), advertencias);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcesoLiquidacion findById(Long procesoId) {
        return procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ProcesoLiquidacionNotFoundException(
                        String.format("Proceso de liquidación no encontrado: %d", procesoId)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcesoLiquidacion> findByEmpresa(Long empresaId) {
        return procesoRepository.findByFkIdEmpresaOrderByAnioDescPeriodoDesc(empresaId);
    }

    @Override
    @Transactional
    public void eliminar(Long procesoId) {
        ProcesoLiquidacion proceso = findById(procesoId);

        if (proceso.getEstadoProcNomina() != EstadoProceso.BORRADOR) {
            throw new ProcesoYaCerradoException(
                    "Solo se pueden eliminar procesos en estado BORRADOR"
            );
        }

        novedadRepository.findByProcesoLiquid(procesoId)
                .forEach(novedadRepository::delete);

        cabeceraLiquiPrestacionRepository
                .findByFkProcesoLiquiId(procesoId)
                .ifPresent(cabecera -> {
                    detalleLiquiPrestacionRepository
                            .findByFkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                            .forEach(detalleLiquiPrestacionRepository::delete);
                    cabeceraLiquiPrestacionRepository.delete(cabecera);
                });
        // Si es cesantías, eliminar también el proceso de intereses del mismo año
        if (proceso.getTipoProceso() == TipoProceso.CESANTIAS_ANUAL) {
            procesoRepository
                    .findByEmpresaAndTipoAndAnio(
                            proceso.getFkIdEmpresa(),
                            TipoProceso.INTERESES_CESANTIAS_ANUAL,
                            proceso.getAnio()
                    )
                    .stream()
                    .findFirst()
                    .ifPresent(procesoIntereses -> {
                        novedadRepository.findByProcesoLiquid(procesoIntereses.getProcesoLiquiId())
                                .forEach(novedadRepository::delete);

                        cabeceraLiquiPrestacionRepository
                                .findByFkProcesoLiquiId(procesoIntereses.getProcesoLiquiId())
                                .ifPresent(cab -> {
                                    detalleLiquiPrestacionRepository
                                            .findByFkCabeLiquiPrestacionId(cab.getCabeLiquiPrestacionId())
                                            .forEach(detalleLiquiPrestacionRepository::delete);
                                    cabeceraLiquiPrestacionRepository.delete(cab);
                                });

                        procesoRepository.delete(procesoIntereses);
                        log.info("Proceso de intereses {} eliminado junto con cesantías",
                                procesoIntereses.getProcesoLiquiId());
                    });
        }

        procesoRepository.delete(proceso);

        log.info("Proceso {} eliminado junto con todos sus registros asociados", procesoId);
    }

    // --- Validaciones internas ---

    private List<String> validarCierre(ProcesoLiquidacion proceso) {
        List<String> advertencias = new ArrayList<>();

        List<EmpleadoDTO> empleados = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        List<Novedad> novedadesProceso = novedadRepository
                .findByProcesoLiquid(proceso.getProcesoLiquiId());

        procesoValidator.validarNovedadesFueraDePeriodo(
                novedadesProceso,
                proceso.getFechaInicioPeriodo(),
                proceso.getFechaFinPeriodo()
        );

        Map<Long, ConceptoNominaDTO> conceptosPorId = masterDataClient
                .findAllConceptosNomina()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ConceptoNominaDTO::concepNominaId, c -> c
                ));

        for (EmpleadoDTO empleado : empleados) {
            List<Novedad> novedadesEmpleado = novedadesProceso.stream()
                    .filter(n -> n.getFkEmpleadoId().equals(empleado.empleadoId()))
                    .toList();

            String advertencia = verificarExcesoNoSalarial(
                    empleado, novedadesEmpleado, conceptosPorId);
            if (advertencia != null) {
                advertencias.add(advertencia);
            }
        }

        return advertencias;
    }

    private String verificarExcesoNoSalarial(
            EmpleadoDTO empleado,
            List<Novedad> novedades,
            Map<Long, ConceptoNominaDTO> conceptosPorId
    ) {
        List<ContratoConceptoDTO> conceptosFijos = masterDataClient
                .findConceptosFijosByEmpleado(empleado.empleadoId());

        BigDecimal totalNoSalarialNovedades = novedades.stream()
                .filter(n -> n.getValorRefNovedad() != null)
                .filter(n -> {
                    ConceptoNominaDTO concepto = conceptosPorId.get(n.getFkConcepNominaId());
                    return concepto != null
                            && Boolean.FALSE.equals(concepto.esSalario())
                            && !"Bonificaciones ocasionales o por mera liberalidad"
                            .equals(concepto.nombreConcepNomina());
                })
                .map(Novedad::getValorRefNovedad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNoSalarialFijos = conceptosFijos.stream()
                .filter(c -> Boolean.FALSE.equals(c.esSalario()) && c.valorFijo() != null)
                .map(ContratoConceptoDTO::valorFijo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNoSalarial = totalNoSalarialNovedades.add(totalNoSalarialFijos);
        BigDecimal exceso = novedadValidator.calcularExcesoNoSalarial(
                totalNoSalarial, empleado.salarioBascMensual());

        if (exceso.compareTo(BigDecimal.ZERO) > 0) {
            return String.format(
                    "Empleado %s %s: los pagos no salariales ($%s) superan el límite del 40%%. " +
                            "El exceso de $%s se incorporará automáticamente al IBC.",
                    empleado.nombresEmp(),
                    empleado.apellidosEmp(),
                    totalNoSalarial.setScale(0, java.math.RoundingMode.HALF_UP),
                    exceso.setScale(0, java.math.RoundingMode.HALF_UP)
            );
        }
        return null;
    }
}