package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.ContratoConceptoRequestDTO;
import com.fenomina.master_data_service.dto.response.ContratoConceptoResponseDTO;

import java.util.List;

public interface ContratoConceptoService {

    ContratoConceptoResponseDTO create(ContratoConceptoRequestDTO request);

    List<ContratoConceptoResponseDTO> findByEmpleadoId(Long empleadoId);

    ContratoConceptoResponseDTO findById(Long id);

    void delete(Long id);
}
