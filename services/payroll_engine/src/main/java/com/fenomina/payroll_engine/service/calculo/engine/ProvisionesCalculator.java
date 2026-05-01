package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.DevengoCalculado;
import com.fenomina.payroll_engine.domain.vo.ProvisionCalculada;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProvisionesCalculator {

    private static final int ESCALA = 2;
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);

    public List<ProvisionCalculada> calcular(
            ContextoLiquidacion ctx,
            List<DevengoCalculado> devengos
    ) {
        List<ProvisionCalculada> provisiones = new ArrayList<>();

        // Salario integral no genera provisiones ordinarias
        // ya están incluidas en el factor prestacional del 30%
        if (Boolean.TRUE.equals(ctx.getEmpleado().esSalarioIntegral())) {
            return provisiones;
        }

        BigDecimal basePrima = calcularBasePrima(ctx, devengos);


        provisiones.add(calcularPrima(ctx, basePrima));
        provisiones.add(calcularCesantias(ctx, basePrima));
        provisiones.add(calcularInteresesCesantias(ctx, basePrima));

        return provisiones;
    }

    // --- Bases de cálculo ---

    private BigDecimal calcularBasePrima(
            ContextoLiquidacion ctx,
            List<DevengoCalculado> devengos
    ) {
        // Base prima y cesantías:
        // salario básico + auxilio de transporte + comisiones +
        // horas extra + recargos + otros devengos salariales
        // Excluye: bonificaciones ocasionales, beneficios no salariales,
        // vacaciones compensadas
        return devengos.stream()
                .filter(d -> !d.isEsInformativo())
                .filter(d -> d.isEsSalario() || d.isEsAuxilioTransporte())
                .map(DevengoCalculado::getValorResultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, RoundingMode.HALF_UP);
    }


    // --- Provisiones ---

    private ProvisionCalculada calcularPrima(
            ContextoLiquidacion ctx,
            BigDecimal basePrima
    ) {
        BigDecimal porcentajePrima = ctx.getParametrosPorNombre()
                .get("PRIMA_SERVICIOS").porcentajeParamGeneral();

        BigDecimal valorPrima = basePrima
                .multiply(porcentajePrima)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return ProvisionCalculada.builder()
                .nombreConcepto("Prima de servicios")
                .baseCalculo(basePrima)
                .porcentaje(porcentajePrima)
                .valorResultado(valorPrima)
                .esInformativo(false)
                .build();
    }

    private ProvisionCalculada calcularCesantias(
            ContextoLiquidacion ctx,
            BigDecimal basePrima
    ) {
        BigDecimal porcentajeCesantias = ctx.getParametrosPorNombre()
                .get("CESANTIAS").porcentajeParamGeneral();

        BigDecimal valorCesantias = basePrima
                .multiply(porcentajeCesantias)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return ProvisionCalculada.builder()
                .nombreConcepto("Cesantías")
                .baseCalculo(basePrima)
                .porcentaje(porcentajeCesantias)
                .valorResultado(valorCesantias)
                .esInformativo(true)
                .textoInformativo("Valor informativo. Se consigna al fondo antes del 14 de febrero")
                .build();
    }

    private ProvisionCalculada calcularInteresesCesantias(
            ContextoLiquidacion ctx,
            BigDecimal basePrima
    ) {
        BigDecimal porcentajeCesantias = ctx.getParametrosPorNombre()
                .get("CESANTIAS").porcentajeParamGeneral();

        BigDecimal porcentajeIntereses = ctx.getParametrosPorNombre()
                .get("INTERESES_CESANTIAS").porcentajeParamGeneral();

        // Intereses = cesantías del periodo * 1% mensual
        BigDecimal cesantiasPeriodo = basePrima
                .multiply(porcentajeCesantias)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal valorIntereses = cesantiasPeriodo
                .multiply(porcentajeIntereses)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return ProvisionCalculada.builder()
                .nombreConcepto("Intereses sobre las cesantías")
                .baseCalculo(cesantiasPeriodo)
                .porcentaje(porcentajeIntereses)
                .valorResultado(valorIntereses)
                .esInformativo(false)
                .build();
    }

    // --- Helpers de filtrado ---

    private boolean esVacacionesCompensadas(String nombreConcepto) {
        return "Vacaciones compensadas en dinero".equals(nombreConcepto);
    }

    private boolean esSalarioBaseVacaciones(String nombreConcepto) {
        return switch (nombreConcepto) {
            case "Salario días trabajados",
                 "Recargo nocturno lunes a sábado" -> true;
            default -> false;
        };
    }
}
