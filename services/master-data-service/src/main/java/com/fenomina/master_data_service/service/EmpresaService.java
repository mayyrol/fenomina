package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmpresaService {

    EmpresaResponseDTO create(EmpresaRequestDTO request, MultipartFile logo);

    List<EmpresaResponseDTO> findAll(String nombreFiltro);

    EmpresaResponseDTO findById(Long id);

    EmpresaResponseDTO update(Long id, EmpresaRequestDTO request, MultipartFile logo);

    void delete(Long id);
}
