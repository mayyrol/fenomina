package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.client.dto.ParametroGeneralDTO;
import com.fenomina.payroll_engine.dto.response.DesprendiblePrestacionResponseDTO;
import com.fenomina.payroll_engine.dto.response.DesprendibleResponseDTO;
import com.fenomina.payroll_engine.entity.*;
import com.fenomina.payroll_engine.repository.CabeceraLiquiPrestacionRepository;
import com.fenomina.payroll_engine.repository.DetalleLiquiPrestacionRepository;
import com.fenomina.payroll_engine.service.proceso.ProcesoLiquidacionService;
import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.repository.NominaCabeceraRepository;
import com.fenomina.payroll_engine.repository.ReporteNominaDetalleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll/desprendibles")
@RequiredArgsConstructor
@Slf4j
public class DesprendibleController {

    private final ProcesoLiquidacionService procesoService;
    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final MasterDataClientWrapper masterDataClient;
    private final CabeceraLiquiPrestacionRepository cabeceraLiquiPrestacionRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    @GetMapping("/nomina/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<DesprendibleResponseDTO>> getDesprendiblesNomina(
            @PathVariable("procesoId") Long procesoId
    ) {
        log.debug("Generando desprendibles nómina - proceso: {}", procesoId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);

        List<EmpleadoDTO> empleados = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleados.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        List<ConceptoNominaDTO> conceptos = masterDataClient.findAllConceptosNomina();

        Map<Long, ConceptoNominaDTO> conceptosPorId = conceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::concepNominaId, c -> c));

        List<NominaCabecera> cabeceras = nominaCabeceraRepository
                .findByFkProcesoLiquiId(procesoId);

        List<DesprendibleResponseDTO> desprendibles = cabeceras.stream()
                .map(cabecera -> {
                    EmpleadoDTO empleado = empleadosPorId.get(cabecera.getFkEmpleadoId());

                    List<ReporteNominaDetalle> detalles = reporteNominaDetalleRepository
                            .findByFkCabecNominaId(cabecera.getCabecNominaId());

                    return DesprendibleResponseDTO.builder()
                            .cabecNominaId(cabecera.getCabecNominaId())
                            .empleadoId(cabecera.getFkEmpleadoId())
                            .nombresEmpleado(empleado != null
                                    ? empleado.nombresEmp() : "N/A")
                            .apellidosEmpleado(empleado != null
                                    ? empleado.apellidosEmp() : "N/A")
                            .documentoEmpleado(empleado != null
                                    ? empleado.documentoEmp() : "N/A")
                            .salarioBasico(empleado != null
                                    ? empleado.salarioBascMensual() : null)
                            .anio(cabecera.getAnioCabecNomina())
                            .periodo(cabecera.getPeriodoCotiNomina())
                            .totalDevengado(cabecera.getTotalDevengadoEmp())
                            .totalDeducciones(cabecera.getTotalDeduccionEmp())
                            .netoAPagar(cabecera.getNetoNominaEmp())
                            .conceptos(detalles.stream()
                                    .map(d -> {
                                        ConceptoNominaDTO concepto = conceptosPorId
                                                .get(d.getFkConcepNominaId());
                                        return DesprendibleResponseDTO.ConceptoDetalleDTO
                                                .builder()
                                                .concepNominaId(d.getFkConcepNominaId())
                                                .nombreConcepto(concepto != null
                                                        ? concepto.nombreConcepNomina()
                                                        : "N/A")
                                                .categoria(concepto != null
                                                        ? concepto.categoriaConcNomina()
                                                        : "N/A")
                                                .cantidad(d.getCantidadConcept())
                                                .baseCalculo(d.getBaseCalculoConcept())
                                                .valorResultado(d.getValorResultConcept())
                                                .build();
                                    })
                                    .toList())
                            .build();
                })
                .toList();

        return ResponseEntity.ok(desprendibles);
    }

    @GetMapping("/prima/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<DesprendiblePrestacionResponseDTO>> getDesprendiblesPrima(
            @PathVariable("procesoId") Long procesoId
    ) {
        log.debug("Generando desprendibles prima - proceso: {}", procesoId);

        return ResponseEntity.ok(
                buildDesprendiblesPrestacion(procesoId, "PRIMA_SEMESTRAL")
        );
    }

    @GetMapping("/cesantias/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<DesprendiblePrestacionResponseDTO>> getDesprendiblesCesantias(
            @PathVariable("procesoId") Long procesoId
    ) {
        log.debug("Generando desprendibles cesantías - proceso: {}", procesoId);

        return ResponseEntity.ok(
                buildDesprendiblesPrestacion(procesoId, "CESANTIAS_ANUAL")
        );
    }

    @GetMapping("/intereses-cesantias/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<DesprendiblePrestacionResponseDTO>> getDesprendiblesIntereses(
            @PathVariable("procesoId") Long procesoId
    ) {
        log.debug("Generando desprendibles intereses cesantías - proceso: {}", procesoId);

        return ResponseEntity.ok(
                buildDesprendiblesPrestacion(procesoId, "INTERESES_CESANTIAS_ANUAL")
        );
    }

    private List<DesprendiblePrestacionResponseDTO> buildDesprendiblesPrestacion(
            Long procesoId,
            String tipoPrestacion
    ) {
        ProcesoLiquidacion proceso = procesoService.findById(procesoId);

        List<EmpleadoDTO> empleados = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleados.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        CabeceraLiquiPrestacion cabecera = cabeceraLiquiPrestacionRepository
                .findByFkProcesoLiquiId(procesoId)
                .orElseThrow(() -> new com.fenomina.payroll_engine.exception
                        .ProcesoLiquidacionNotFoundException(
                        String.format("No existe cabecera de prestación para proceso %d",
                                procesoId)
                ));

        List<DetalleLiquiPrestacion> detalles = detalleLiquiPrestacionRepository
                .findByFkCabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId());

        List<ParametroGeneralDTO> parametros = masterDataClient.findAllParametros();

        BigDecimal auxTransporte = parametros.stream()
                .filter(p -> "AUXILIO_TRANSPORTE".equals(p.nombreParamGeneral()))
                .filter(p -> !p.fechaParamGeneral().isAfter(proceso.getFechaFinPeriodo()))
                .max(java.util.Comparator.comparing(ParametroGeneralDTO::fechaParamGeneral))
                .map(ParametroGeneralDTO::valorParamGeneral)
                .orElse(java.math.BigDecimal.ZERO);

        return detalles.stream()
                .map(detalle -> {
                    EmpleadoDTO empleado = empleadosPorId.get(detalle.getFkEmpleadoId());
                    BigDecimal auxTransporteEmpleado = empleado != null
                            && Boolean.TRUE.equals(empleado.tieneAuxTransporte())
                            ? auxTransporte
                            : java.math.BigDecimal.ZERO;

                    return DesprendiblePrestacionResponseDTO.builder()
                            .cabeLiquiPrestacionId(cabecera.getCabeLiquiPrestacionId())
                            .empleadoId(detalle.getFkEmpleadoId())
                            .nombresEmpleado(empleado != null
                                    ? empleado.nombresEmp() : "N/A")
                            .apellidosEmpleado(empleado != null
                                    ? empleado.apellidosEmp() : "N/A")
                            .documentoEmpleado(empleado != null
                                    ? empleado.documentoEmp() : "N/A")
                            .fondoPension(empleado != null
                                    ? empleado.fondoPensionEmp() : "N/A")
                            .anio(cabecera.getAnioLiquiPrestacion())
                            .periodo(cabecera.getPeriodoLiquiPrestacion())
                            .fechaInicioCorte(detalle.getFechaInicioCorteEmp())
                            .fechaFinCorte(detalle.getFechaFinCorteEmp())
                            .diasLiquidados(detalle.getDiasLiquidadosInt())
                            .salarioBase(detalle.getSalarioFijoMomento())
                            .auxTransporte(auxTransporteEmpleado)
                            .baseLiquidacion(detalle.getBaseLiquiTotal())
                            .valorPrestacion(detalle.getValorNetaPresta())
                            .valorInteresesCesantias(detalle.getValorIntCesantias())
                            .tipoPrestacion(tipoPrestacion)
                            .build();
                })
                .toList();
    }

    @GetMapping("/prima/preview/{empresaId}/empleado/{empleadoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, Object>> getPreviewPrimaEmpleado(
            @PathVariable("empresaId") Long empresaId,
            @PathVariable("empleadoId") Long empleadoId,
            @RequestParam("semestre") Integer semestre,
            @RequestParam("anio") Integer anio
    ) {
        log.debug("Preview prima - empresa: {}, empleado: {}, semestre: {}, anio: {}",
                empresaId, empleadoId, semestre, anio);

        LocalDate fechaInicio = semestre == 1
                ? LocalDate.of(anio, 1, 1)
                : LocalDate.of(anio, 7, 1);
        LocalDate fechaFin = semestre == 1
                ? LocalDate.of(anio, 6, 30)
                : LocalDate.of(anio, 12, 31);

        // Cabeceras de nóminas pagadas del empleado en el semestre
        List<NominaCabecera> cabeceras = nominaCabeceraRepository
                .findNominasDelSemestre(empleadoId, empresaId, fechaInicio, fechaFin);

        // Sumar días laborados (concepto ID=1)
        int diasLaborados = cabeceras.stream()
                .mapToInt(cabecera -> {
                    List<ReporteNominaDetalle> detalles = reporteNominaDetalleRepository
                            .findByFkCabecNominaId(cabecera.getCabecNominaId());
                    return detalles.stream()
                            .filter(d -> d.getFkConcepNominaId() == 1L)
                            .mapToInt(d -> d.getCantidadConcept() != null
                                    ? d.getCantidadConcept() : 0)
                            .sum();
                })
                .sum();

        // Conceptos variables del semestre (excluir salario base, deducciones y aportes patronales)
        List<Long> CONCEPTOS_EXCLUIDOS = List.of(1L, 16L, 22L, 23L, 32L, 33L, 34L, 35L, 36L,
                37L, 38L, 39L, 40L, 41L, 42L, 43L, 44L, 45L, 46L, 47L);

        List<ConceptoNominaDTO> todosConceptos = masterDataClient.findAllConceptosNomina();
        Map<Long, ConceptoNominaDTO> conceptosPorId = todosConceptos.stream()
                .collect(Collectors.toMap(ConceptoNominaDTO::concepNominaId, c -> c));

        List<Map<String, Object>> novedades = cabeceras.stream()
                .flatMap(cabecera -> {
                    List<ReporteNominaDetalle> detalles = reporteNominaDetalleRepository
                            .findByFkCabecNominaId(cabecera.getCabecNominaId());
                    return detalles.stream()
                            .filter(d -> !CONCEPTOS_EXCLUIDOS.contains(d.getFkConcepNominaId()))
                            .map(d -> {
                                ConceptoNominaDTO concepto = conceptosPorId
                                        .get(d.getFkConcepNominaId());
                                return Map.<String, Object>of(
                                        "nombreConcepto", concepto != null
                                                ? concepto.nombreConcepNomina() : "N/A",
                                        "valorResultado", d.getValorResultConcept() != null
                                                ? d.getValorResultConcept() : BigDecimal.ZERO,
                                        "cantidad", d.getCantidadConcept() != null
                                                ? d.getCantidadConcept() : 0,
                                        "periodo", cabecera.getPeriodoCotiNomina(),
                                        "anio", cabecera.getAnioCabecNomina()
                                );
                            });
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "diasLaborados", diasLaborados,
                "novedades", novedades,
                "fechaInicio", fechaInicio.toString(),
                "fechaFin", fechaFin.toString()
        ));
    }
}
