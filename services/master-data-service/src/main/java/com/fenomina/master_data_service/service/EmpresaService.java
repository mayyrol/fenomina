package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmpresaService {

    /**
     * Crea una nueva empresa
     */
    EmpresaResponseDTO create(EmpresaRequestDTO request, MultipartFile logo);

    /**
     * Lista todas las empresas activas, con filtro opcional por nombre
     */
    List<EmpresaResponseDTO> findAll(String nombreFiltro);

    /**
     * Obtiene una empresa por ID
     */
    EmpresaResponseDTO findById(Long id);

    /**
     * Actualiza una empresa existente
     */
    EmpresaResponseDTO update(Long id, EmpresaRequestDTO request, MultipartFile logo);

    /**
     * Elimina (soft delete) una empresa
     */
    void delete(Long id);
}
