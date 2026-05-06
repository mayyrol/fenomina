package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.conceptos.*;
import com.fenomina.historicos_service.service.ReporteConceptosService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historicos/conceptos")
@RequiredArgsConstructor
public class ReporteConceptosController {

    private final ReporteConceptosService reporteConceptosService;

    @GetMapping("/horas-recargos/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteHorasRecargosDTO>> getHorasRecargosPorEmpleado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getHorasRecargosPorEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/horas-recargos/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteHorasRecargosTotalDTO>> getHorasRecargosConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getHorasRecargosTotalEmpresa(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/incapacidades/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteIncapacidadesDTO>> getIncapacidadesPorEmpleado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getIncapacidadesPorEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/incapacidades/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteIncapacidadesTotalDTO>> getIncapacidadesConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getIncapacidadesTotalEmpresa(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/licencias/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteLicenciasDTO>> getLicenciasPorEmpleado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getLicenciasPorEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/licencias/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteLicenciasTotalDTO>> getLicenciasConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getLicenciasTotalEmpresa(
                        empresaId, anio, periodo, pageable));
    }

    @GetMapping("/retefuente")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteRetefuenteDTO>> getRetefuente(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getRetefuentePorEmpleado(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/vacaciones/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteVacacionesEmpresaDTO>> getVacacionesPorEmpresa(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getVacacionesPorEmpresa(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/vacaciones/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteVacacionesTotalDTO>> getVacacionesConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteConceptosService.getVacacionesTotalEmpresa(
                        empresaId, anio, periodo, pageable));
    }
}
