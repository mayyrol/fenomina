package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.dto.request.ContratoConceptoRequestDTO;
import com.fenomina.master_data_service.dto.response.ContratoConceptoResponseDTO;
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
            @PathVariable Long empleadoId) {

        log.debug("Consultando conceptos del empleado ID: {}", empleadoId);

        List<ContratoConceptoResponseDTO> conceptos = contratoConceptoService.findByEmpleadoId(empleadoId);

        return ResponseEntity.ok(conceptos);
    }


    @GetMapping("/contratos-concepto/{id}")
    public ResponseEntity<ContratoConceptoResponseDTO> findById(@PathVariable Long id) {
        log.debug("Consultando contrato concepto ID: {}", id);

        ContratoConceptoResponseDTO contratoConcepto = contratoConceptoService.findById(id);

        return ResponseEntity.ok(contratoConcepto);
    }


    @DeleteMapping("/contratos-concepto/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Solicitud de eliminación de contrato concepto ID: {}", id);

        contratoConceptoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
