package com.fenomina.payroll_engine.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoDTO(
        Long empleadoId,
        Long empresaId,
        String tipoDocumento,
        String documentoEmp,
        String nombresEmp,
        String apellidosEmp,
        String tipoContratoEmp,
        LocalDate fechaIngresoEmp,
        String cargoEmp,
        Boolean esSalarioIntegral,
        BigDecimal salarioBascMensual,
        Boolean tieneAuxTransporte,
        String claseRiesgo,
        String tipoCotizante,
        String subtipoCotizante,
        String fondoPensionEmp,
        String cajaCompensacion,
        String fondoCesantiasEmp,
        Boolean estaExnrdParafis,
        String jornadaTrabajoEmp,
        String estadoEmp
) {}
