package com.fenomina.master_data_service.util;

import java.time.LocalDate;
import java.time.Period;

public final class DateUtils {

    private DateUtils() {
        throw new UnsupportedOperationException("Clase de utilidades");
    }

    public static int calcularAntiguedadEnAnios(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 0;
        }
        Period periodo = Period.between(fechaInicio, fechaFin);
        return periodo.getYears();
    }

    public static int calcularAntiguedadActual(LocalDate fechaInicio) {
        return calcularAntiguedadEnAnios(fechaInicio, LocalDate.now());
    }

    public static boolean esFechaFutura(LocalDate fecha) {
        return fecha != null && fecha.isAfter(LocalDate.now());
    }

    public static boolean esFechaInicioAnteriorAFin(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            return true; // No validar si alguna es nula
        }
        return inicio.isBefore(fin);
    }
}
