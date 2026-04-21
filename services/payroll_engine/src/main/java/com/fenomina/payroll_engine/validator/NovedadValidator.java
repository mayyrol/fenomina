package com.fenomina.payroll_engine.validator;

import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.exception.ValidacionNominaException;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class NovedadValidator {

    private static final int MAX_DIAS_MES = 30;
    private static final int MAX_DIAS_QUINCENA = 15;
    private static final BigDecimal MAX_HORAS_EXTRA_MES = BigDecimal.valueOf(48);
    private static final BigDecimal LIMITE_NO_SALARIAL = new BigDecimal("0.40");

    public void validarNovedad(
            Novedad novedad,
            ConceptoNominaDTO concepto,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo,
            boolean esQuincenal
    ) {
        validarConceptoNoNulo(concepto, novedad.getFkConcepNominaId());
        validarSegunTipoEntrada(novedad, concepto, esQuincenal);
        validarFechasAusencia(novedad, fechaInicioPeriodo, fechaFinPeriodo);
    }

    public void validarLimiteNoSalarial(
            BigDecimal totalNoSalarial,
            BigDecimal totalSalarial,
            String nombresEmpleado,
            String apellidosEmpleado
    ) {
        if (totalSalarial.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal totalRemuneracion = totalSalarial.add(totalNoSalarial);
        BigDecimal proporcion = totalNoSalarial.divide(
                totalRemuneracion, 4, java.math.RoundingMode.HALF_UP
        );

        if (proporcion.compareTo(LIMITE_NO_SALARIAL) > 0) {
            throw new ValidacionNominaException(
                    "pagosNoSalariales",
                    String.format(
                            "Empleado %s %s supera el límite del 40%% de pagos no " +
                                    "constitutivos de salario. Proporción actual: %.2f%%",
                            nombresEmpleado,
                            apellidosEmpleado,
                            proporcion.multiply(BigDecimal.valueOf(100))
                    )
            );
        }
    }

    // --- Validaciones internas ---

    private void validarConceptoNoNulo(ConceptoNominaDTO concepto, Long concepNominaId) {
        if (concepto == null) {
            throw new ValidacionNominaException(
                    "fkConcepNominaId",
                    String.format("Concepto de nómina no encontrado: %d", concepNominaId)
            );
        }
    }

    private void validarSegunTipoEntrada(
            Novedad novedad,
            ConceptoNominaDTO concepto,
            boolean esQuincenal
    ) {
        String tipoEntrada = concepto.nombreConcepNomina();

        switch (concepto.categoriaConcNomina()) {
            case "DEVENGO", "PROVISION" -> {
                if (esTipoHoras(tipoEntrada)) {
                    validarHoras(novedad, tipoEntrada);
                } else if (esTipoDias(tipoEntrada)) {
                    validarDias(novedad, esQuincenal);
                } else {
                    validarValorFijo(novedad);
                }
            }
            case "DEDUCCION" -> validarValorFijo(novedad);
            default -> { }
        }
    }

    private void validarHoras(Novedad novedad, String nombreConcepto) {
        if (novedad.getCantidadHorasNovedad() == null
                || novedad.getCantidadHorasNovedad()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNominaException(
                    "cantidadHorasNovedad",
                    String.format("El concepto '%s' requiere cantidad de horas mayor a cero",
                            nombreConcepto)
            );
        }

        if (novedad.getCantidadHorasNovedad()
                .compareTo(MAX_HORAS_EXTRA_MES) > 0) {
            throw new ValidacionNominaException(
                    "cantidadHorasNovedad",
                    String.format(
                            "La cantidad de horas extra (%s) supera el máximo legal " +
                                    "mensual permitido (%s horas)",
                            novedad.getCantidadHorasNovedad(),
                            MAX_HORAS_EXTRA_MES
                    )
            );
        }
    }

    private void validarDias(Novedad novedad, boolean esQuincenal) {
        if (novedad.getCantidadDiasNovedad() == null
                || novedad.getCantidadDiasNovedad() <= 0) {
            throw new ValidacionNominaException(
                    "cantidadDiasNovedad",
                    "La cantidad de días debe ser mayor a cero"
            );
        }

        int maxDias = esQuincenal ? MAX_DIAS_QUINCENA : MAX_DIAS_MES;

        if (novedad.getCantidadDiasNovedad() > maxDias) {
            throw new ValidacionNominaException(
                    "cantidadDiasNovedad",
                    String.format(
                            "La cantidad de días (%d) supera el máximo permitido " +
                                    "para el periodo (%d días)",
                            novedad.getCantidadDiasNovedad(),
                            maxDias
                    )
            );
        }
    }

    private void validarValorFijo(Novedad novedad) {
        if (novedad.getValorRefNovedad() == null
                || novedad.getValorRefNovedad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNominaException(
                    "valorRefNovedad",
                    "El valor de la novedad debe ser mayor a cero"
            );
        }
    }

    private void validarFechasAusencia(
            Novedad novedad,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo
    ) {
        if (novedad.getFechaInicioAusen() == null
                && novedad.getFechaFinAusen() == null) {
            return;
        }

        if (novedad.getFechaInicioAusen() != null
                && novedad.getFechaFinAusen() != null) {
            if (novedad.getFechaFinAusen().isBefore(novedad.getFechaInicioAusen())) {
                throw new ValidacionNominaException(
                        "fechaFinAusen",
                        "La fecha fin de ausencia no puede ser anterior a la fecha inicio"
                );
            }
        }

        if (novedad.getFechaInicioAusen() != null
                && novedad.getFechaInicioAusen().isBefore(fechaInicioPeriodo)) {
            throw new ValidacionNominaException(
                    "fechaInicioAusen",
                    String.format(
                            "La fecha inicio de ausencia (%s) es anterior al inicio " +
                                    "del periodo (%s)",
                            novedad.getFechaInicioAusen(),
                            fechaInicioPeriodo
                    )
            );
        }

        if (novedad.getFechaFinAusen() != null
                && novedad.getFechaFinAusen().isAfter(fechaFinPeriodo)) {
            throw new ValidacionNominaException(
                    "fechaFinAusen",
                    String.format(
                            "La fecha fin de ausencia (%s) excede el fin del periodo (%s). " +
                                    "Si el evento continúa en el siguiente periodo, debe " +
                                    "registrarse por separado",
                            novedad.getFechaFinAusen(),
                            fechaFinPeriodo
                    )
            );
        }
    }

    private boolean esTipoHoras(String nombreConcepto) {
        return nombreConcepto.startsWith("Hora extra")
                || nombreConcepto.startsWith("Recargo");
    }

    private boolean esTipoDias(String nombreConcepto) {
        return nombreConcepto.contains("Incapacidad")
                || nombreConcepto.contains("Licencia")
                || nombreConcepto.contains("Vacaciones")
                || nombreConcepto.contains("permisos")
                || nombreConcepto.contains("transitorios")
                || nombreConcepto.contains("judiciales");
    }
}
