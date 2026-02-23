package com.fenomina.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para validación de token (endpoint interno).
 * Usado por otros microservicios para validar tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponseDTO {

    private boolean valid;
    private Long usuarioId;
    private String username;
    private String rol;
    private Long empresaId;
}
