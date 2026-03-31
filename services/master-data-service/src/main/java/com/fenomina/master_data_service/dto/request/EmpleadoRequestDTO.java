package com.fenomina.master_data_service.dto.request;

import com.fenomina.master_data_service.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoRequestDTO(

        @NotNull(message = "El ID de la empresa es obligatorio")
        Long empresaId,

        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "El documento del empleado es obligatorio")
        @Size(max = 30, message = "El documento no puede tener más de 30 caracteres")
        String documentoEmp,

        @NotBlank(message = "Los nombres del empleado son obligatorios")
        @Size(max = 255, message = "Los nombres no pueden tener más de 255 caracteres")
        String nombresEmp,

        @NotBlank(message = "Los apellidos del empleado son obligatorios")
        @Size(max = 255, message = "Los apellidos no pueden tener más de 255 caracteres")
        String apellidosEmp,

        @Size(max = 255, message = "La dirección no puede tener más de 255 caracteres")
        String direccionEmp,

        @NotNull(message = "El tipo de contrato es obligatorio")
        TipoContrato tipoContratoEmp,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
        LocalDate fechaIngresoEmp,

        LocalDate fechaFinContrato,

        @Size(max = 60, message = "El cargo no puede tener más de 60 caracteres")
        String cargoEmp,

        @NotNull(message = "El salario básico mensual es obligatorio")
        @DecimalMin(value = "0.01", message = "El salario debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El salario tiene formato inválido")
        BigDecimal salarioBascMensual,

        @NotBlank(message = "La clase de riesgo es obligatoria")
        ClaseRiesgo claseRiesgo,

        @NotBlank(message = "El tipo de cotizante es obligatorio")
        TipoCotizante tipoCotizante,

        @NotBlank(message = "El subtipo de cotizante es obligatorio")
        SubtipoCotizante subtipoCotizante,

        @NotBlank(message = "El nombre de la ARL es obligatorio")
        @Size(max = 30, message = "El nombre de la ARL no puede tener más de 30 caracteres")
        String nombreArl,

        @NotBlank(message = "El nombre de la EPS es obligatorio")
        @Size(max = 30, message = "El nombre de la EPS no puede tener más de 30 caracteres")
        String nombreEps,

        @NotBlank(message = "El fondo de pensión es obligatorio")
        @Size(max = 60, message = "El fondo de pensión no puede tener más de 60 caracteres")
        String fondoPensionEmp,

        @NotBlank(message = "La caja de compensación es obligatoria")
        @Size(max = 60, message = "La caja de compensación no puede tener más de 60 caracteres")
        String cajaCompensacion,

        @NotBlank(message = "El fondo de cesantías es obligatorio")
        @Size(max = 60, message = "El fondo de cesantías no puede tener más de 60 caracteres")
        String fondoCesantiasEmp,

        Boolean estaExnrdParafis,

        @NotBlank(message = "El tipo de jornada laboral es obligatorio")
        JornadaTrabajo jornadaTrabajoEmp
) {
}
