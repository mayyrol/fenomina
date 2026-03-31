package com.fenomina.master_data_service.dto.request;

import com.fenomina.master_data_service.enums.TipoDocumento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoUpdateRequestDTO(

        TipoDocumento tipoDocumento,

        @Size(max = 30, message = "El documento no puede tener más de 30 caracteres")
        String documentoEmp,

        @Size(max = 255, message = "Los nombres no pueden tener más de 255 caracteres")
        String nombresEmp,

        @Size(max = 255, message = "Los apellidos no pueden tener más de 255 caracteres")
        String apellidosEmp,

        @Size(max = 255, message = "La dirección no puede tener más de 255 caracteres")
        String direccionEmp,

        @Size(max = 50, message = "El tipo de contrato no puede tener más de 50 caracteres")
        String tipoContratoEmp,

        @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
        LocalDate fechaIngresoEmp,

        LocalDate fechaFinContrato,

        @Size(max = 60, message = "El cargo no puede tener más de 60 caracteres")
        String cargoEmp,

        @DecimalMin(value = "0.01", message = "El salario debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El salario tiene formato inválido")
        BigDecimal salarioBascMensual,

        @Size(max = 5, message = "La clase de riesgo no puede tener más de 5 caracteres")
        String claseRiesgo,

        @Size(max = 60, message = "El tipo de cotizante no puede tener más de 60 caracteres")
        String tipoCotizante,

        @Size(max = 60, message = "El subtipo de cotizante no puede tener más de 60 caracteres")
        String subtipoCotizante,

        @Size(max = 30, message = "El nombre de la ARL no puede tener más de 30 caracteres")
        String nombreArl,

        @Size(max = 30, message = "El nombre de la EPS no puede tener más de 30 caracteres")
        String nombreEps,

        @Size(max = 60, message = "El fondo de pensión no puede tener más de 60 caracteres")
        String fondoPensionEmp,

        @Size(max = 60, message = "La caja de compensación no puede tener más de 60 caracteres")
        String cajaCompensacion,

        @Size(max = 60, message = "El fondo de cesantías no puede tener más de 60 caracteres")
        String fondoCesantiasEmp,

        Boolean estaExnrdParafis,

        @Size(max = 60, message = "La jornada de trabajo no puede tener más de 60 caracteres")
        String jornadaTrabajoEmp
) {
}
