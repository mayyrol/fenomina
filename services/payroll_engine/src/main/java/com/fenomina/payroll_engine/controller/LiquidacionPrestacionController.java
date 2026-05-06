package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.dto.request.LiquidarPrestacionRequestDTO;
import com.fenomina.payroll_engine.service.liquidacion.LiquidacionCesantiasService;
import com.fenomina.payroll_engine.service.liquidacion.LiquidacionInteresesService;
import com.fenomina.payroll_engine.service.liquidacion.LiquidacionPrimaService;
import com.fenomina.payroll_engine.service.proceso.ProcesoLiquidacionService;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
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
public class LiquidacionPrestacionController {

    private final LiquidacionPrimaService liquidacionPrimaService;
    private final LiquidacionCesantiasService liquidacionCesantiasService;
    private final LiquidacionInteresesService liquidacionInteresesService;
    private final ProcesoLiquidacionService procesoService;

    @PostMapping("/prima/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, String>> liquidarPrima(
            @PathVariable("procesoId") Long procesoId,
            @Valid @RequestBody LiquidarPrestacionRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Liquidando prima - proceso: {}, usuario: {}", procesoId, usuarioId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);

        validarTipoProceso(proceso, TipoProceso.PRIMA_SEMESTRAL);

        liquidacionPrimaService.liquidar(proceso, request.empleadosSeleccionados());

        procesoService.cambiarEstado(
                procesoId,
                EstadoProceso.PENDIENTE_PAGO,
                usuarioId
        );

        return ResponseEntity.ok(Map.of(
                "mensaje", "Prima de servicios liquidada exitosamente",
                "estado", EstadoProceso.PENDIENTE_PAGO.name()
        ));
    }

    @PostMapping("/cesantias/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, String>> liquidarCesantias(
            @PathVariable("procesoId") Long procesoId,
            @Valid @RequestBody LiquidarPrestacionRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Liquidando cesantías - proceso: {}, usuario: {}", procesoId, usuarioId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);
        validarTipoProceso(proceso, TipoProceso.CESANTIAS_ANUAL);

        liquidacionCesantiasService.liquidar(proceso, request.empleadosSeleccionados());

        // Pasar por CERRADO solo si está en BORRADOR
        if (proceso.getEstadoProcNomina() == EstadoProceso.BORRADOR) {
            procesoService.cambiarEstado(procesoId, EstadoProceso.CERRADO, usuarioId);
        }
        procesoService.cambiarEstado(procesoId, EstadoProceso.PENDIENTE_PAGO, usuarioId);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Cesantías liquidadas exitosamente",
                "estado", EstadoProceso.PENDIENTE_PAGO.name()
        ));
    }

    @PostMapping("/intereses-cesantias/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, String>> liquidarIntereses(
            @PathVariable("procesoId") Long procesoId,
            @Valid @RequestBody LiquidarPrestacionRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Liquidando intereses cesantías - proceso: {}, usuario: {}",
                procesoId, usuarioId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);
        validarTipoProceso(proceso, TipoProceso.INTERESES_CESANTIAS_ANUAL);

        liquidacionInteresesService.liquidar(proceso, request.empleadosSeleccionados());

        // Pasar por CERRADO solo si está en BORRADOR
        if (proceso.getEstadoProcNomina() == EstadoProceso.BORRADOR) {
            procesoService.cambiarEstado(procesoId, EstadoProceso.CERRADO, usuarioId);
        }
        procesoService.cambiarEstado(procesoId, EstadoProceso.PENDIENTE_PAGO, usuarioId);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Intereses sobre cesantías liquidados exitosamente",
                "estado", EstadoProceso.PENDIENTE_PAGO.name()
        ));
    }

    private void validarTipoProceso(ProcesoLiquidacion proceso, TipoProceso tipoEsperado) {
        if (proceso.getTipoProceso() != tipoEsperado) {
            throw new com.fenomina.payroll_engine.exception.CalculoNominaException(
                    String.format("El proceso %d no es de tipo %s",
                            proceso.getProcesoLiquiId(), tipoEsperado.name())
            );
        }
    }
}
