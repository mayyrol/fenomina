package com.fenomina.master_data_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubtipoCotizante {

    CODIGO_0("0", "Ninguno"),
    CODIGO_1("1", "Dependiente Pensionado Activo"),
    CODIGO_3("3", "Cotizante No Obligado a Cotización a Pensiones por Edad"),
    CODIGO_4("4", "Con Requisitos Cumplidos para Pensión, Indemnización Sustitutiva o Devolución de Saldos"),
    CODIGO_5("5", "Cotizante con Devolución de Saldos"),
    CODIGO_6("6", "Cotizante Perteneciente a Régimen Exceptuado"),
    CODIGO_9("9", "Cotizante Pensionado con Mesada Superior a 25 SMMLV"),
    CODIGO_11("11", "Conductor de Vehículo Taxi"),
    CODIGO_12("12", "Conductor de Vehículo Taxi No Obligado a Cotizar Pensión");

    private final String codigo;
    private final String descripcion;
}
