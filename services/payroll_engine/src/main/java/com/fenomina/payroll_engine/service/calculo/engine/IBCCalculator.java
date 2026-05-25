package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.IBCCalculado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class IBCCalculator {

    private static final BigDecimal FACTOR_IBC_INTEGRAL = new BigDecimal("0.70");
    private static final BigDecimal FACTOR_IBC_INDEPENDIENTE = new BigDecimal("0.40");
    private static final int ESCALA = 2;

    public IBCCalculado calcular(ContextoLiquidacion ctx,
                                 BigDecimal totalDevengadoSalarial) {

        String tipoCotizante = ctx.getEmpleado().tipoCotizante();
        String subtipoCotizante = ctx.getEmpleado().subtipoCotizante();

        return switch (tipoCotizante) {
            case "01", "02", "19", "20", "44", "45" ->
                    calcularDependienteEstandar(ctx, totalDevengadoSalarial, subtipoCotizante);
            case "12" ->
                    calcularAprendizLectiva(ctx, totalDevengadoSalarial);
            case "23" ->
                    calcularEstudianteSoloArl(ctx);
            case "03" ->
                    calcularIndependiente(ctx, totalDevengadoSalarial, subtipoCotizante, false);
            case "59" ->
                    calcularIndependiente(ctx, totalDevengadoSalarial, subtipoCotizante, true);
            case "51" ->
                    calcularTiempoParcial(ctx, totalDevengadoSalarial, subtipoCotizante);
            default ->
                    calcularDependienteEstandar(ctx, totalDevengadoSalarial, subtipoCotizante);
        };
    }

    // --- Grupo 1: Dependiente estándar ---

    private IBCCalculado calcularDependienteEstandar(
            ContextoLiquidacion ctx,
            BigDecimal totalDevengadoSalarial,
            String subtipoCotizante
    ) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal topeIbc = ctx.getParametrosPorNombre()
                .get("TOPE_COTIZACION_IBC").valorParamGeneral();

        BigDecimal ibc;

        if (Boolean.TRUE.equals(ctx.getEmpleado().esSalarioIntegral())) {
            BigDecimal salarioIntegral = ctx.getEmpleado().salarioBascMensual();
            ibc = salarioIntegral.multiply(FACTOR_IBC_INTEGRAL)
                    .setScale(ESCALA, RoundingMode.HALF_UP);
        } else {
            ibc = totalDevengadoSalarial.setScale(ESCALA, RoundingMode.HALF_UP);
        }

        ibc = aplicarTopes(ibc, smmlv, topeIbc, ctx.getDiasLaboradosBrutos());

        boolean cotizaPension = cotizaPension(subtipoCotizante);
        boolean cotizaSalud = cotizaSalud(subtipoCotizante);

        return IBCCalculado.builder()
                .ibcSalud(cotizaSalud ? ibc : BigDecimal.ZERO)
                .ibcPension(cotizaPension ? ibc : BigDecimal.ZERO)
                .ibcArl(ibc)
                .ibcParafiscales(calcularBaseParafiscales(ctx, totalDevengadoSalarial))
                .cotizaSalud(cotizaSalud)
                .cotizaPension(cotizaPension)
                .cotizaArl(true)
                .build();
    }

    // --- Grupo 5: Aprendiz SENA lectiva ---

    private IBCCalculado calcularAprendizLectiva(
            ContextoLiquidacion ctx,
            BigDecimal totalDevengadoSalarial
    ) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal topeIbc = ctx.getParametrosPorNombre()
                .get("TOPE_COTIZACION_IBC").valorParamGeneral();

        BigDecimal ibc = aplicarTopes(
                totalDevengadoSalarial.setScale(ESCALA, RoundingMode.HALF_UP),
                smmlv, topeIbc, ctx.getDiasLaboradosBrutos());

        return IBCCalculado.builder()
                .ibcSalud(ibc)
                .ibcPension(BigDecimal.ZERO)
                .ibcArl(ibc)
                .ibcParafiscales(BigDecimal.ZERO)
                .cotizaSalud(true)
                .cotizaPension(false)
                .cotizaArl(true)
                .build();
    }

    // --- Grupo 6: Estudiante solo ARL ---

    private IBCCalculado calcularEstudianteSoloArl(ContextoLiquidacion ctx) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        return IBCCalculado.builder()
                .ibcSalud(BigDecimal.ZERO)
                .ibcPension(BigDecimal.ZERO)
                .ibcArl(smmlv)
                .ibcParafiscales(BigDecimal.ZERO)
                .cotizaSalud(false)
                .cotizaPension(false)
                .cotizaArl(true)
                .build();
    }

    // --- Grupos 7 y 8: Independiente ---

    private IBCCalculado calcularIndependiente(
            ContextoLiquidacion ctx,
            BigDecimal totalDevengadoSalarial,
            String subtipoCotizante,
            boolean arlObligatoria
    ) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal topeIbc = ctx.getParametrosPorNombre()
                .get("TOPE_COTIZACION_IBC").valorParamGeneral();

        BigDecimal ibc = totalDevengadoSalarial
                .multiply(FACTOR_IBC_INDEPENDIENTE)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        ibc = aplicarTopes(ibc, smmlv, topeIbc, ctx.getDiasLaboradosBrutos());

        boolean cotizaPension = cotizaPension(subtipoCotizante);

        // Subtipo 11: conductor taxi, ARL clase IV obligatoria
        boolean esArlObligatoria = arlObligatoria || "11".equals(subtipoCotizante);

        return IBCCalculado.builder()
                .ibcSalud(ibc)
                .ibcPension(cotizaPension ? ibc : BigDecimal.ZERO)
                .ibcArl(esArlObligatoria ? ibc : BigDecimal.ZERO)
                .ibcParafiscales(BigDecimal.ZERO)
                .cotizaSalud(true)
                .cotizaPension(cotizaPension)
                .cotizaArl(esArlObligatoria)
                .build();
    }

    // --- Grupo 9: Tiempo parcial ---

    private IBCCalculado calcularTiempoParcial(
            ContextoLiquidacion ctx,
            BigDecimal totalDevengadoSalarial,
            String subtipoCotizante
    ) {
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();

        BigDecimal topeIbc = ctx.getParametrosPorNombre()
                .get("TOPE_COTIZACION_IBC").valorParamGeneral();

        // IBC semanal según tabla Decreto 2616: aplica a pensión y CCF
        BigDecimal ibcProporcional = calcularIbcTiempoParcial(
                ctx.getDiasLaborados(), smmlv
        );

        ibcProporcional = ibcProporcional.min(topeIbc);

        boolean cotizaPension = cotizaPension(subtipoCotizante);

        BigDecimal baseParafiscales = calcularBaseParafiscales(ctx, totalDevengadoSalarial);

        return IBCCalculado.builder()
                .ibcSalud(ibcProporcional)
                .ibcPension(cotizaPension ? ibcProporcional : BigDecimal.ZERO)
                .ibcArl(smmlv) // Siempre 1 SMMLV completo para ARL tipo 51
                .ibcParafiscales(baseParafiscales)
                .cotizaSalud(true)
                .cotizaPension(cotizaPension)
                .cotizaArl(true)
                .build();
    }

    // --- Helpers ---

    private BigDecimal calcularIbcTiempoParcial(Integer diasLaborados, BigDecimal smmlv) {
        // Tabla Decreto 2616 de 2013
        BigDecimal fraccion;
        if (diasLaborados <= 7) {
            fraccion = new BigDecimal("0.25"); // 1/4 SMMLV
        } else if (diasLaborados <= 14) {
            fraccion = new BigDecimal("0.50"); // 2/4 SMMLV
        } else if (diasLaborados <= 21) {
            fraccion = new BigDecimal("0.75"); // 3/4 SMMLV
        } else {
            fraccion = BigDecimal.ONE; // SMMLV completo
        }
        return smmlv.multiply(fraccion).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private BigDecimal aplicarTopes(
            BigDecimal ibc,
            BigDecimal smmlv,
            BigDecimal topeIbc,
            Integer diasLaborados
    ) {
        // Si no laboró el mes completo, el mínimo se aplica proporcional
        BigDecimal minimoAplicable;
        if (diasLaborados != null && diasLaborados < 30) {
            minimoAplicable = smmlv
                    .multiply(BigDecimal.valueOf(diasLaborados))
                    .divide(BigDecimal.valueOf(30), ESCALA, RoundingMode.HALF_UP);
        } else {
            minimoAplicable = smmlv;
        }

        if (ibc.compareTo(minimoAplicable) < 0) {
            ibc = minimoAplicable;
        }

        if (ibc.compareTo(topeIbc) > 0) {
            ibc = topeIbc;
        }

        return ibc;
    }

    private BigDecimal calcularBaseParafiscales(
            ContextoLiquidacion ctx,
            BigDecimal totalDevengadoSalarial
    ) {
        // Base parafiscales = total devengado salarial, excluye auxilio de transporte
        // El auxilio de transporte nunca entra en la base de parafiscales
        // Para salario integral: 70% del salario integral
        if (Boolean.TRUE.equals(ctx.getEmpleado().esSalarioIntegral())) {
            return ctx.getEmpleado().salarioBascMensual()
                    .multiply(FACTOR_IBC_INTEGRAL)
                    .setScale(ESCALA, RoundingMode.HALF_UP);
        }

        return totalDevengadoSalarial.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private boolean cotizaPension(String subtipoCotizante) {
        if (subtipoCotizante == null) return true;
        return switch (subtipoCotizante) {
            case "1", "3", "4", "5", "9", "12" -> false;
            default -> true;
        };
    }

    private boolean cotizaSalud(String subtipoCotizante) {
        if (subtipoCotizante == null) return true;
        return !"9".equals(subtipoCotizante);
    }
}
