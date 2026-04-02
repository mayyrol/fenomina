package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.dto.request.CambiarEstadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoUpdateRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoDetalleResponseDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoResponseDTO;
import com.fenomina.master_data_service.enums.EstadoEmpleado;
import com.fenomina.master_data_service.security.SecurityUtils;
import com.fenomina.master_data_service.service.EmpleadoService;
import com.fenomina.master_data_service.util.ValidationMessages;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master/empleados")
@RequiredArgsConstructor
@Slf4j
public class EmpleadoController {

    private final EmpleadoService empleadoService;


    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<EmpleadoResponseDTO> create(
            @Valid @RequestBody EmpleadoRequestDTO request) {

        log.info("Solicitud de creación de empleado - Documento: {}, Empresa: {}",
                request.documentoEmp(), request.empresaId());

        EmpleadoResponseDTO response = empleadoService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<EmpleadoResponseDTO>> findAll(
            @RequestParam(name = "empresaId", required = false) Long empresaId,
            @RequestParam(name = "estado", required = false) EstadoEmpleado estado,
            @RequestParam(name = "documento", required = false) String documento) {

        String currentRole = SecurityUtils.getCurrentUserRole();
        Long userEmpresaId = SecurityUtils.getCurrentUserEmpresaId();

        log.debug("Listando empleados - Usuario rol: {}, Empresa: {}", currentRole, userEmpresaId);

        // Si es CLIENTE_EMPRESA, forzar filtro por su empresa
        if ("CLIENTE_EMPRESA".equals(currentRole)) {
            if (userEmpresaId == null) {
                log.warn("Usuario CLIENTE_EMPRESA sin empresa asociada");
                throw new AccessDeniedException(ValidationMessages.FORBIDDEN_EMPRESA_ACCESS);
            }
            empresaId = userEmpresaId;  // Forzar filtro
            log.debug("CLIENTE_EMPRESA - Filtrando por empresa: {}", empresaId);
        }

        List<EmpleadoResponseDTO> empleados = empleadoService.findByFilters(empresaId, estado, documento);

        return ResponseEntity.ok(empleados);
    }


    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoDetalleResponseDTO> findById(@PathVariable("id") Long id) {
        log.debug("Consultando empleado ID: {}", id);

        EmpleadoDetalleResponseDTO empleado = empleadoService.findById(id);

        // Validar acceso si es CLIENTE_EMPRESA
        String currentRole = SecurityUtils.getCurrentUserRole();
        if ("CLIENTE_EMPRESA".equals(currentRole)) {
            Long userEmpresaId = SecurityUtils.getCurrentUserEmpresaId();

            if (userEmpresaId == null || !userEmpresaId.equals(empleado.empresa().empresaId())) {
                log.warn("CLIENTE_EMPRESA intenta acceder a empleado de otra empresa - Usuario empresa: {}, Empleado empresa: {}",
                        userEmpresaId, empleado.empresa().empresaId());
                throw new AccessDeniedException(ValidationMessages.FORBIDDEN_EMPRESA_ACCESS);
            }
        }

        return ResponseEntity.ok(empleado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<EmpleadoResponseDTO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody EmpleadoUpdateRequestDTO request) {

        log.info("Solicitud de actualización de empleado ID: {}", id);

        EmpleadoResponseDTO response = empleadoService.update(id, request);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<EmpleadoResponseDTO> cambiarEstado(
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarEstadoRequestDTO request) {

        log.info("Solicitud de cambio de estado de empleado ID: {} a {}", id, request.nuevoEstado());

        EmpleadoResponseDTO response = empleadoService.cambiarEstado(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        log.info("Solicitud de eliminación de empleado ID: {}", id);

        empleadoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
