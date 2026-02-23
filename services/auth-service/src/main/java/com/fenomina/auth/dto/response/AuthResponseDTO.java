package com.fenomina.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta después de login exitoso o refresh token.
 * Contiene los tokens y datos del usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn; // Tiempo de expiración en segundos
    private String tokenType; // "Bearer"
    private UsuarioResponseDTO usuario;
}