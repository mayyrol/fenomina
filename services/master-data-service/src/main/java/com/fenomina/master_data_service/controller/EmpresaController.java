package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import com.fenomina.master_data_service.security.SecurityUtils;
import com.fenomina.master_data_service.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/master/empresas")
@RequiredArgsConstructor
@Slf4j
public class EmpresaController {

    private final EmpresaService empresaService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<EmpresaResponseDTO> create(
            @RequestPart("empresa") @Valid EmpresaRequestDTO request,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {

        log.info("Solicitud de creación de empresa con NIT: {}", request.empresaNit());

        EmpresaResponseDTO response = empresaService.create(request, logo);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponseDTO>> findAll(
            @RequestParam(required = false) String nombre) {

        String currentRole = SecurityUtils.getCurrentUserRole();
        Long currentUserEmpresaId = SecurityUtils.getCurrentUserEmpresaId();

        // CLIENTE_EMPRESA solo ve su empresa
        if ("CLIENTE_EMPRESA".equals(currentRole)) {
            if (currentUserEmpresaId == null) {
                return ResponseEntity.ok(List.of()); // Sin empresa asignada
            }
            // Devolver solo su empresa
            EmpresaResponseDTO empresa = empresaService.findById(currentUserEmpresaId);
            return ResponseEntity.ok(List.of(empresa));
        }

        // SUPER_ADMIN, RRHH, AUDITOR ven todas
        List<EmpresaResponseDTO> empresas = empresaService.findAll(nombre);
        return ResponseEntity.ok(empresas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> findById(@PathVariable Long id) {
        String currentRole = SecurityUtils.getCurrentUserRole();
        Long currentUserEmpresaId = SecurityUtils.getCurrentUserEmpresaId();

        // CLIENTE_EMPRESA solo puede ver su empresa
        if ("CLIENTE_EMPRESA".equals(currentRole)) {
            if (!id.equals(currentUserEmpresaId)) {
                throw new AccessDeniedException("No tiene acceso a esta empresa");
            }
        }

        EmpresaResponseDTO empresa = empresaService.findById(id);
        return ResponseEntity.ok(empresa);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<EmpresaResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("empresa") @Valid EmpresaRequestDTO request,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {

        log.info("Solicitud de actualización de empresa ID: {}", id);

        EmpresaResponseDTO response = empresaService.update(id, request, logo);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Solicitud de eliminación de empresa ID: {}", id);

        empresaService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
