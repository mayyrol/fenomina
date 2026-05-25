package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.domain.vo.AportePatronalCalculado;
import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.IBCCalculado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeguridadSocialCalculator {

    private static final int ESCALA = 2;
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);
    private static final BigDecimal DIEZ_SMMLV = BigDecimal.valueOf(10);

    public List<AportePatronalCalculado> calcular(
            ContextoLiquidacion ctx,
            IBCCalculado ibc,
            BigDecimal totalDevengadoSalarial
    ) {
        List<AportePatronalCalculado> aportes = new ArrayList<>();

        String tipoCotizante = ctx.getEmpleado().tipoCotizante();
        String subtipoCotizante = ctx.getEmpleado().subtipoCotizante();
        boolean exoneradaParafiscales = Boolean.TRUE
                .equals(ctx.getEsEmpresaExoneradaParafiscales());

        // --- Salud empleador ---
        aportes.addAll(calcularAporteSalud(ctx, ibc, tipoCotizante,
                subtipoCotizante, exoneradaParafiscales, totalDevengadoSalarial));

        // --- Pensión empleador ---
        aportes.addAll(calcularAportePension(ctx, ibc, subtipoCotizante));

        // --- ARL ---
        aportes.addAll(calcularAporteArl(ctx, ibc, tipoCotizante));

        // --- Parafiscales ---
        aportes.addAll(calcularAportesParafiscales(ctx, ibc, tipoCotizante,
                subtipoCotizante, exoneradaParafiscales, totalDevengadoSalarial));

        return aportes;
    }

    // --- Salud empleador ---

    private List<AportePatronalCalculado> calcularAporteSalud(
            ContextoLiquidacion ctx,
            IBCCalculado ibc,
            String tipoCotizante,
            String subtipoCotizante,
            boolean exoneradaParafiscales,
            BigDecimal totalDevengadoSalarial
    ) {
        List<AportePatronalCalculado> aportes = new ArrayList<>();

        // Grupo 6 (subtipo 6): salud va a subcuenta especial
        // Grupo 3 (subtipo 9): no cotiza salud
        // Tipo 23: no cotiza salud
        if ("9".equals(subtipoCotizante) || "23".equals(tipoCotizante)) {
            return aportes;
        }

        if (!ibc.isCotizaSalud()) return aportes;

        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal porcentajeSaludEmp = ctx.getParametrosPorNombre()
                .get("SALUD_EMPLEADOR").porcentajeParamGeneral();

        // La ley habla de lo que el trabajador "devengue", no del salario base contractual
        boolean exoneradoSalud = exoneradaParafiscales
                && totalDevengadoSalarial.compareTo(smmlv.multiply(DIEZ_SMMLV)) < 0;


        if (!exoneradoSalud) {
            BigDecimal valorSaludEmpleador = ibc.getIbcSalud()
                    .multiply(porcentajeSaludEmp)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            aportes.add(AportePatronalCalculado.builder()
                    .nombreConcepto("Aporte salud empleador")
                    .baseCalculo(ibc.getIbcSalud())
                    .porcentaje(porcentajeSaludEmp)
                    .valorResultado(valorSaludEmpleador)
                    .esAporteLicenciaNoRemunerada(false)
                    .build());
        }

        // Salud empleador por días de licencia no remunerada
        if (ctx.getDiasLicenciaNoRemunerada() != null
                && ctx.getDiasLicenciaNoRemunerada() > 0) {

            BigDecimal ibcSaludLnr = ctx.getIbcSaludAnterior() != null
                    ? calcularIbcProporcional(
                    ctx.getIbcSaludAnterior(),
                    ctx.getDiasLicenciaNoRemunerada())
                    : calcularIbcProporcional(
                    ibc.getIbcSalud(),
                    ctx.getDiasLicenciaNoRemunerada());

            if (!exoneradoSalud) {
                BigDecimal valorSaludLnr = ibcSaludLnr
                        .multiply(porcentajeSaludEmp)
                        .setScale(ESCALA, RoundingMode.HALF_UP);

                aportes.add(AportePatronalCalculado.builder()
                        .nombreConcepto("Aporte salud empleador")
                        .baseCalculo(ibcSaludLnr)
                        .porcentaje(porcentajeSaludEmp)
                        .valorResultado(valorSaludLnr)
                        .esAporteLicenciaNoRemunerada(true)
                        .textoInformativo(String.format(
                                "Aporte salud empleador por %d día(s) de licencia no remunerada. " +
                                        "Solo empleador aporta el 8.5%%",
                                ctx.getDiasLicenciaNoRemunerada()))
                        .build());
            }
        }

        return aportes;
    }

    // --- Pensión empleador ---

    private List<AportePatronalCalculado> calcularAportePension(
            ContextoLiquidacion ctx,
            IBCCalculado ibc,
            String subtipoCotizante
    ) {
        List<AportePatronalCalculado> aportes = new ArrayList<>();

        if (!ibc.isCotizaPension()) return aportes;

        BigDecimal porcentajePensionEmp = ctx.getParametrosPorNombre()
                .get("PENSION_EMPLEADOR").porcentajeParamGeneral();


        BigDecimal valorPensionEmpleador = ibc.getIbcPension()
                .multiply(porcentajePensionEmp)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        aportes.add(AportePatronalCalculado.builder()
                .nombreConcepto("Pensión empleador")
                .baseCalculo(ibc.getIbcPension())
                .porcentaje(porcentajePensionEmp)
                .valorResultado(valorPensionEmpleador)
                .esAporteLicenciaNoRemunerada(false)
                .build());

        // Pensión empleador por días de licencia no remunerada
        if (ctx.getDiasLicenciaNoRemunerada() != null
                && ctx.getDiasLicenciaNoRemunerada() > 0) {

            BigDecimal ibcPensionLnr = ctx.getIbcPensionAnterior() != null
                    ? calcularIbcProporcional(
                    ctx.getIbcPensionAnterior(),
                    ctx.getDiasLicenciaNoRemunerada())
                    : calcularIbcProporcional(
                    ibc.getIbcPension(),
                    ctx.getDiasLicenciaNoRemunerada());

            BigDecimal valorPensionLnr = ibcPensionLnr
                    .multiply(porcentajePensionEmp)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            aportes.add(AportePatronalCalculado.builder()
                    .nombreConcepto("Pensión empleador")
                    .baseCalculo(ibcPensionLnr)
                    .porcentaje(porcentajePensionEmp)
                    .valorResultado(valorPensionLnr)
                    .esAporteLicenciaNoRemunerada(true)
                    .textoInformativo(String.format(
                            "Aporte pensión empleador por %d día(s) de licencia no remunerada. " +
                                    "Empleador adelanta también el 4%% del empleado",
                            ctx.getDiasLicenciaNoRemunerada()))
                    .build());
        }

        return aportes;
    }

    // --- ARL ---

    private List<AportePatronalCalculado> calcularAporteArl(
            ContextoLiquidacion ctx,
            IBCCalculado ibc,
            String tipoCotizante
    ) {
        List<AportePatronalCalculado> aportes = new ArrayList<>();

        if (!ibc.isCotizaArl()) return aportes;

        String claseRiesgo = ctx.getEmpleado().claseRiesgo();

        String claveArl = switch (claseRiesgo.toUpperCase()) {
            case "I", "1", "CLASE_I" -> "ARL_EMPLEADOR_I";
            case "II", "2", "CLASE_II" -> "ARL_EMPLEADOR_II";
            case "III", "3", "CLASE_III" -> "ARL_EMPLEADOR_III";
            case "IV", "4", "CLASE_IV" -> "ARL_EMPLEADOR_IV";
            case "V", "5", "CLASE_V" -> "ARL_EMPLEADOR_V";
            default -> throw new com.fenomina.payroll_engine.exception.ParametroNoEncontradoException(
                    String.format("Clase de riesgo ARL no reconocida: %s", claseRiesgo)
            );
        };

        BigDecimal porcentajeArl = ctx.getParametrosPorNombre()
                .get(claveArl).porcentajeParamGeneral();

        // Tipo 23: ARL siempre sobre 1 SMMLV
        // Tipo 51: ARL siempre sobre 1 SMMLV por 30 días completos
        BigDecimal baseArl;
        if ("23".equals(tipoCotizante) || "51".equals(tipoCotizante)) {
            baseArl = ctx.getParametrosPorNombre()
                    .get("SMMLV").valorParamGeneral();
        } else {
            baseArl = ibc.getIbcArl();
        }

        BigDecimal valorArl = baseArl
                .multiply(porcentajeArl)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        aportes.add(AportePatronalCalculado.builder()
                .nombreConcepto("ARL empleador")
                .baseCalculo(baseArl)
                .porcentaje(porcentajeArl)
                .valorResultado(valorArl)
                .esAporteLicenciaNoRemunerada(false)
                .build());

        return aportes;
    }

    // --- Parafiscales ---

    private List<AportePatronalCalculado> calcularAportesParafiscales(
            ContextoLiquidacion ctx,
            IBCCalculado ibc,
            String tipoCotizante,
            String subtipoCotizante,
            boolean exoneradaParafiscales,
            BigDecimal totalDevengadoSalarial
    ) {
        List<AportePatronalCalculado> aportes = new ArrayList<>();

        // Tipos sin parafiscales: independientes, aprendiz lectiva,
        // estudiante solo ARL, licencia mat/pat
        if ("03".equals(tipoCotizante)
                || "59".equals(tipoCotizante)
                || "12".equals(tipoCotizante)
                || "23".equals(tipoCotizante)) {
            return aportes;
        }

        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal baseParafiscales = ibc.getIbcParafiscales();

        boolean exoneradoSenaIcbf = exoneradaParafiscales
                && totalDevengadoSalarial.compareTo(smmlv.multiply(DIEZ_SMMLV)) < 0;

        // Tipo 20 (estudiante Ley 789): exento de SENA e ICBF por ley
        boolean estudianteLey789 = "20".equals(tipoCotizante);

        // CCF: siempre aplica para dependientes, nunca exonerado
        BigDecimal porcentajeCcf = ctx.getParametrosPorNombre()
                .get("CAJA_COMPENSACION").porcentajeParamGeneral();

        BigDecimal valorCcf = baseParafiscales
                .multiply(porcentajeCcf)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        aportes.add(AportePatronalCalculado.builder()
                .nombreConcepto("Caja de compensación empleador")
                .baseCalculo(baseParafiscales)
                .porcentaje(porcentajeCcf)
                .valorResultado(valorCcf)
                .esAporteLicenciaNoRemunerada(false)
                .build());

        // SENA
        if (!exoneradoSenaIcbf && !estudianteLey789) {
            BigDecimal porcentajeSena = ctx.getParametrosPorNombre()
                    .get("SENA").porcentajeParamGeneral();

            BigDecimal valorSena = baseParafiscales
                    .multiply(porcentajeSena)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            aportes.add(AportePatronalCalculado.builder()
                    .nombreConcepto("SENA empleador")
                    .baseCalculo(baseParafiscales)
                    .porcentaje(porcentajeSena)
                    .valorResultado(valorSena)
                    .esAporteLicenciaNoRemunerada(false)
                    .build());
        }

        // ICBF
        if (!exoneradoSenaIcbf && !estudianteLey789) {
            BigDecimal porcentajeIcbf = ctx.getParametrosPorNombre()
                    .get("ICBF").porcentajeParamGeneral();

            BigDecimal valorIcbf = baseParafiscales
                    .multiply(porcentajeIcbf)
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            aportes.add(AportePatronalCalculado.builder()
                    .nombreConcepto("ICBF empleador")
                    .baseCalculo(baseParafiscales)
                    .porcentaje(porcentajeIcbf)
                    .valorResultado(valorIcbf)
                    .esAporteLicenciaNoRemunerada(false)
                    .build());
        }

        return aportes;
    }

    // --- Helper ---

    private BigDecimal calcularIbcProporcional(BigDecimal ibc, int dias) {
        return ibc
                .multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
    }
}
