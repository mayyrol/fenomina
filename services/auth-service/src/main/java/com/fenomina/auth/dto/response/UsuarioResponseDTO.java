package com.fenomina.auth.dto.response;

import com.fenomina.auth.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long usuarioId;
    private String userName;
    private String nombresUsuario;
    private String apellidosUsuario;
    private String numIdentiUsuario;
    private String cargoUsuario;
    private RolUsuario rolUsuario;
    private Long fkIdEmpresa;
    private Boolean estadoUsuario;
    private LocalDateTime ultimoLogin;
    private LocalDateTime createdAt;
    private Boolean bloqueadoLogin;
    private Integer intentosFallidosLogin;
    private LocalDateTime fechaBloqueo;
}
