package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteLicenciasDTO {

    private Integer anio;
    private Integer periodo;
    private LocalDate fechaPeriodo;
    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;

    // Días por tipo de licencia remunerada (de novedad)
    private Integer diasLicenciaMaternidadPaternidad;
    private Integer diasLicenciaCalamidad;
    private Integer diasLicenciaMatrimonio;
    private Integer diasLicenciaIsaac;
    private Integer diasLicenciaSufragio;
    private Integer diasCargosTransitorios;
    private Integer diasCitacionesJudiciales;
    private Integer diasOtrosPermisosRemunerados;

    // Valores liquidados (de reporte_nomina_detalle)
    private BigDecimal valorLicenciaMaternidadPaternidad;
    private BigDecimal valorLicenciaCalamidad;
    private BigDecimal valorLicenciaMatrimonio;
    private BigDecimal valorLicenciaIsaac;
    private BigDecimal valorLicenciaSufragio;
    private BigDecimal valorCargosTransitorios;
    private BigDecimal valorCitacionesJudiciales;
    private BigDecimal valorOtrosPermisosRemunerados;

    // Licencias no remuneradas
    private Integer diasLicenciasNoRemuneradas;
    private BigDecimal valorLicenciasNoRemuneradas;
}