package com.fenomina.payroll_engine.service.liquidacion;

import java.time.LocalDate;

public final class LiquidacionFechaUtils {

    private LiquidacionFechaUtils() {}

    public static int calcularDias(LocalDate inicio, LocalDate fin) {
        int meses = fin.getMonthValue() - inicio.getMonthValue()
                + (fin.getYear() - inicio.getYear()) * 12;
        int diasUltimoMes = Math.min(fin.getDayOfMonth(), 30)
                - Math.min(inicio.getDayOfMonth(), 30);
        return meses * 30 + diasUltimoMes;
    }
}
