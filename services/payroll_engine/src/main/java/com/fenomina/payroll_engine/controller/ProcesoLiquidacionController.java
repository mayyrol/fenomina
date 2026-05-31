package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.dto.request.CambiarEstadoProcesoRequestDTO;
import com.fenomina.payroll_engine.dto.request.ProcesoLiquidacionRequestDTO;
import com.fenomina.payroll_engine.dto.response.ProcesoLiquidacionResponseDTO;
import com.fenomina.payroll_engine.mapper.ProcesoLiquidacionMapper;
import com.fenomina.payroll_engine.service.proceso.ProcesoLiquidacionService;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.security.SecurityUtils;
import com.fenomina.payroll_engine.service.proceso.ResultadoCambioEstado;
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
@RequestMapping("/api/payroll/procesos")
@RequiredArgsConstructor
@Slf4j
public class ProcesoLiquidacionController {

    private final ProcesoLiquidacionService procesoService;
    private final ProcesoLiquidacionMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<ProcesoLiquidacionResponseDTO> crear(
            @Valid @RequestBody ProcesoLiquidacionRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Creando proceso de liquidación - usuario: {}, empresa: {}",
                usuarioId, request.empresaId());

        ProcesoLiquidacion proceso = procesoService.crear(
                request.empresaId(),
                usuarioId,
                TipoProceso.valueOf(request.tipoProceso()),
                request.anio(),
                request.periodo(),
                request.fechaInicio(),
                request.fechaFin()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(proceso));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<List<ProcesoLiquidacionResponseDTO>> findByEmpresa(
            @RequestParam("empresaId") Long empresaId,
            @RequestParam(value = "tipoProceso", required = false) String tipoProceso
    ) {
        String rol = SecurityUtils.getCurrentUserRole();
        Long empresaIdUsuario = SecurityUtils.getCurrentUserEmpresaId();

        if ("CLIENTE_EMPRESA".equals(rol)) {
            if (empresaIdUsuario == null || !empresaIdUsuario.equals(empresaId)) {
                throw new AccessDeniedException("No tiene acceso a esta empresa");
            }
        }

        log.debug("Consultando procesos - empresa: {}, tipoProceso: {}", empresaId, tipoProceso);

        List<ProcesoLiquidacion> procesos = procesoService.findByEmpresa(empresaId);

        if (tipoProceso != null && !tipoProceso.isBlank()) {
            procesos = procesos.stream()
                    .filter(p -> p.getTipoProceso() != null &&
                            p.getTipoProceso().name().equals(tipoProceso))
                    .toList();
        }

        List<ProcesoLiquidacionResponseDTO> response = mapper.toResponseList(procesos);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH', 'CLIENTE_EMPRESA', 'AUDITOR')")
    public ResponseEntity<ProcesoLiquidacionResponseDTO> findById(
            @PathVariable("id") Long id
    ) {
        log.debug("Consultando proceso: {}", id);
        ProcesoLiquidacion proceso = procesoService.findById(id);

        String rol = SecurityUtils.getCurrentUserRole();
        Long empresaIdUsuario = SecurityUtils.getCurrentUserEmpresaId();

        if ("CLIENTE_EMPRESA".equals(rol)) {
            if (empresaIdUsuario == null || !proceso.getFkIdEmpresa().equals(empresaIdUsuario)) {
                throw new AccessDeniedException("No tiene acceso a este proceso");
            }
        }

        return ResponseEntity.ok(mapper.toResponse(proceso));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<ProcesoLiquidacionResponseDTO> cambiarEstado(
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarEstadoProcesoRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();

        ResultadoCambioEstado resultado = procesoService.cambiarEstado(
                id,
                EstadoProceso.valueOf(request.nuevoEstado()),
                usuarioId
        );

        ProcesoLiquidacionResponseDTO response = mapper.toResponse(resultado.proceso());

        if (!resultado.advertencias().isEmpty()) {
            response = new ProcesoLiquidacionResponseDTO(
                    response.procesoLiquiId(),
                    response.fkIdEmpresa(),
                    response.tipoProceso(),
                    response.estadoProcNomina(),
                    response.anio(),
                    response.periodo(),
                    response.fechaInicioPeriodo(),
                    response.fechaFinPeriodo(),
                    response.createdAt(),
                    response.updatedAt(),
                    response.cantidadEmpleados(),
                    response.totalNeto(),
                    response.totalIntereses(),
                    resultado.advertencias()
            );
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        log.info("Eliminando proceso: {}", id);
        procesoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}