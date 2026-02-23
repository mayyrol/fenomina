package com.fenomina.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para respuestas simples con mensaje.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    private String message;
    private Object data; // Opcional

    public MessageResponseDTO(String message) {
        this.message = message;
    }
}
