package com.fenomina.payroll_engine.service.calculo.concepto;

import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.domain.vo.ContextoLiquidacion;
import com.fenomina.payroll_engine.domain.vo.DevengoCalculado;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.ContratoConceptoDTO;
import com.fenomina.payroll_engine.repository.ReporteNominaDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DevengosCalculator {

    private static final int ESCALA = 2;
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);
    private static final BigDecimal PORCENTAJE_INCAPACIDAD_LABORAL = BigDecimal.ONE;
    private static final BigDecimal PORCENTAJE_66 = new BigDecimal("0.6667");
    private static final BigDecimal PORCENTAJE_50 = new BigDecimal("0.50");
    private static final int DIAS_EMPLEADOR = 2;
    private static final int DIAS_TOPE_EPS = 90;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;


    private BigDecimal calcularValorHoraOrdinaria(ContextoLiquidacion ctx) {
        BigDecimal horasMes = ctx.getParametrosPorNombre()
                .get("HORAS_TRABAJADAS_MES").valorParamGeneral();
        BigDecimal valorCalculado = ctx.getEmpleado().salarioBascMensual()
                .divide(horasMes, ESCALA, RoundingMode.HALF_UP);

        // El valor hora nunca puede ser inferior al mínimo legal
        BigDecimal valorMinimo = ctx.getParametrosPorNombre()
                .get("VALOR_HORA_ORDINARIA").valorParamGeneral();

        return valorCalculado.max(valorMinimo);
    }

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

            String nombre = concepto.nombreConcepNomina();

            // Incapacidad común puede retornar múltiples devengos (desglose 66/50)
            if ("Incapacidad por enfermedad general".equals(nombre)) {
                devengos.addAll(calcularIncapacidadComun(ctx, novedad, concepto));
            } else {
                DevengoCalculado devengo = resolverDevengoNovedad(ctx, novedad, concepto);
                if (devengo != null) {
                    devengos.add(devengo);
                }
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
            case "Hora extra diurna ordinaria",
                 "Hora extra nocturna ordinaria",
                 "Hora extra diurna dominical o festiva",
                 "Hora extra nocturna dominical o festiva" ->
                    calcularHoraExtra(ctx, novedad, concepto, nombre);

            case "Recargo nocturno ordinario",
                 "Recargo diurno dominical o festivo",
                 "Recargo nocturno dominical o festivo" ->
                    calcularRecargo(ctx, novedad, concepto, nombre);

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
        BigDecimal valorHora = calcularValorHoraOrdinaria(ctx);

        String claveParam = switch (nombre) {
            case "Hora extra diurna ordinaria" -> "EXTRA_DIURNA";
            case "Hora extra nocturna ordinaria" -> "EXTRA_NOCTURNA";
            case "Hora extra diurna dominical o festiva" -> "EXTRA_DIURNA_DOMINICAL";
            case "Hora extra nocturna dominical o festiva" -> "EXTRA_NOCTURNA_DOMINICAL";
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
        BigDecimal valorHora = calcularValorHoraOrdinaria(ctx);

        String claveParam = switch (nombre) {
            case "Recargo nocturno ordinario" -> "RECARGO_NOCTURNO";
            case "Recargo diurno dominical o festivo" -> "RECARGO_DIURNO_DOMINICAL";
            case "Recargo nocturno dominical o festivo" -> "RECARGO_NOCTURNO_DOMINICAL";
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

    private List<DevengoCalculado> calcularIncapacidadComun(
            ContextoLiquidacion ctx,
            Novedad novedad,
            ConceptoNominaDTO concepto
    ) {
        List<DevengoCalculado> resultado = new ArrayList<>();

        int diasIngresados = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        if (diasIngresados == 0) return resultado;

        BigDecimal salario = ctx.getEmpleado().salarioBascMensual();
        BigDecimal smmlv = ctx.getParametrosPorNombre()
                .get("SMMLV").valorParamGeneral();
        BigDecimal valorDiaSmmlv = smmlv
                .divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);

        // --- Caso 1: exactamente 2 días, empleador al 100% ---
        if (diasIngresados <= DIAS_EMPLEADOR) {
            BigDecimal valorDia = salario.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
            BigDecimal valor = valorDia
                    .multiply(BigDecimal.valueOf(diasIngresados))
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            resultado.add(DevengoCalculado.builder()
                    .concepNominaId(concepto.concepNominaId())
                    .nombreConcepto(concepto.nombreConcepNomina())
                    .cantidad(diasIngresados)
                    .baseCalculo(salario)
                    .valorResultado(valor)
                    .esSalario(true)
                    .esIbc(true)
                    .esAuxilioTransporte(false)
                    .novedadId(novedad.getNovedadId())
                    .textoInformativo("Días 1-2 a cargo del empleador al 100% del salario")
                    .build());

            return resultado;
        }

        // --- Caso 2: más de 2 días ---
        // Calcular valor día con piso SMMLV para tramo 66.67%
        BigDecimal valorDia66 = salario
                .divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP)
                .multiply(PORCENTAJE_66)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal valorDiaFinal66 = valorDia66.max(valorDiaSmmlv);

        // Calcular valor día con piso SMMLV para tramo 50%
        BigDecimal valorDia50 = salario
                .divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP)
                .multiply(PORCENTAJE_50)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal valorDiaFinal50 = valorDia50.max(valorDiaSmmlv);

        // Consultar días acumulados de períodos anteriores
        Long concepId = concepto.concepNominaId();
        int diasAcumulados = obtenerDiasAcumulados(
                ctx, concepId);

        // Determinar desglose
        if (diasAcumulados >= DIAS_TOPE_EPS) {
            // Todo al 50%
            BigDecimal valor = valorDiaFinal50
                    .multiply(BigDecimal.valueOf(diasIngresados))
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            resultado.add(construirDevengoIncapacidad(
                    concepto, novedad, diasIngresados, salario, valor,
                    "Incapacidad por enfermedad general (50%)",
                    String.format("Incapacidad EPS al 50%% (%d días, acumulado >= 90)", diasIngresados)));

        } else if (diasAcumulados + diasIngresados <= DIAS_TOPE_EPS) {
            // Todo al 66.67%
            BigDecimal valor = valorDiaFinal66
                    .multiply(BigDecimal.valueOf(diasIngresados))
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            resultado.add(construirDevengoIncapacidad(
                    concepto, novedad, diasIngresados, salario, valor,
                    "Incapacidad por enfermedad general (66.67%)",
                    String.format("Incapacidad EPS al 66.67%% (%d días)", diasIngresados)));

        } else {
            // Desglose: parte al 66.67% y parte al 50%
            int diasAl66 = DIAS_TOPE_EPS - diasAcumulados;
            int diasAl50 = diasIngresados - diasAl66;

            BigDecimal valor66 = valorDiaFinal66
                    .multiply(BigDecimal.valueOf(diasAl66))
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            BigDecimal valor50 = valorDiaFinal50
                    .multiply(BigDecimal.valueOf(diasAl50))
                    .setScale(ESCALA, RoundingMode.HALF_UP);

            resultado.add(construirDevengoIncapacidad(
                    concepto, novedad, diasAl66, salario, valor66,
                    "Incapacidad por enfermedad general (66.67%)",
                    String.format("Incapacidad EPS al 66.67%% (%d días)", diasAl66)));

            resultado.add(construirDevengoIncapacidad(
                    concepto, novedad, diasAl50, salario, valor50,
                    "Incapacidad por enfermedad general (50%)",
                    String.format("Incapacidad EPS al 50%% (%d días, acumulado supera 90)", diasAl50)));
        }

        return resultado;
    }

    private DevengoCalculado construirDevengoIncapacidad(
            ConceptoNominaDTO concepto,
            Novedad novedad,
            int dias,
            BigDecimal salario,
            BigDecimal valor,
            String nombreMostrar,
            String textoInformativo
    ) {
        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(nombreMostrar)
                .cantidad(dias)
                .baseCalculo(salario)
                .valorResultado(valor)
                .esSalario(true)
                .esIbc(true)
                .esAuxilioTransporte(false)
                .novedadId(novedad.getNovedadId())
                .textoInformativo(textoInformativo)
                .build();
    }

    private int obtenerDiasAcumulados(
            ContextoLiquidacion ctx,
            Long concepNominaId
    ) {
        // Buscar el repositorio a través del contexto no es posible directamente,
        // así que lo inyectamos vía constructor — ver ajuste 4
        BigDecimal acumulado = reporteNominaDetalleRepository
                .sumDiasIncapacidadAcumulados(
                        ctx.getEmpleado().empleadoId(),
                        concepNominaId,
                        ctx.getAnio(),
                        ctx.getPeriodo()
                );
        return acumulado != null ? acumulado.intValue() : 0;
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
                .esInformativo(false)
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

        BigDecimal valorDia = salario.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valor = valorDia
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(salario)
                .valorResultado(valor)
                .esSalario(true)
                .esIbc(true)
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

        BigDecimal valorDia = salario.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);
        BigDecimal valor = valorDia
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, RoundingMode.HALF_UP);

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
        // Licencias remuneradas a cargo del empleador: base es el salario ordinario actual
        BigDecimal salarioBase = ctx.getEmpleado().salarioBascMensual();
        BigDecimal valorDia = salarioBase.divide(DIAS_MES, ESCALA, RoundingMode.HALF_UP);

        int dias = novedad.getCantidadDiasNovedad() != null
                ? novedad.getCantidadDiasNovedad() : 0;

        BigDecimal valor = valorDia
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return DevengoCalculado.builder()
                .concepNominaId(concepto.concepNominaId())
                .nombreConcepto(concepto.nombreConcepNomina())
                .cantidad(dias)
                .baseCalculo(salarioBase)
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
                .observacion(novedad.getObservaciones())
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
