package com.fenomina.payroll_engine.controller;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.dto.request.LiquidarNominaRequestDTO;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.exception.ValidacionNominaException;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll/liquidacion")
@RequiredArgsConstructor
@Slf4j
public class LiquidacionNominaController {

    private final CalculoNominaEngine calculoNominaEngine;
    private final ProcesoLiquidacionService procesoService;
    private final MasterDataClientWrapper masterDataClient;

    @PostMapping("/nomina/{procesoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RRHH')")
    public ResponseEntity<Map<String, String>> liquidarNomina(
            @PathVariable("procesoId") Long procesoId,
            @Valid @RequestBody LiquidarNominaRequestDTO request
    ) {
        Long usuarioId = SecurityUtils.getCurrentUserId();
        log.info("Liquidando nómina - proceso: {}, usuario: {}", procesoId, usuarioId);

        ProcesoLiquidacion proceso = procesoService.findById(procesoId);

        // Validación 3: fecha de ingreso del empleado no puede ser
        // posterior a la fecha fin del periodo
        List<EmpleadoDTO> empleadosActivos = masterDataClient
                .findEmpleadosActivos(proceso.getFkIdEmpresa());

        Map<Long, EmpleadoDTO> empleadosPorId = empleadosActivos.stream()
                .collect(java.util.stream.Collectors.toMap(
                        EmpleadoDTO::empleadoId, e -> e
                ));

        for (Long empId : request.empleadosSeleccionados()) {
            EmpleadoDTO empleado = empleadosPorId.get(empId);
            if (empleado != null &&
                    empleado.fechaIngresoEmp() != null &&
                    empleado.fechaIngresoEmp().isAfter(proceso.getFechaFinPeriodo())) {
                throw new ValidacionNominaException(
                        "empleadosSeleccionados",
                        String.format(
                                "El empleado %s %s tiene fecha de ingreso %s, " +
                                        "que es posterior al periodo a liquidar (%s). " +
                                        "No puede incluirse en este proceso",
                                empleado.nombresEmp(),
                                empleado.apellidosEmp(),
                                empleado.fechaIngresoEmp(),
                                proceso.getFechaFinPeriodo()
                        )
                );
            }
        }

        // Validación 5: días laborados no deben superar el máximo
        // según tipo de proceso
        boolean esQuincenal = proceso.getTipoProceso() ==
                TipoProceso.NOMINA_QUINCENAL;
        int maxDias = esQuincenal ? 15 : 30;

        for (Map.Entry<Long, Integer> entry :
                request.diasLaborados().entrySet()) {
            Integer dias = entry.getValue();
            if (dias != null && dias > maxDias) {
                EmpleadoDTO empleado = empleadosPorId.get(entry.getKey());
                String nombreEmp = empleado != null
                        ? empleado.nombresEmp() + " " + empleado.apellidosEmp()
                        : "ID " + entry.getKey();
                throw new ValidacionNominaException(
                        "diasLaborados",
                        String.format(
                                "El empleado %s tiene %d días laborados, " +
                                        "que supera el máximo de %d días para %s",
                                nombreEmp,
                                dias,
                                maxDias,
                                esQuincenal ? "nómina quincenal" : "nómina mensual"
                        )
                );
            }
            if (dias != null && dias <= 0) {
                throw new ValidacionNominaException(
                        "diasLaborados",
                        String.format(
                                "Los días laborados del empleado ID %d " +
                                        "deben ser un valor mayor a cero",
                                entry.getKey()
                        )
                );
            }
        }

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