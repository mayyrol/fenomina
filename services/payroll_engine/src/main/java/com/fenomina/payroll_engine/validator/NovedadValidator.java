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
        validarFechasAusencia(novedad, fechaInicioPeriodo, fechaFinPeriodo,
                concepto.nombreConcepNomina());
    }

    public BigDecimal calcularExcesoNoSalarial(
            BigDecimal totalNoSalarial,
            BigDecimal totalSalarial
    ) {
        if (totalSalarial.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (totalNoSalarial.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal totalRemuneracion = totalSalarial.add(totalNoSalarial);
        BigDecimal limiteNoSalarial = totalRemuneracion
                .multiply(LIMITE_NO_SALARIAL)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal exceso = totalNoSalarial.subtract(limiteNoSalarial);
        return exceso.compareTo(BigDecimal.ZERO) > 0 ? exceso : BigDecimal.ZERO;
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
        String nombreConcepto = concepto.nombreConcepNomina();

        if ("Licencias no remuneradas".equals(nombreConcepto)) {
            validarDias(novedad, esQuincenal);
            return;
        }

        switch (concepto.categoriaConcNomina()) {
            case "DEVENGO", "PROVISION" -> {
                if (esTipoHoras(nombreConcepto)) {
                    validarHoras(novedad, nombreConcepto);
                } else if (esTipoDias(nombreConcepto)) {
                    if (esLicenciaLargaDuracion(nombreConcepto)) {
                        validarDiasLicenciaLarga(novedad);
                    } else {
                        validarDias(novedad, esQuincenal);
                    }
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
            LocalDate fechaFinPeriodo,
            String nombreConcepto
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

        if ("Vacaciones disfrutadas".equals(nombreConcepto)) {
            return;
        }

        if (novedad.getFechaInicioAusen() != null
                && novedad.getFechaInicioAusen().isBefore(fechaInicioPeriodo)) {
            throw new ValidacionNominaException(
                    "fechaInicioAusen",
                    String.format(
                            "La fecha inicio de ausencia (%s) es anterior al inicio del periodo (%s)",
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
                                    "Si el evento continúa en el siguiente periodo, debe registrarse por separado",
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

    private boolean esLicenciaLargaDuracion(String nombreConcepto) {
        return "Licencia de maternidad".equals(nombreConcepto)
                || "Licencia de paternidad".equals(nombreConcepto)
                || "Incapacidad por enfermedad general".equals(nombreConcepto)
                || "Incapacidad por origen laboral".equals(nombreConcepto);
    }

    private void validarDiasLicenciaLarga(Novedad novedad) {
        if (novedad.getCantidadDiasNovedad() == null
                || novedad.getCantidadDiasNovedad() <= 0) {
            throw new ValidacionNominaException(
                    "cantidadDiasNovedad",
                    "La cantidad de días debe ser mayor a cero"
            );
        }
        // Máximo razonable: 180 días (límite de incapacidad común / licencia maternidad extendida)
        if (novedad.getCantidadDiasNovedad() > 180) {
            throw new ValidacionNominaException(
                    "cantidadDiasNovedad",
                    String.format(
                            "La cantidad de días (%d) supera el máximo permitido para este tipo de novedad (180 días)",
                            novedad.getCantidadDiasNovedad()
                    )
            );
        }
    }
}
