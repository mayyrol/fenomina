package com.fenomina.master_data_service.dto.request;

import com.fenomina.master_data_service.enums.EstadoEmpleado;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequestDTO(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoEmpleado nuevoEstado
) {
}