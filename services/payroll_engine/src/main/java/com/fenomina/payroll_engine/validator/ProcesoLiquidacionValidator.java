package com.fenomina.payroll_engine.validator;

import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.exception.ValidacionNominaException;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProcesoLiquidacionValidator {

    public void validarCreacion(
            TipoProceso tipoProceso,
            Integer anio,
            Integer periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (fechaInicio == null || fechaFin == null) {
            throw new ValidacionNominaException(
                    "fechas",
                    "Las fechas de inicio y fin del periodo son obligatorias"
            );
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new ValidacionNominaException(
                    "fechaFin",
                    "La fecha fin no puede ser anterior a la fecha inicio del periodo"
            );
        }

        if (tipoProceso == TipoProceso.NOMINA_MENSUAL ||
                tipoProceso == TipoProceso.NOMINA_QUINCENAL) {

            if (fechaInicio.getMonthValue() != fechaFin.getMonthValue() ||
                    fechaInicio.getYear() != fechaFin.getYear()) {
                throw new ValidacionNominaException(
                        "fechas",
                        "Las fechas de inicio y fin del periodo de nómina deben " +
                                "corresponder al mismo mes y año"
                );
            }

            int mesFechaInicio = fechaInicio.getMonthValue();
            int mesFechaFin    = fechaFin.getMonthValue();

            if (mesFechaInicio != periodo || mesFechaFin != periodo) {
                throw new ValidacionNominaException(
                        "periodo",
                        String.format(
                                "Las fechas ingresadas corresponden al mes %d pero el " +
                                        "periodo declarado es el mes %d. " +
                                        "Las fechas deben corresponder al mes seleccionado",
                                mesFechaInicio, periodo
                        )
                );
            }

            long diasPeriodo = java.time.temporal.ChronoUnit.DAYS
                    .between(fechaInicio, fechaFin) + 1;

            if (diasPeriodo < 12) {
                throw new ValidacionNominaException(
                        "fechas",
                        String.format(
                                "El periodo ingresado tiene %d día(s), lo cual es insuficiente. " +
                                        "El mínimo permitido es 12 días",
                                diasPeriodo
                        )
                );
            }

            if (diasPeriodo > 31) {
                throw new ValidacionNominaException(
                        "fechas",
                        String.format(
                                "El periodo ingresado tiene %d día(s), lo cual excede el máximo " +
                                        "permitido de 31 días",
                                diasPeriodo
                        )
                );
            }

        }

        validarPeriodoSegunTipo(tipoProceso, anio, periodo, fechaInicio, fechaFin);
    }

    public void validarEmpleadosSeleccionados(
            List<Long> empleadosSeleccionados,
            List<EmpleadoDTO> empleadosActivos,
            LocalDate fechaFinPeriodo
    ) {
        if (empleadosSeleccionados == null || empleadosSeleccionados.isEmpty()) {
            throw new ValidacionNominaException(
                    "empleadosSeleccionados",
                    "Debe seleccionar al menos un empleado para liquidar"
            );
        }

        Map<Long, EmpleadoDTO> activosPorId = empleadosActivos.stream()
                .collect(Collectors.toMap(EmpleadoDTO::empleadoId, e -> e));

        List<Long> noActivos = empleadosSeleccionados.stream()
                .filter(empId -> !activosPorId.containsKey(empId))
                .toList();

        if (!noActivos.isEmpty()) {
            throw new ValidacionNominaException(
                    "empleadosSeleccionados",
                    String.format(
                            "Los siguientes empleados no están activos o no pertenecen " +
                                    "a la empresa: %s",
                            noActivos
                    )
            );
        }

        for (Long empId : empleadosSeleccionados) {
            EmpleadoDTO empleado = activosPorId.get(empId);
            if (empleado.fechaIngresoEmp() != null &&
                    empleado.fechaIngresoEmp().isAfter(fechaFinPeriodo)) {
                throw new ValidacionNominaException(
                        "empleadosSeleccionados",
                        String.format(
                                "El empleado %s %s tiene fecha de ingreso %s, que es " +
                                        "posterior al periodo a liquidar (%s). " +
                                        "No puede incluirse en este proceso",
                                empleado.nombresEmp(),
                                empleado.apellidosEmp(),
                                empleado.fechaIngresoEmp(),
                                fechaFinPeriodo
                        )
                );
            }
        }
    }

    public void validarDiasLaborados(
            Map<Long, Integer> diasLaboradosPorEmpleado,
            List<Long> empleadosSeleccionados,
            boolean esQuincenal
    ) {
        int maxDias = esQuincenal ? 15 : 30;

        for (Long empleadoId : empleadosSeleccionados) {
            Integer dias = diasLaboradosPorEmpleado.get(empleadoId);

            if (dias == null || dias <= 0) {
                throw new ValidacionNominaException(
                        "diasLaborados",
                        String.format(
                                "El empleado %d no tiene días laborados registrados " +
                                        "o el valor es inválido",
                                empleadoId
                        )
                );
            }

            if (dias > maxDias) {
                throw new ValidacionNominaException(
                        "diasLaborados",
                        String.format(
                                "El empleado %d tiene %d días laborados, que supera " +
                                        "el máximo permitido de %d días para el periodo %s",
                                empleadoId,
                                dias,
                                maxDias,
                                esQuincenal ? "quincenal" : "mensual"
                        )
                );
            }
        }
    }

    public void validarNovedadesFueraDePeriodo(
            List<Novedad> novedades,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo
    ) {
        List<Novedad> fuera = novedades.stream()
                .filter(n -> n.getFechaFinAusen() != null
                        && n.getFechaFinAusen().isAfter(fechaFinPeriodo))
                .toList();

        if (!fuera.isEmpty()) {
            throw new ValidacionNominaException(
                    "novedades",
                    String.format(
                            "%d novedad(es) tienen fecha fin fuera del periodo. " +
                                    "Deben ajustarse antes de cerrar el proceso",
                            fuera.size()
                    )
            );
        }
    }

    public void validarEstadoParaLiquidar(ProcesoLiquidacion proceso) {
        if (proceso.getEstadoProcNomina() != EstadoProceso.CERRADO) {
            throw new ValidacionNominaException(
                    "estadoProceso",
                    String.format(
                            "El proceso debe estar en estado CERRADO para liquidar. " +
                                    "Estado actual: %s",
                            proceso.getEstadoProcNomina()
                    )
            );
        }
    }

    // --- Validaciones por tipo de proceso ---

    private void validarPeriodoSegunTipo(
            TipoProceso tipoProceso,
            Integer anio,
            Integer periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        switch (tipoProceso) {
            case NOMINA_MENSUAL -> {
                if (periodo < 1 || periodo > 12) {
                    throw new ValidacionNominaException(
                            "periodo",
                            "Para nómina mensual el periodo debe estar entre 1 y 12"
                    );
                }
            }
            case NOMINA_QUINCENAL -> {
                if (periodo < 1 || periodo > 24) {
                    throw new ValidacionNominaException(
                            "periodo",
                            "Para nómina quincenal el periodo debe estar entre 1 y 24"
                    );
                }
            }
            case PRIMA_SEMESTRAL -> {
                if (periodo != 1 && periodo != 2) {
                    throw new ValidacionNominaException(
                            "periodo",
                            "Para prima semestral el periodo debe ser 1 (enero-junio) " +
                                    "o 2 (julio-diciembre)"
                    );
                }
                validarFechasPrima(periodo, fechaInicio, fechaFin);
            }
            case CESANTIAS_ANUAL, INTERESES_CESANTIAS_ANUAL -> {
                if (periodo != 1) {
                    throw new ValidacionNominaException(
                            "periodo",
                            "Para cesantías e intereses el periodo debe ser 1 (anual)"
                    );
                }
                validarFechasCesantias(anio, fechaInicio, fechaFin);
            }
        }
    }

    private void validarFechasPrima(
            Integer periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (periodo == 1) {
            if (fechaInicio.getMonthValue() != 1 || fechaInicio.getDayOfMonth() != 1) {
                throw new ValidacionNominaException(
                        "fechaInicio",
                        "Para prima del primer semestre la fecha inicio debe ser 01/01"
                );
            }
            if (fechaFin.getMonthValue() != 6 || fechaFin.getDayOfMonth() != 30) {
                throw new ValidacionNominaException(
                        "fechaFin",
                        "Para prima del primer semestre la fecha fin debe ser 30/06"
                );
            }
        } else {
            if (fechaInicio.getMonthValue() != 7 || fechaInicio.getDayOfMonth() != 1) {
                throw new ValidacionNominaException(
                        "fechaInicio",
                        "Para prima del segundo semestre la fecha inicio debe ser 01/07"
                );
            }
            if (fechaFin.getMonthValue() != 12 || fechaFin.getDayOfMonth() != 31) {
                throw new ValidacionNominaException(
                        "fechaFin",
                        "Para prima del segundo semestre la fecha fin debe ser 31/12"
                );
            }
        }
    }

    private void validarFechasCesantias(
            Integer anio,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (fechaInicio.getMonthValue() != 1 || fechaInicio.getDayOfMonth() != 1
                || fechaInicio.getYear() != anio) {
            throw new ValidacionNominaException(
                    "fechaInicio",
                    String.format(
                            "Para cesantías la fecha inicio debe ser 01/01/%d", anio)
            );
        }

        if (fechaFin.getMonthValue() != 12 || fechaFin.getDayOfMonth() != 31
                || fechaFin.getYear() != anio) {
            throw new ValidacionNominaException(
                    "fechaFin",
                    String.format(
                            "Para cesantías la fecha fin debe ser 31/12/%d", anio)
            );
        }
    }
}