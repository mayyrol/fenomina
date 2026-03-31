package com.fenomina.master_data_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoCotizante {

    DEPENDIENTE("01", "Dependiente"),
    SERVICIO_DOMESTICO("02", "Servicio Doméstico"),
    INDEPENDIENTE("03", "Independiente"),
    APRENDIZ_SENA_LECTIVA("12", "Aprendiz SENA (Etapa Lectiva)"),
    APRENDIZ_SENA_PRODUCTIVA("19", "Aprendiz SENA (Etapa Productiva)"),
    ESTUDIANTE_LEY_789("20", "Estudiantes (Régimen Especial Ley 789 de 2002)"),
    ESTUDIANTE_SOLO_ARL("23", "Estudiantes Aporte Solo Riesgos Laborales"),
    COTIZANTE_EMERGENCIA_1("44", "Cotizante Dependiente de Empleo de Emergencia"),
    COTIZANTE_EMERGENCIA_2("45", "Cotizante Dependiente de Empleo de Emergencia"),
    TIEMPO_PARCIAL("51", "Trabajador de Tiempo Parcial"),
    INDEPENDIENTE_PRESTACION_SERVICIOS("59", "Independiente con Contrato de Prestación de Servicios Superior a 1 Mes");

    private final String codigo;
    private final String descripcion;
}
