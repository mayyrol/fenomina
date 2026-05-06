package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.nomina.*;
import com.fenomina.historicos_service.service.ReporteNominaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historicos/nominas")
@RequiredArgsConstructor
public class ReporteNominaController {

    private final ReporteNominaService reporteNominaService;

    @GetMapping("/desprendible/{cabecNominaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<ReporteNominaEmpleadoDTO> getDesprendible(
            @PathVariable Long cabecNominaId) {

        return ResponseEntity.ok(
                reporteNominaService.getDesprendibleNomina(cabecNominaId));
    }

    @GetMapping("/empleados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteNominaEmpleadosDTO>> getReporteNominaEmpleados(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteNominaService.getReporteNominaEmpleados(
                        empresaId, anio, periodo, documento, nombres, pageable));
    }

    @GetMapping("/consolidado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<ReporteNominaTotalEmpresaDTO>> getConsolidado(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteNominaService.getReporteNominaTotalEmpresa(
                        empresaId, anio, periodo, pageable));
    }


    @GetMapping("/estados")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH','AUDITOR','CLIENTE_EMPRESA')")
    public ResponseEntity<Page<EstadoNominaDTO>> getEstadosNominas(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteNominaService.getEstadosNominas(
                        empresaId, anio, periodo, estado, pageable));
    }

    @GetMapping("/liquidacion")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','RRHH')")
    public ResponseEntity<Page<EstadoNominaDTO>> getNominasParaLiquidar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                reporteNominaService.getEstadosNominasBorradorYPagado(
                        empresaId, anio, periodo, pageable));
    }
}