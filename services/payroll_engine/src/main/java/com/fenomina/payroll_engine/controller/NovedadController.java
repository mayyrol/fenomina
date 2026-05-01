package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.dto.request.NovedadRequestDTO;
import com.fenomina.payroll_engine.dto.response.NovedadResponseDTO;
import com.fenomina.payroll_engine.mapper.NovedadMapper;
import com.fenomina.payroll_engine.service.novedad.NovedadService;
import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/novedades")
@RequiredArgsConstructor
@Slf4j
public class NovedadController {

    private final NovedadService novedadService;
    private final NovedadMapper mapper;
    private final MasterDataClientWrapper masterDataClient;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<NovedadResponseDTO> crear(
            @Valid @RequestBody NovedadRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Registrando novedad - empleado: {}, proceso: {}",
                request.fkEmpleadoId(), request.procesoLiquid());

        Novedad novedad = mapper.toEntity(request, usuarioId);
        Novedad creada = novedadService.crear(novedad);

        List<ConceptoNominaDTO> conceptos = masterDataClient.findAllConceptosNomina();
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(creada, conceptos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<NovedadResponseDTO> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody NovedadRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Actualizando novedad: {}", id);

        Novedad novedadActualizada = mapper.toEntity(request, usuarioId);
        Novedad actualizada = novedadService.actualizar(id, novedadActualizada);

        List<ConceptoNominaDTO> conceptos = masterDataClient.findAllConceptosNomina();
        return ResponseEntity.ok(mapper.toResponse(actualizada, conceptos));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        log.info("Eliminando novedad: {}", id);
        novedadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proceso/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<NovedadResponseDTO>> findByProceso(
            @PathVariable("procesoId") Long procesoId
    ) {
        log.debug("Consultando novedades del proceso: {}", procesoId);
        List<ConceptoNominaDTO> conceptos = masterDataClient.findAllConceptosNomina();
        List<NovedadResponseDTO> novedades = novedadService.findByProceso(procesoId)
                .stream()
                .map(n -> mapper.toResponse(n, conceptos))
                .toList();
        return ResponseEntity.ok(novedades);
    }

    @GetMapping("/proceso/{procesoId}/empleado/{empleadoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<NovedadResponseDTO>> findByEmpleadoYProceso(
            @PathVariable("procesoId") Long procesoId,
            @PathVariable("empleadoId") Long empleadoId
    ) {
        log.debug("Consultando novedades empleado: {} proceso: {}", empleadoId, procesoId);
        List<ConceptoNominaDTO> conceptos = masterDataClient.findAllConceptosNomina();
        List<NovedadResponseDTO> novedades = novedadService
                .findByEmpleadoYProceso(empleadoId, procesoId)
                .stream()
                .map(n -> mapper.toResponse(n, conceptos))
                .toList();
        return ResponseEntity.ok(novedades);
    }
}
