package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.dto.request.LiquidarNominaRequestDTO;
import com.fenomina.payroll_engine.service.calculo.engine.CalculoNominaEngine;
import com.fenomina.payroll_engine.service.proceso.ProcesoLiquidacionService;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll/liquidacion")
@RequiredArgsConstructor
@Slf4j
public class LiquidacionNominaController {

    private final CalculoNominaEngine calculoNominaEngine;
    private final ProcesoLiquidacionService procesoService;

    @PostMapping("/nomina/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, String>> liquidarNomina(
            @PathVariable("procesoId") Long procesoId,
            @Valid @RequestBody LiquidarNominaRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Liquidando nómina - proceso: {}, usuario: {}", procesoId, usuarioId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);

        calculoNominaEngine.liquidar(
                proceso,
                request.empleadosSeleccionados(),
                request.diasLaborados()
        );

        procesoService.cambiarEstado(
                procesoId,
                EstadoProceso.PENDIENTE_PAGO,
                usuarioId
        );

        return ResponseEntity.ok(Map.of(
                "mensaje", "Nómina liquidada exitosamente",
                "estado", EstadoProceso.PENDIENTE_PAGO.name()
        ));
    }
}