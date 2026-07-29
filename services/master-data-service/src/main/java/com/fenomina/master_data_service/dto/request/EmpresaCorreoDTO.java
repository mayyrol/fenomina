package com.fenomina.master_data_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaCorreoDTO(

        Long empresaCorreoId,

        @NotBlank(message = "El correo no puede estar vacío")
        @Email(message = "Debe ingresar un correo electrónico válido")
        @Size(max = 150, message = "El correo no puede tener más de 150 caracteres")
        String correo
) {
}