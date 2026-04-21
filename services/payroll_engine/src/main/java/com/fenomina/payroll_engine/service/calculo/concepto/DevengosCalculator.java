package com.fenomina.payroll_engine.service.calculo.concepto;

import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.DevengoCalculado;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.ContratoConceptoDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DevengosCalculator {

    private static final int ESCALA = 2;
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);
    private static final BigDecimal DIAS_ANIO_VACACIONES = BigDecimal.valueOf(720);
    private static final BigDecimal PORCENTAJE_INCAPACIDAD_COMUN = new BigDecimal("0.6666");
    private static final BigDecimal PORCENTAJE_INCAPACIDAD_LABORAL = BigDecimal.ONE;
    private static final int DIAS_EMPLEADOR_INCAPACIDAD_COMUN = 2;

    public List<DevengoCalculado> calcular(ContextoLiquidacion ctx) {
        List<DevengoCalculado> devengos = new ArrayList<>();

        devengos.add(calcularSalarioBase(ctx));

        DevengoCalculado auxTransporte = calcularAuxilioTransporte(ctx);
        if (auxTransporte != null) {
            devengos.add(auxTransporte);
        }

        for (Novedad novedad : ctx.getNovedades()) {
            ConceptoNominaDTO concepto = ctx.getConceptosPorId()
                    .get(novedad.getFkConcepNominaId());

            if (concepto == null) continue;

            DevengoCalculado devengo = resolverDevengoNovedad(ctx, novedad, concepto);
            if (devengo != null) {
                devengos.add(devengo);
            }
        }

        for (ContratoConceptoDTO conceptoFijo : ctx.getConceptosFijos()) {
            if (Boolean.TRUE.equals(conceptoFijo.esSalario())
                    && conceptoFijo.valorFijo() != null
                    && conceptoFijo.valorFijo().compareTo(BigDecimal.ZERO) > 0) {

                devengos.add(DevengoCalculado.builder()
                        .concepNominaId(conceptoFijo.conceptoNominaId())
                        .nombreConcepto(conceptoFijo.nombreConcepto())
                        .cantidad(null)
                        .baseCalculo(conceptoFijo.valorFijo())
                        .valorResultado(conceptoFijo.valorFijo())
                        .esSalario(true)
                        .esIbc(Boolean.TRUE.equals(conceptoFijo.esIbc()))
                        .esAuxilioTransporte(false)
                        .build());
            }
        }

        return devengos;
    }

    // --- Salario base ---

    private DevengoCalculado calcularSalarioBase(ContextoLiquidacion ctx) {
        BigDecimal salarioMensual = ctx.getEmpleado().salarioBascMensual();
        BigDecimal valorDia = salarioMensual.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valorLiquidado = valorDia
                .multiply(BigDecimal.valueOf(ctx.getDiasLaborados()))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(resolverIdConcepto(ctx, "Salario días trabajados"))
                .nombreConcepto("Salario días trabajados")
                .cantidad(ctx.getDiasLaborados())
                .baseCalculo(salarioMensual)
                .valorResultado(valorLiquidado)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .build();
    }

    // --- Auxilio de transporte ---

    private DevengoCalculado calcularAuxilioTransporte(ContextoLiquidacion ctx) {
        if (!Boolean.TRUE.equals(ctx.getEmpleado().tieneAuxTransporte())) {
            return null;
        }

        BigDecimal auxMensual = ctx.getParametrosPorNombre()
                .get("AUXILIO_TRANSPORTE").valorParamGeneral();

        BigDecimal valorDia = auxMensual.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valorLiquidado = valorDia
                .multiply(BigDecimal.valueOf(ctx.getDiasLaborados()))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(resolverIdConcepto(ctx, "Auxilio de transporte"))
                .nombreConcepto("Auxilio de transporte")
                .cantidad(ctx.getDiasLaborados())
                .baseCalculo(auxMensual)
                .valorResultado(valorLiquidado)
                .esSalario(false)
                .esIbc(false)
                .esAuxilioTransporte(true)
                .build();
    }

    // --- Resolución de novedades ---

    private DevengoCalculado resolverDevengoNovedad(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        String nombre = concepto.nombreConcepNomina();

        return switch (nombre) {
            case "Hora extra diurna lunes a sábado",
                 "Hora extra nocturna lunes a sábado",
                 "Hora extra diurna dominical o festivo",
                 "Hora extra nocturna dominical o festivo" ->
                    calcularHoraExtra(ctx, novedad, concepto, nombre);

            case "Recargo nocturno lunes a sábado",
                 "Recargo diurno domingo o festivo",
                 "Recargo nocturno domingo o festivo" ->
                    calcularRecargo(ctx, novedad, concepto, nombre);

            case "Incapacidad por enfermedad general" ->
                    calcularIncapacidadComun(ctx, novedad, concepto);

            case "Incapacidad por origen laboral" ->
                    calcularIncapacidadLaboral(ctx, novedad, concepto);

            case "Vacaciones disfrutadas" ->
                    calcularVacacionesDisfrutadas(ctx, novedad, concepto);

            case "Vacaciones compensadas en dinero" ->
                    calcularVacacionesCompensadas(ctx, novedad, concepto);

            case "Licencia de maternidad",
                 "Licencia de paternidad" ->
                    calcularLicenciaMaternidadPaternidad(ctx, novedad, concepto);

            case "Licencia por calamidad doméstica",
                 "Licencia por matrimonio",
                 "Licencia Ley ISAAC",
                 "Licencia por sufragio",
                 "Cargos transitorios",
                 "Citaciones judiciales",
                 "Otros permisos remunerados pactados" ->
                    calcularLicenciaRemunerada(ctx, novedad, concepto);

            case "Licencias no remuneradas" ->
                    null; // Informativo, no genera devengo

            case "Comisiones",
                 "Bonificaciones habituales",
                 "Bonificaciones ocasionales o por mera liberalidad",
                 "Beneficios o extralegales no salariales",
                 "Otro concepto a devenir salarial",
                 "Otro concepto a devenir no salarial",
                 "Viáticos permanentes manutención y alojamiento",
                 "Otros pagos que constituyen salario",
                 "Otros pagos que no constituyen salario permanente" ->
                    calcularConceptoValorFijo(novedad, concepto);

            default -> null;
        };
    }

    // --- Horas extra ---

    private DevengoCalculado calcularHoraExtra(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto,
            String nombre
    ) {
        BigDecimal valorHora = ctx.getParametrosPorNombre()
                .get("VALOR_HORA_ORDINARIA").valorParamGeneral();

        String claveParam = switch (nombre) {
            case "Hora extra diurna lunes a sábado" -> "EXTRA_DIURNA";
            case "Hora extra nocturna lunes a sábado" -> "EXTRA_NOCTURNA";
            case "Hora extra diurna dominical o festivo" -> "EXTRA_DIURNA_DOMINICAL";
            case "Hora extra nocturna dominical o festivo" -> "EXTRA_NOCTURNA_DOMINICAL";
            default -> throw new IllegalArgumentException("Hora extra no reconocida: " + nombre);
        };

        BigDecimal porcentaje = ctx.getParametrosPorNombre()
                .get(claveParam).porcentajeParamGeneral();

        BigDecimal horas = novedad.getCantidadHorasNovedad();
        BigDecimal valorExtra = valorHora
                .multiply(BigDecimal.ONE.add(porcentaje))
                .multiply(horas)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(nombre)
                .cantidad(null)
                .cantidadHoras(horas)
                .baseCalculo(valorHora)
                .valorResultado(valorExtra)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Recargos ---

    private DevengoCalculado calcularRecargo(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto,
            String nombre
    ) {
        BigDecimal valorHora = ctx.getParametrosPorNombre()
                .get("VALOR_HORA_ORDINARIA").valorParamGeneral();

        String claveParam = switch (nombre) {
            case "Recargo nocturno lunes a sábado" -> "RECARGO_NOCTURNO";
            case "Recargo diurno domingo o festivo" -> "RECARGO_DIURNO_DOMINICAL";
            case "Recargo nocturno domingo o festivo" -> "RECARGO_NOCTURNO_DOMINICAL";
            default -> throw new IllegalArgumentException("Recargo no reconocido: " + nombre);
        };

        BigDecimal porcentaje = ctx.getParametrosPorNombre()
                .get(claveParam).porcentajeParamGeneral();

        BigDecimal horas = novedad.getCantidadHorasNovedad();
        BigDecimal valorRecargo = valorHora
                .multiply(porcentaje)
                .multiply(horas)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(nombre)
                .cantidad(null)
                .cantidadHoras(horas)
                .baseCalculo(valorHora)
                .valorResultado(valorRecargo)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Incapacidad origen común ---

    private DevengoCalculado calcularIncapacidadComun(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        int diasTotales = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        // El empleador solo paga los primeros 2 días
        int diasEmpleador = Math.min(diasTotales, DIAS_EMPLEADOR_INCAPACIDAD_COMUN);

        if (diasEmpleador == 0) return null;

        // Base: IBC del periodo anterior
        BigDecimal ibcBase = ctx.getIbcSaludAnterior() != null
                ? ctx.getIbcSaludAnterior()
                : ctx.getEmpleado().salarioBascMensual();

        BigDecimal valorDia = ibcBase.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valorEmpleador = valorDia
                .multiply(BigDecimal.valueOf(diasEmpleador))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        int diasEps = diasTotales - diasEmpleador;

        String textoInformativo = diasEps > 0
                ? String.format("Empleador paga días 1-%d. EPS reconoce %d día(s) restante(s) " +
                        "al 66.66%% del IBL desde el día 3",
                diasEmpleador, diasEps)
                : null;

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(diasEmpleador)
                .baseCalculo(ibcBase)
                .valorResultado(valorEmpleador)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .textoInformativo(textoInformativo)
                .build();
    }

    // --- Incapacidad origen laboral ---

    private DevengoCalculado calcularIncapacidadLaboral(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        // ARL paga desde el día 1 al 100% del IBL
        // El empleador no paga ningún día, solo registra la novedad
        // Se registra como informativo en el desprendible
        int diasTotales = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal ibcBase = ctx.getIbcSaludAnterior() != null
                ? ctx.getIbcSaludAnterior()
                : ctx.getEmpleado().salarioBascMensual();

        BigDecimal valorDia = ibcBase.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valorArl = valorDia
                .multiply(PORCENTAJE_INCAPACIDAD_LABORAL)
                .multiply(BigDecimal.valueOf(diasTotales))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(diasTotales)
                .baseCalculo(ibcBase)
                .valorResultado(valorArl)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .esInformativo(true)
                .novedadId(novedad.getNovedadId())
                .textoInformativo(String.format(
                        "ARL reconoce %d día(s) al 100%% del IBL desde el día 1",
                        diasTotales))
                .build();
    }

    // --- Vacaciones disfrutadas ---

    private DevengoCalculado calcularVacacionesDisfrutadas(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        // Fórmula: salario * días / 720
        // Excluye horas extra, recargos dominicales y auxilio de transporte
        BigDecimal salario = ctx.getEmpleado().salarioBascMensual();
        int dias = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal valor = salario
                .multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_ANIO_VACACIONES, ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(salario)
                .valorResultado(valor)
                .esSalario(false)
                .esIbc(false)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Vacaciones compensadas en dinero ---

    private DevengoCalculado calcularVacacionesCompensadas(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        BigDecimal salario = ctx.getEmpleado().salarioBascMensual();
        int dias = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal valor = salario
                .multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_ANIO_VACACIONES, ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(salario)
                .valorResultado(valor)
                .esSalario(false)
                .esIbc(false)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Licencia maternidad y paternidad ---

    private DevengoCalculado calcularLicenciaMaternidadPaternidad(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        // Base: IBC del periodo anterior o salario actual si no hay histórico
        BigDecimal ibcBase = ctx.getIbcSaludAnterior() != null
                ? ctx.getIbcSaludAnterior()
                : ctx.getEmpleado().salarioBascMensual();

        int dias = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal valorDia = ibcBase.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valor = valorDia
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(ibcBase)
                .valorResultado(valor)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .textoInformativo("Valor reconocido por EPS al 100% del IBC. " +
                        "No aplica auxilio de transporte ni parafiscales")
                .build();
    }

    // --- Licencias remuneradas generales ---

    private DevengoCalculado calcularLicenciaRemunerada(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        // Base: último IBC reportado antes del inicio de la licencia
        BigDecimal ibcBase = ctx.getIbcSaludAnterior() != null
                ? ctx.getIbcSaludAnterior()
                : ctx.getEmpleado().salarioBascMensual();

        int dias = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal valorDia = ibcBase.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valor = valorDia
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(ibcBase)
                .valorResultado(valor)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Conceptos de valor fijo ---

    private DevengoCalculado calcularConceptoValorFijo(
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        BigDecimal valor = novedad.getValorRefNovedad() != null
                ? novedad.getValorRefNovedad()
                : BigDecimal.ZERO;

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(null)
                .baseCalculo(valor)
                .valorResultado(valor)
                .esSalario(concepto.esSalario())
                .esIbc(concepto.esIbc())
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .build();
    }

    // --- Helper para resolver id de concepto por nombre ---

    private Long resolverIdConcepto(ContextoLiquidacion ctx, String nombre) {
        return ctx.getConceptosPorId().values().stream()
                .filter(c -> c.nombreConcepNomina().equals(nombre))
                .map(ConceptoNominaDTO::concepNominaId)
                .findFirst()
                .orElse(null);
    }
}
