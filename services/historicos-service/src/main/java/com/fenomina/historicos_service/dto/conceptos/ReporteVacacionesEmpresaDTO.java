package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReporteVacacionesEmpresaDTO {

    private String documentoEmp;
    private String nombresEmp;
    private String apellidosEmp;
    private LocalDate fechaInicioVac;
    private LocalDate fechaFinVac;
    private String tipoVacaciones;
    private Integer diasTomados;
    private String estadoVacaciones;   // traído del tipo_vacacion en novedad
    private BigDecimal valorPagoVac;
}
