package com.fenomina.master_data_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpresaRequestDTO(

        @NotBlank(message = "El NIT de la empresa es obligatorio")
        @Size(max = 30, message = "El NIT no puede tener más de 30 caracteres")
        String empresaNit,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 70, message = "La razón social no puede tener más de 70 caracteres")
        String razonSocial,

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 70, message = "El nombre de la empresa no puede tener más de 70 caracteres")
        String nombreEmpresa,

        @NotNull(message = "Debe indicar si está exonerada por Ley 1607")
        Boolean esExoneradaLey1607,

        @NotNull(message = "Debe indicar si aplica nómina")
        Boolean aplicaNomina,

        @NotNull(message = "Debe indicar si aplica prima")
        Boolean aplicaPrima,

        @NotNull(message = "Debe indicar si aplica cesantías")
        Boolean aplicaCesantias
) {
}