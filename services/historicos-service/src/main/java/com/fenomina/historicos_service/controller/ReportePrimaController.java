package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.prestaciones.ReportePrimaEmpresaDTO;
import com.fenomina.historicos_service.dto.prestaciones.ReporteTotalesPrimasDTO;
import com.fenomina.historicos_service.service.ReportePrestacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historicos/primas")
@RequiredArgsConstructor
public class ReportePrimaController {

    private final ReportePrestacionesService reportePrestacionesService;

    @GetMapping("/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReportePrimaEmpresaDTO>> getPrimasEmpleados(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reportePrestacionesService.getReportePrimasEmpresa(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteTotalesPrimasDTO>> getPrimasConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reportePrestacionesService.getTotalesPrimasEmpresa(
                        empresaId, anio, periodo, pageable));
    }
}