package com.fenomina.payroll_engine.dto.response;

import com.fenomina.payroll_engine.entity.EnvioDesprendibleDetalle;

public record EnvioDesprendibleDetalleResponseDTO(
        String correoDestino,
        String estadoDetalle,
        String mensajeError
) {
    public static EnvioDesprendibleDetalleResponseDTO desde(EnvioDesprendibleDetalle detalle) {
        return new EnvioDesprendibleDetalleResponseDTO(
                detalle.getCorreoDestino(),
                detalle.getEstadoDetalle().name(),
                detalle.getMensajeError()
        );
    }
}