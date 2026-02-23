package com.fenomina.auth.dto.request;

import com.fenomina.auth.enums.RolUsuario;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualización de usuarios existentes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarUsuarioRequestDTO {

    @Size(max = 255, message = "Los nombres no pueden exceder 255 caracteres")
    private String nombresUsuario;

    @Size(max = 255, message = "Los apellidos no pueden exceder 255 caracteres")
    private String apellidosUsuario;

    @Size(max = 60, message = "El cargo no puede exceder 60 caracteres")
    private String cargoUsuario;

    private RolUsuario rolUsuario;

    private Long fkIdEmpresa;
}
