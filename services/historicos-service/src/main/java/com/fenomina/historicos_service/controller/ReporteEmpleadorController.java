package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.empleador.*;
import com.fenomina.historicos_service.service.ReporteEmpleadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historicos")
@RequiredArgsConstructor
public class ReporteEmpleadorController {

    private final ReporteEmpleadorService reporteEmpleadorService;

    @GetMapping("/seguridad-social/total")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteSegSocialTotalDTO>> getSegSocialTotal(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getSegSocialTotal(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/seguridad-social/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteSegSocialXEmpleadoDTO>> getSegSocialXEmpleado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getSegSocialXEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/provisiones/parafiscales/total")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteAportesParafTotalDTO>> getAportesParafTotal(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getAportesParafTotal(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/provisiones/parafiscales/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteAportesParafXEmpleadoDTO>> getAportesParafXEmpleado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getAportesParafXEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/provisiones/cargas-prestacionales")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteCargasPrestTotalDTO>> getCargasPrestacionales(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getCargasPrestacionalesTotal(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/provisiones/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteConceptosEmpleadorTotalDTO>> getConsolidadoEmpleador(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteEmpleadorService.getConceptosEmpleadorTotal(
                        empresaId, anio, periodo, pageable));
    }
}
