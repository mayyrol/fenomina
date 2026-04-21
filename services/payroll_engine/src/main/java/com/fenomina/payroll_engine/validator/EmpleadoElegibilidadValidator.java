package com.fenomina.payroll_engine.validator;

import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.exception.EmpleadoNoElegibleException;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmpleadoElegibilidadValidator {

    public void validar(
            EmpleadoDTO empleado,
            TipoProceso tipoProceso,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo
    ) {
        List<String> errores = new ArrayList<>();

        validarEstadoActivo(empleado, errores);
        validarFechaIngreso(empleado, fechaFinPeriodo, errores);
        validarTipoContrato(empleado, tipoProceso, errores);
        validarSalarioIntegralPrestaciones(empleado, tipoProceso, errores);

        if (!errores.isEmpty()) {
            throw new EmpleadoNoElegibleException(
                    String.format("Empleado %s %s no elegible para liquidación %s: %s",
                            empleado.nombresEmp(),
                            empleado.apellidosEmp(),
                            tipoProceso.name(),
                            String.join(", ", errores)
                    )
            );
        }
    }

    public void validarLista(
            List<EmpleadoDTO> empleados,
            TipoProceso tipoProceso,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo
    ) {
        empleados.forEach(e -> validar(e, tipoProceso, fechaInicioPeriodo, fechaFinPeriodo));
    }

    // --- Validaciones individuales ---

    private void validarEstadoActivo(EmpleadoDTO empleado, List<String> errores) {
        if (!"ACTIVO".equals(empleado.estadoEmp())) {
            errores.add(String.format(
                    "el empleado no está activo (estado actual: %s)",
                    empleado.estadoEmp()
            ));
        }
    }

    private void validarFechaIngreso(
            EmpleadoDTO empleado,
            LocalDate fechaFinPeriodo,
            List<String> errores
    ) {
        if (empleado.fechaIngresoEmp() == null) {
            errores.add("no tiene fecha de ingreso registrada");
            return;
        }

        if (empleado.fechaIngresoEmp().isAfter(fechaFinPeriodo)) {
            errores.add(String.format(
                    "la fecha de ingreso (%s) es posterior al fin del periodo (%s)",
                    empleado.fechaIngresoEmp(),
                    fechaFinPeriodo
            ));
        }
    }

    private void validarTipoContrato(
            EmpleadoDTO empleado,
            TipoProceso tipoProceso,
            List<String> errores
    ) {
        if (empleado.tipoContratoEmp() == null) {
            errores.add("no tiene tipo de contrato registrado");
        }
    }

    private void validarSalarioIntegralPrestaciones(
            EmpleadoDTO empleado,
            TipoProceso tipoProceso,
            List<String> errores
    ) {
        // El salario integral no impide la liquidación pero sí
        // cambia el comportamiento del motor. No es un error,
        // se registra como advertencia en el log.
        // La exclusión real ocurre dentro de cada calculator.
    }
}
