package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.CambiarEstadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoUpdateRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoDetalleResponseDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoResponseDTO;
import com.fenomina.master_data_service.enums.EstadoEmpleado;

import java.util.List;

public interface EmpleadoService {

    EmpleadoResponseDTO create(EmpleadoRequestDTO request);

    List<EmpleadoResponseDTO> findByFilters(Long empresaId, EstadoEmpleado estado, String documento);

    EmpleadoDetalleResponseDTO findById(Long id);

    EmpleadoResponseDTO update(Long id, EmpleadoUpdateRequestDTO request);

    EmpleadoResponseDTO cambiarEstado(Long id, CambiarEstadoRequestDTO request);

    void delete(Long id);
}
