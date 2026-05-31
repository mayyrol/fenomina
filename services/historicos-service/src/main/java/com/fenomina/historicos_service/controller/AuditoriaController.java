package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.auditoria.AuditLogResponseDTO;
import com.fenomina.historicos_service.dto.auditoria.SystemAuditLogResponseDTO;
import com.fenomina.historicos_service.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/historicos/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/auth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getAuthLogs(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                auditoriaService.getAuthAuditLogs(
                        usuarioId, username, accion, desde, hasta, pageable));
    }

    @GetMapping("/sistema")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SystemAuditLogResponseDTO>> getSystemLogs(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String tablaAfectada,
            @RequestParam(required = false) String operacion,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        return ResponseEntity.ok(
                auditoriaService.getSystemAuditLogs(
                        usuarioId, username, tablaAfectada,
                        operacion, empresaId, desde, hasta));
    }
}