package com.fenomina.auth.dto.request;

import com.fenomina.auth.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para registro de nuevos usuarios.
 * Solo puede ser usado por SUPER_ADMIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroUsuarioRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String userName;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 255, message = "Los nombres no pueden exceder 255 caracteres")
    private String nombresUsuario;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 255, message = "Los apellidos no pueden exceder 255 caracteres")
    private String apellidosUsuario;

    @NotBlank(message = "El número de identificación es obligatorio")
    @Size(max = 50, message = "El número de identificación no puede exceder 50 caracteres")
    private String numIdentiUsuario;

    @NotBlank(message = "El cargo es obligatorio")
    @Size(max = 60, message = "El cargo no puede exceder 60 caracteres")
    private String cargoUsuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
    )
    private String contrasenaUsuario;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rolUsuario;

    // Puede ser null (acceso a todas las empresas)
    private Long fkIdEmpresa;
}
