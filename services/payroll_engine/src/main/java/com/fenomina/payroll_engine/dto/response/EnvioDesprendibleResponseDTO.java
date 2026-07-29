package com.fenomina.payroll_engine.dto.response;

import com.fenomina.payroll_engine.entity.EnvioDesprendible;
import com.fenomina.payroll_engine.entity.EnvioDesprendibleDetalle;

import java.time.LocalDateTime;
import java.util.List;

public record EnvioDesprendibleResponseDTO(
        Long envioDesprendibleId,
        Long fkProcesoLiquiId,
        String estadoEnvio,
        LocalDateTime fechaEnvio,
        String asuntoCorreo,
        List<EnvioDesprendibleDetalleResponseDTO> detalles
) {
    public static EnvioDesprendibleResponseDTO desde(
            EnvioDesprendible envio,
            List<EnvioDesprendibleDetalle> detalles
    ) {
        return new EnvioDesprendibleResponseDTO(
                envio.getEnvioDesprendibleId(),
                envio.getFkProcesoLiquiId(),
                envio.getEstadoEnvio().name(),
                envio.getFechaEnvio(),
                envio.getAsuntoCorreo(),
                detalles.stream()
                        .map(EnvioDesprendibleDetalleResponseDTO::desde)
                        .toList()
        );
    }
}