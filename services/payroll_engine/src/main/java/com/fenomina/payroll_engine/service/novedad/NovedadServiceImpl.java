package com.fenomina.payroll_engine.service.novedad;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.exception.NovedadNotFoundException;
import com.fenomina.payroll_engine.exception.ProcesoYaCerradoException;
import com.fenomina.payroll_engine.exception.ValidacionNominaException;
import com.fenomina.payroll_engine.repository.NovedadRepository;
import com.fenomina.payroll_engine.repository.ProcesoLiquidacionRepository;
import com.fenomina.payroll_engine.exception.ProcesoLiquidacionNotFoundException;
import com.fenomina.payroll_engine.validator.NovedadValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovedadServiceImpl implements NovedadService {

    private final NovedadRepository novedadRepository;
    private final ProcesoLiquidacionRepository procesoRepository;
    private final MasterDataClientWrapper masterDataClient;
    private final NovedadValidator novedadValidator;

    private static final List<String> CONCEPTOS_PERMITE_DUPLICADO = List.of(
            "Recargo nocturno ordinario",
            "Recargo diurno dominical o festivo",
            "Recargo nocturno dominical o festivo",
            "Hora extra diurna ordinaria",
            "Hora extra nocturna ordinaria",
            "Hora extra diurna dominical o festiva",
            "Hora extra nocturna dominical o festiva",
            "Otro concepto a devenir salarial",
            "Otro concepto a devenir no salarial",
            "Otros conceptos a deducir salariales",
            "Otros conceptos a deducir no salariales"
    );

    @Override
    @Transactional
    public Novedad crear(Novedad novedad) {
        log.info("Registrando novedad para empleado {} en proceso {}",
                novedad.getFkEmpleadoId(), novedad.getProcesoLiquid());

        ProcesoLiquidacion proceso = obtenerProcesoEnBorrador(novedad.getProcesoLiquid());

        ConceptoNominaDTO concepto = masterDataClient.findAllConceptosNomina()
                .stream()
                .filter(c -> c.concepNominaId().equals(novedad.getFkConcepNominaId()))
                .findFirst()
                .orElseThrow(() -> new ValidacionNominaException(
                        "fkConcepNominaId",
                        String.format("Concepto de nómina no encontrado: %d",
                                novedad.getFkConcepNominaId())
                ));
        completarDiasDesdeFechas(novedad);

        novedadValidator.validarNovedad(
                novedad,
                concepto,
                proceso.getFechaInicioPeriodo(),
                proceso.getFechaFinPeriodo(),
                proceso.getTipoProceso() ==
                        com.fenomina.payroll_engine.enums.TipoProceso.NOMINA_QUINCENAL
        );

        validarNovedadDuplicada(novedad, concepto.nombreConcepNomina());

        return novedadRepository.save(novedad);
    }

    @Override
    @Transactional
    public Novedad actualizar(Long novedadId, Novedad novedadActualizada) {
        log.info("Actualizando novedad {}", novedadId);

        Novedad existente = findById(novedadId);
        ProcesoLiquidacion proceso = obtenerProcesoEnBorrador(existente.getProcesoLiquid());

        ConceptoNominaDTO concepto = masterDataClient.findAllConceptosNomina()
                .stream()
                .filter(c -> c.concepNominaId().equals(novedadActualizada.getFkConcepNominaId()))
                .findFirst()
                .orElseThrow(() -> new ValidacionNominaException(
                        "fkConcepNominaId",
                        String.format("Concepto de nómina no encontrado: %d",
                                novedadActualizada.getFkConcepNominaId())
                ));

        completarDiasDesdeFechas(novedadActualizada);

        novedadValidator.validarNovedad(
                novedadActualizada,
                concepto,
                proceso.getFechaInicioPeriodo(),
                proceso.getFechaFinPeriodo(),
                proceso.getTipoProceso() ==
                        com.fenomina.payroll_engine.enums.TipoProceso.NOMINA_QUINCENAL
        );

        existente.setFkConcepNominaId(novedadActualizada.getFkConcepNominaId());
        existente.setFechaNovedad(novedadActualizada.getFechaNovedad());
        existente.setFechaInicioAusen(novedadActualizada.getFechaInicioAusen());
        existente.setFechaFinAusen(novedadActualizada.getFechaFinAusen());
        existente.setCantidadDiasNovedad(novedadActualizada.getCantidadDiasNovedad());
        existente.setCantidadHorasNovedad(novedadActualizada.getCantidadHorasNovedad());
        existente.setValorRefNovedad(novedadActualizada.getValorRefNovedad());
        existente.setObservaciones(novedadActualizada.getObservaciones());
        existente.setUpdatedBy(novedadActualizada.getUpdatedBy());

        return novedadRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long novedadId) {
        log.info("Eliminando novedad {}", novedadId);

        Novedad existente = findById(novedadId);
        obtenerProcesoEnBorrador(existente.getProcesoLiquid());

        novedadRepository.delete(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public Novedad findById(Long novedadId) {
        return novedadRepository.findById(novedadId)
                .orElseThrow(() -> new NovedadNotFoundException(
                        String.format("Novedad no encontrada: %d", novedadId)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novedad> findByProceso(Long procesoId) {
        return novedadRepository.findByProcesoLiquid(procesoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novedad> findByEmpleadoYProceso(Long empleadoId, Long procesoId) {
        return novedadRepository.findByFkEmpleadoIdAndProcesoLiquid(empleadoId, procesoId);
    }

    // --- Validaciones internas ---
    private ProcesoLiquidacion obtenerProcesoEnBorrador(Long procesoId) {
        if (procesoId == null) {
            throw new ValidacionNominaException(
                    "procesoId",
                    "La novedad debe estar asociada a un proceso de liquidación"
            );
        }

        ProcesoLiquidacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ProcesoLiquidacionNotFoundException(
                        String.format("Proceso de liquidación no encontrado: %d", procesoId)
                ));

        if (proceso.getEstadoProcNomina() != EstadoProceso.BORRADOR) {
            throw new ProcesoYaCerradoException(
                    String.format(
                            "No se pueden modificar novedades de un proceso en estado %s. " +
                                    "El proceso debe estar en BORRADOR",
                            proceso.getEstadoProcNomina()
                    )
            );
        }

        return proceso;
    }

    private void completarDiasDesdeFechas(Novedad novedad) {
        if (novedad.getCantidadDiasNovedad() != null
                && novedad.getCantidadDiasNovedad() > 0) {
            return; // Ya viene calculado, no sobrescribir
        }

        if (novedad.getFechaInicioAusen() != null
                && novedad.getFechaFinAusen() != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(
                    novedad.getFechaInicioAusen(),
                    novedad.getFechaFinAusen()
            ) + 1;
            if (dias > 30) {
                dias = 30;
            }

            novedad.setCantidadDiasNovedad((int) dias);
        }
    }

    private void validarNovedadDuplicada(Novedad novedad, String nombreConcepto) {
        if (CONCEPTOS_PERMITE_DUPLICADO.contains(nombreConcepto)) {
            return;
        }

        boolean existe = novedadRepository.existsNovedadActivaByEmpleadoAndConceptoAndPeriodo(
                novedad.getFkEmpleadoId(),
                novedad.getFkConcepNominaId(),
                novedad.getAnio(),
                novedad.getPeriodo()
        );

        if (existe) {
            throw new ValidacionNominaException(
                    "fkConcepNominaId",
                    String.format(
                            "Ya existe una novedad del mismo concepto para el empleado %d " +
                                    "en el periodo %d/%d",
                            novedad.getFkEmpleadoId(),
                            novedad.getPeriodo(),
                            novedad.getAnio()
                    )
            );
        }
    }
}
