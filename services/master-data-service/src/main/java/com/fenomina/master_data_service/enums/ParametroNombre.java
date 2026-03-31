package com.fenomina.master_data_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParametroNombre {

    // 1. Valores Base
    SMMLV("Salario Mínimo Mensual Legal Vigente", TipoParametro.VALOR),
    AUXILIO_TRANSPORTE("Auxilio de Transporte", TipoParametro.VALOR),
    VALOR_UVT("Valor UVT (Unidad de Valor Tributario)", TipoParametro.VALOR),
    SANCION_MINIMA_DIAN("Sanción Mínima DIAN (10 UVT)", TipoParametro.VALOR),
    TOPE_COTIZACION("Tope de Cotización (IBC Máximo)", TipoParametro.VALOR),
    SALARIO_INTEGRAL_MINIMO("Salario Integral Mínimo", TipoParametro.VALOR),

    // 2. Jornada y Recargos
    JORNADA_MAXIMA_SEMANAL("Jornada Máxima Semanal", TipoParametro.VALOR),
    HORAS_TRABAJADAS_MES("Horas Trabajadas al Mes", TipoParametro.VALOR),
    VALOR_HORA_ORDINARIA("Valor Hora Ordinaria", TipoParametro.VALOR),
    EXTRA_DIURNA("Extra Diurna", TipoParametro.PORCENTAJE),
    EXTRA_NOCTURNA("Extra Nocturna", TipoParametro.PORCENTAJE),
    EXTRA_DIURNA_DOMINICAL("Extra Diurna Dominical/Festiva", TipoParametro.PORCENTAJE),
    EXTRA_NOCTURNA_DOMINICAL("Extra Nocturna Dominical/Festiva", TipoParametro.PORCENTAJE),
    RECARGO_NOCTURNO("Recargo Nocturno", TipoParametro.PORCENTAJE),
    RECARGO_DIURNO_DOMINICAL("Recargo Diurno Dominical o Festivo", TipoParametro.PORCENTAJE),
    RECARGO_NOCTURNO_DOMINICAL("Recargo Nocturno en Dominical o Festivo", TipoParametro.PORCENTAJE),

    // 3. Seguridad Social
    SALUD_EMPLEADO("Salud Empleado", TipoParametro.PORCENTAJE),
    SALUD_EMPLEADOR("Salud Empleador", TipoParametro.PORCENTAJE),
    PENSION_EMPLEADO("Pensión Empleado", TipoParametro.PORCENTAJE),
    PENSION_EMPLEADOR("Pensión Empleador", TipoParametro.PORCENTAJE),
    FONDO_SOLIDARIDAD_PENSIONAL("Fondo de Solidaridad Pensional", TipoParametro.PORCENTAJE),
    ARL_EMPLEADOR("ARL (Riesgos Laborales) Empleador", TipoParametro.PORCENTAJE),

    // 4. Parafiscales
    CAJA_COMPENSACION("Caja de Compensación", TipoParametro.PORCENTAJE),
    SENA("SENA", TipoParametro.PORCENTAJE),
    ICBF("ICBF", TipoParametro.PORCENTAJE),

    // 5. Prestaciones Sociales
    PRIMA_SERVICIOS("Prima de Servicios", TipoParametro.PORCENTAJE),
    CESANTIAS("Cesantías", TipoParametro.PORCENTAJE),
    INTERESES_CESANTIAS("Intereses sobre Cesantías", TipoParametro.PORCENTAJE),
    VACACIONES("Vacaciones", TipoParametro.PORCENTAJE);

    private final String descripcion;
    private final TipoParametro tipo;

    public enum TipoParametro {
        VALOR,      // Guarda en valor_param_general
        PORCENTAJE  // Guarda en porcentaje_param_general
    }
}
