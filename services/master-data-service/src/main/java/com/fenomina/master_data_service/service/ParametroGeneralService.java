package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.ParametroGeneralRequestDTO;
import com.fenomina.master_data_service.dto.response.ParametroGeneralResponseDTO;

import java.util.List;

public interface ParametroGeneralService {

    /**
     * Crea un nuevo parámetro general.
     * Solo accesible por SUPER_ADMIN.
     */
    ParametroGeneralResponseDTO create(ParametroGeneralRequestDTO request);

    List<ParametroGeneralResponseDTO> findAll();

    ParametroGeneralResponseDTO findById(Long id);
}
