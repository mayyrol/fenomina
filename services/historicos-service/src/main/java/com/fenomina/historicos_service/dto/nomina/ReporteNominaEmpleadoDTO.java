package com.fenomina.historicos_service.dto.nomina;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReporteNominaEmpleadoDTO {

    // Datos de cabecera del desprendible
    private String nombreEmpresa;
    private String nitEmpresa;
    private Integer anio;
    private Integer periodo;
    private LocalDateTime fechaCierreNomina;

    // Datos del empleado
    private String nombresEmp;
    private String apellidosEmp;
    private String documentoEmp;
    private String tipoDocumento;
    private BigDecimal salarioBascMensual;
    private String cargoEmp;

    // Totales de la nómina
    private BigDecimal totalDevengado;
    private BigDecimal totalDeducciones;
    private BigDecimal netoAPagar;

    // Líneas de concepto (una por fila en el desprendible)
    private List<ConceptoNominaLineaDTO> conceptos;
}