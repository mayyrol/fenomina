package com.fenomina.historicos_service.controller;

import com.fenomina.historicos_service.dto.notificaciones.NotificacionVacacionesDTO;
import com.fenomina.historicos_service.service.NotificacionVacacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/historicos/notificaciones")
@RequiredArgsConstructor
public class NotificacionVacacionesController {

    private final NotificacionVacacionesService notificacionService;

    @PostMapping("/evaluar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<List<NotificacionVacacionesDTO>> evaluar() {
        return ResponseEntity.ok(notificacionService.evaluar());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<List<NotificacionVacacionesDTO>> listar(
            @RequestParam(required = false) String nombreEmpresa,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(
                notificacionService.listar(nombreEmpresa, desde, hasta));
    }

    @GetMapping("/no-leidas/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        return ResponseEntity.ok(
                Map.of("count", notificacionService.contarNoLeidas()));
    }

    @PatchMapping("/{id}/leer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leer-todas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Void> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }
}