package com.fenomina.payroll_engine.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DesprendiblePrestacionResponseDTO {

    private final Long cabeLiquiPrestacionId;
    private final Long empleadoId;
    private final String nombresEmpleado;
    private final String apellidosEmpleado;
    private final String documentoEmpleado;
    private final String fondoPension;
    private final Integer anio;
    private final Integer periodo;
    private final LocalDate fechaInicioCorte;
    private final LocalDate fechaFinCorte;
    private final Integer diasLiquidados;
    private final BigDecimal salarioBase;
    private final BigDecimal auxTransporte;
    private final BigDecimal baseLiquidacion;
    private final BigDecimal valorPrestacion;
    private final BigDecimal valorInteresesCesantias;
    private final String tipoPrestacion;
}
