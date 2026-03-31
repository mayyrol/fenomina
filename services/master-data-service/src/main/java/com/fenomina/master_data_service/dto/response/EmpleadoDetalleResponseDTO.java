package com.fenomina.master_data_service.dto.response;

import com.fenomina.master_data_service.enums.EstadoEmpleado;
import com.fenomina.master_data_service.enums.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmpleadoDetalleResponseDTO(
        Long empleadoId,
        TipoDocumento tipoDocumento,
        String documentoEmp,
        String nombresEmp,
        String apellidosEmp,
        String direccionEmp,
        String tipoContratoEmp,
        LocalDate fechaIngresoEmp,
        LocalDate fechaRetiroEmp,
        LocalDate fechaFinContrato,
        String cargoEmp,
        Boolean esSalarioIntegral,
        BigDecimal salarioBascMensual,
        Boolean tieneAuxTransporte,
        String claseRiesgo,
        String tipoCotizante,
        String subtipoCotizante,
        String nombreArl,
        String nombreEps,
        String fondoPensionEmp,
        String cajaCompensacion,
        String fondoCesantiasEmp,
        Boolean estaExnrdParafis,
        String jornadaTrabajoEmp,
        EstadoEmpleado estadoEmp,
        EmpresaBasicInfoDTO empresa,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


