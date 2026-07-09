package com.fenomina.historicos_service.dto.conceptos;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReporteProximasVacacionesDTO {
    private String  documentoEmp;
    private String  nombresEmp;
    private String  apellidosEmp;
    private LocalDate fechaIngresoEmp;
    private Integer  anioUltimasVac;
    private LocalDate fechaInicioUltimasVac;
    private LocalDate fechaFinUltimasVac;
    private LocalDate proximaFechaVac;
    private String   fuente; // "Desde ingreso" o "Desde últimas vacaciones"
}
