package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.dto.request.ContratoConceptoRequestDTO;
import com.fenomina.master_data_service.dto.response.ConceptoNominaInternalDTO;
import com.fenomina.master_data_service.dto.response.ConceptoNominaResponseDTO;
import com.fenomina.master_data_service.dto.response.ContratoConceptoResponseDTO;
import com.fenomina.master_data_service.repository.ConceptoNominaRepository;
import com.fenomina.master_data_service.service.ConceptoNominaInternalService;
import com.fenomina.master_data_service.service.ContratoConceptoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
@Slf4j
public class ContratoConceptoController {

    private final ContratoConceptoService contratoConceptoService;
    private final ConceptoNominaRepository conceptoNominaRepository;
    private final ConceptoNominaInternalService conceptoNominaInternalService;

    @PostMapping("/contratos-concepto")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<ContratoConceptoResponseDTO> create(
            @Valid @RequestBody ContratoConceptoRequestDTO request) {

        log.info("Solicitud de asignación de concepto {} a empleado {}",
                request.conceptoNominaId(), request.empleadoId());

        ContratoConceptoResponseDTO response = contratoConceptoService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/empleados/{empleadoId}/conceptos")
    public ResponseEntity<List<ContratoConceptoResponseDTO>> findByEmpleadoId(
            @PathVariable("empleadoId") Long empleadoId) {

        log.debug("Consultando conceptos del empleado ID: {}", empleadoId);

        List<ContratoConceptoResponseDTO> conceptos = contratoConceptoService.findByEmpleadoId(empleadoId);

        return ResponseEntity.ok(conceptos);
    }


    @GetMapping("/contratos-concepto/{id}")
    public ResponseEntity<ContratoConceptoResponseDTO> findById(@PathVariable("id") Long id) {
        log.debug("Consultando contrato concepto ID: {}", id);

        ContratoConceptoResponseDTO contratoConcepto = contratoConceptoService.findById(id);

        return ResponseEntity.ok(contratoConcepto);
    }


    @DeleteMapping("/contratos-concepto/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        log.info("Solicitud de eliminación de contrato concepto ID: {}", id);

        contratoConceptoService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conceptos-nomina/contrato")
    public ResponseEntity<List<ConceptoNominaResponseDTO>> findConceptosContrato() {
        log.debug("Consultando conceptos de nómina disponibles para contrato");

        List<ConceptoNominaResponseDTO> response = conceptoNominaRepository
                .findConceptosDisponiblesParaContrato()
                .stream()
                .map(c -> new ConceptoNominaResponseDTO(
                        c.getConcepNominaId(),
                        c.getNombreConcepNomina(),
                        c.getDescrConcepNomina(),
                        c.getCategoriaConcNomina() != null
                                ? c.getCategoriaConcNomina().name()
                                : null
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/contratos-concepto/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<ContratoConceptoResponseDTO> update(
            @PathVariable("id") Long id,
            @RequestBody ContratoConceptoRequestDTO request) {
        log.info("Solicitud de actualización de contrato concepto ID: {}", id);
        ContratoConceptoResponseDTO response = contratoConceptoService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conceptos-nomina/novedades")
    public ResponseEntity<List<ConceptoNominaInternalDTO>> findConceptosParaNovedades() {
        log.debug("Consultando catálogo de conceptos de nómina para formulario de novedades");
        return ResponseEntity.ok(conceptoNominaInternalService.findAll());
    }
}
