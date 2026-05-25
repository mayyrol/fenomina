package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.DeduccionCalculada;
import com.fenomina.payroll_engine.domain.vo.IBCCalculado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeduccionesCalculator {

    private static final int ESCALA = 2;
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);

    public List<DeduccionCalculada> calcular(
            ContextoLiquidacion ctx,
            IBCCalculado ibc
    ) {
        List<DeduccionCalculada> deducciones = new ArrayList<>();

        String subtipo = ctx.getEmpleado().subtipoCotizante();

        // --- Salud empleado ---
        if (ibc.isCotizaSalud()) {

            BigDecimal porcentajeSaludEmp = ctx.getParametrosPorNombre()
                    .get("SALUD_EMPLEADO").porcentajeParamGeneral();

            BigDecimal valorSaludEmp = ibc.getIbcSalud()
                    .multiply(porcentajeSaludEmp)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            deducciones.add(DeduccionCalculada.builder()
                    .nombreConcepto("Salud empleado")
                    .baseCalculo(ibc.getIbcSalud())
                    .porcentaje(porcentajeSaludEmp)
                    .valorResultado(valorSaludEmp)
                    .esAporteLicenciaNoRemunerada(false)
                    .build());
        }

        // --- Pensión empleado días trabajados ---
        if (ibc.isCotizaPension()) {

            BigDecimal porcentajePensionEmp = ctx.getParametrosPorNombre()
                    .get("PENSION_EMPLEADO").porcentajeParamGeneral();

            BigDecimal valorPensionEmp = ibc.getIbcPension()
                    .multiply(porcentajePensionEmp)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            deducciones.add(DeduccionCalculada.builder()
                    .nombreConcepto("Pensión empleado")
                    .baseCalculo(ibc.getIbcPension())
                    .porcentaje(porcentajePensionEmp)
                    .valorResultado(valorPensionEmp)
                    .esAporteLicenciaNoRemunerada(false)
                    .build());
        }

        // --- Aportes por días de licencia no remunerada ---
        if (ctx.getDiasLicenciaNoRemunerada() != null
                && ctx.getDiasLicenciaNoRemunerada() > 0) {

            deducciones.addAll(calcularAportesLicenciaNoRemunerada(ctx, ibc));
        }

        // --- Fondo de solidaridad pensional ---
        if (ibc.isCotizaPension()) {
            BigDecimal fspPorcentaje = calcularPorcentajeFsp(ctx, ibc.getIbcPension());

            if (fspPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal valorFsp = ibc.getIbcPension()
                        .multiply(fspPorcentaje)
                        .setScale(ESCALA, RoundingMode.HALF_UP);

                deducciones.add(DeduccionCalculada.builder()
                        .nombreConcepto("Aporte fondo de solidaridad pensional empleado")
                        .baseCalculo(ibc.getIbcPension())
                        .porcentaje(fspPorcentaje)
                        .valorResultado(valorFsp)
                        .esAporteLicenciaNoRemunerada(false)
                        .build());
            }
        }

        // Subtipo 6: cotiza FSP sobre IBC salud SOLO si no cotiza pensión
        // (evita duplicar la deducción cuando ibc.isCotizaPension() ya lo calculó)
        if ("6".equals(subtipo) && !ibc.isCotizaPension()) {
            BigDecimal smmlv = ctx.getParametrosPorNombre()
                    .get("SMMLV").valorParamGeneral();
            BigDecimal cuatroSmmlv = smmlv.multiply(BigDecimal.valueOf(4));
            if (ibc.getIbcSalud().compareTo(cuatroSmmlv) >= 0) {
                BigDecimal fspPorcentaje = calcularPorcentajeFsp(ctx, ibc.getIbcSalud());
                if (fspPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal valorFsp = ibc.getIbcSalud()
                            .multiply(fspPorcentaje)
                            .setScale(ESCALA, RoundingMode.HALF_UP);
                    deducciones.add(DeduccionCalculada.builder()
                            .nombreConcepto("Aporte fondo de solidaridad pensional empleado")
                            .baseCalculo(ibc.getIbcSalud())
                            .porcentaje(fspPorcentaje)
                            .valorResultado(valorFsp)
                            .esAporteLicenciaNoRemunerada(false)
                            .build());
                }
            }
        }

        // --- Retención en la fuente ---
        deducciones.addAll(calcularDeduccionesNovedades(ctx));

        return deducciones;
    }

    // --- Aportes licencia no remunerada ---

    private List<DeduccionCalculada> calcularAportesLicenciaNoRemunerada(
            ContextoLiquidacion ctx,
            IBCCalculado ibc
    ) {
        List<DeduccionCalculada> aportes = new ArrayList<>();

        int diasLnr = ctx.getDiasLicenciaNoRemunerada();

        // IBC anterior proporcional a días de licencia
        BigDecimal ibcSaludLnr = ctx.getIbcSaludAnterior() != null
                ? calcularIbcProporcional(ctx.getIbcSaludAnterior(), diasLnr)
                : calcularIbcProporcional(ibc.getIbcSalud(), diasLnr);

        BigDecimal ibcPensionLnr = ctx.getIbcPensionAnterior() != null
                ? calcularIbcProporcional(ctx.getIbcPensionAnterior(), diasLnr)
                : calcularIbcProporcional(ibc.getIbcPension(), diasLnr);

        // Salud: solo empleador adelanta, empleado no aporta
        // Se registra como informativo en deducciones del empleado
        // El cargo real va a SeguridadSocialCalculator como aporte patronal

        // Pensión empleado adelantada por empleador en LNR
        if (ibc.isCotizaPension()) {
            BigDecimal porcentajePensionEmp = ctx.getParametrosPorNombre()
                    .get("PENSION_EMPLEADO").porcentajeParamGeneral();

            BigDecimal valorPensionLnr = ibcPensionLnr
                    .multiply(porcentajePensionEmp)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            aportes.add(DeduccionCalculada.builder()
                    .nombreConcepto("Pensión empleado")
                    .baseCalculo(ibcPensionLnr)
                    .porcentaje(porcentajePensionEmp)
                    .valorResultado(valorPensionLnr)
                    .esAporteLicenciaNoRemunerada(true)
                    .textoInformativo(String.format(
                            "Aporte pensión por %d día(s) de licencia no remunerada. " +
                                    "Empleador adelanta y recupera en siguiente periodo con devengado disponible",
                            diasLnr))
                    .build());
        }

        return aportes;
    }

    // --- Deducciones desde novedades ---

    private List<DeduccionCalculada> calcularDeduccionesNovedades(ContextoLiquidacion ctx) {
        List<DeduccionCalculada> deducciones = new ArrayList<>();

        for (var novedad : ctx.getNovedades()) {
            var concepto = ctx.getConceptosPorId().get(novedad.getFkConcepNominaId());
            if (concepto == null) continue;

            String nombre = concepto.nombreConcepNomina();

            switch (nombre) {
                case "Retención en la fuente" -> {
                    if (novedad.getValorRefNovedad() != null
                            && novedad.getValorRefNovedad().compareTo(BigDecimal.ZERO) > 0) {

                        deducciones.add(DeduccionCalculada.builder()
                                .nombreConcepto("Retención en la fuente")
                                .baseCalculo(novedad.getValorRefNovedad())
                                .porcentaje(null)
                                .valorResultado(novedad.getValorRefNovedad())
                                .esAporteLicenciaNoRemunerada(false)
                                .novedadId(novedad.getNovedadId())
                                .build());
                    }
                }
                case "Otros conceptos a deducir salariales",
                     "Otros conceptos a deducir no salariales" -> {
                    if (novedad.getValorRefNovedad() != null
                            && novedad.getValorRefNovedad().compareTo(BigDecimal.ZERO) > 0) {

                        deducciones.add(DeduccionCalculada.builder()
                                .nombreConcepto(nombre)
                                .baseCalculo(novedad.getValorRefNovedad())
                                .porcentaje(null)
                                .valorResultado(novedad.getValorRefNovedad())
                                .esAporteLicenciaNoRemunerada(false)
                                .novedadId(novedad.getNovedadId())
                                .build());
                    }
                }
                default -> { }
            }
        }

        return deducciones;
    }

    // --- Helpers ---

    private BigDecimal calcularIbcProporcional(BigDecimal ibc, int dias) {
        return ibc
                .multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPorcentajeFsp(ContextoLiquidacion ctx, BigDecimal ibc) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal cuatroSmmlv = smmlv.multiply(BigDecimal.valueOf(4));

        if (ibc.compareTo(cuatroSmmlv) < 0) {
            return BigDecimal.ZERO;
        }

        if (ibc.compareTo(smmlv.multiply(BigDecimal.valueOf(16))) < 0) {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_1").porcentajeParamGeneral();
        } else if (ibc.compareTo(smmlv.multiply(BigDecimal.valueOf(17))) < 0) {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_2").porcentajeParamGeneral();
        } else if (ibc.compareTo(smmlv.multiply(BigDecimal.valueOf(18))) < 0) {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_3").porcentajeParamGeneral();
        } else if (ibc.compareTo(smmlv.multiply(BigDecimal.valueOf(19))) < 0) {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_4").porcentajeParamGeneral();
        } else if (ibc.compareTo(smmlv.multiply(BigDecimal.valueOf(20))) < 0) {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_5").porcentajeParamGeneral();
        } else {
            return ctx.getParametrosPorNombre()
                    .get("FONDO_SOLIDARIDAD_PENSIONAL_6").porcentajeParamGeneral();
        }
    }
}
