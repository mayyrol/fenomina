package com.fenomina.historicos_service.config;

public final class ConceptoNominaIds {

    private ConceptoNominaIds() {
        throw new UnsupportedOperationException("Clase de constantes");
    }

    // Seguridad social empleador
    public static final Long SALUD_EMPLEADOR   = 42L;
    public static final Long PENSION_EMPLEADOR = 43L;
    public static final Long ARL_EMPLEADOR     = 44L;

    // Parafiscales empleador
    public static final Long SENA_EMPLEADOR  = 45L;
    public static final Long ICBF_EMPLEADOR  = 46L;
    public static final Long CAJA_COMP       = 47L;

    // Deducciones empleado
    public static final Long SALUD_EMPLEADO   = 33L;
    public static final Long PENSION_EMPLEADO = 34L;
    public static final Long FONDO_SOLIDARIDAD = 35L;
    public static final Long RETEFUENTE       = 36L;

    // Vacaciones
    public static final Long VACACIONES_DISFRUTADAS  = 2L;
    public static final Long VACACIONES_COMPENSADAS  = 3L;

    // Horas extra y recargos
    public static final Long RECARGO_NOCTURNO_LUN_SAB    = 24L;
    public static final Long RECARGO_DIURNO_DOM_FEST     = 25L;
    public static final Long RECARGO_NOCTURNO_DOM_FEST   = 26L;
    public static final Long HORA_EXTRA_DIURNA_LUN_SAB   = 27L;
    public static final Long HORA_EXTRA_NOCTURNA_LUN_SAB = 28L;
    public static final Long HORA_EXTRA_DIURNA_DOM_FEST  = 29L;
    public static final Long HORA_EXTRA_NOCTURNA_DOM_FEST= 30L;

    // Incapacidades
    public static final Long INCAPACIDAD_COMUN   = 4L;
    public static final Long INCAPACIDAD_LABORAL = 5L;

    // Licencias remuneradas
    public static final Long LICENCIA_MATERNIDAD_PATERNIDAD = 6L;
    public static final Long LICENCIA_MATERNIDAD            = 6L;
    public static final Long LICENCIA_PATERNIDAD            = 7L;
    public static final Long LICENCIA_CALAMIDAD             = 8L;
    public static final Long LICENCIA_MATRIMONIO            = 9L;
    public static final Long LICENCIA_ISAAC                 = 10L;
    public static final Long LICENCIA_SUFRAGIO              = 11L;
    public static final Long CARGOS_TRANSITORIOS            = 12L;
    public static final Long CITACIONES_JUDICIALES          = 13L;
    public static final Long OTROS_PERMISOS_REMUNERADOS     = 14L;
    public static final Long LICENCIAS_NO_REMUNERADAS       = 15L;

    // Prestaciones sociales
    public static final Long CESANTIAS           = 39L;
    public static final Long PRIMA_SERVICIOS     = 40L;
    public static final Long INTERESES_CESANTIAS = 41L;
}