package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import com.fenomina.master_data_service.entity.Empresa;
import com.fenomina.master_data_service.exceptions.DuplicateNitException;
import com.fenomina.master_data_service.exceptions.EmpresaNotFoundException;
import com.fenomina.master_data_service.mappers.EmpresaMapper;
import com.fenomina.master_data_service.repository.EmpresaRepository;
import com.fenomina.master_data_service.util.FileUtils;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public EmpresaResponseDTO create(EmpresaRequestDTO request, MultipartFile logo) {
        log.info("Creando empresa con NIT: {}", request.empresaNit());

        // Validar NIT único
        if (empresaRepository.existsByNitActive(request.empresaNit())) {
            log.warn("Intento de crear empresa con NIT duplicado: {}", request.empresaNit());
            throw new DuplicateNitException(ValidationMessages.EMPRESA_NIT_DUPLICATE);
        }

        // Mapear DTO a entidad
        Empresa empresa = empresaMapper.toEntity(request);

        // Guardar empresa primero para obtener el ID
        Empresa empresaGuardada = empresaRepository.save(empresa);

        // Guardar logo si existe
        if (!FileUtils.isFileEmpty(logo)) {
            String logoPath = fileStorageService.storeFile(logo, empresaGuardada.getEmpresaId());
            empresaGuardada.setLogoEmpresaUrl(logoPath);
            empresaGuardada = empresaRepository.save(empresaGuardada);
            log.info("Logo guardado para empresa ID: {} en ruta: {}",
                    empresaGuardada.getEmpresaId(), logoPath);
        }

        log.info("Empresa creada exitosamente con ID: {}", empresaGuardada.getEmpresaId());

        return empresaMapper.toResponseDTO(empresaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaResponseDTO> findAll(String nombreFiltro) {
        log.debug("Buscando empresas con filtro: {}", nombreFiltro);

        List<Empresa> empresas;

        if (nombreFiltro != null && !nombreFiltro.trim().isEmpty()) {
            empresas = empresaRepository.findByNombreContaining(nombreFiltro.trim());
        } else {
            empresas = empresaRepository.findAllActive();
        }

        log.debug("Empresas encontradas: {}", empresas.size());

        return empresaMapper.toResponseDTOList(empresas);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponseDTO findById(Long id) {
        log.debug("Buscando empresa por ID: {}", id);

        Empresa empresa = empresaRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Empresa no encontrada con ID: {}", id);
                    return new EmpresaNotFoundException(ValidationMessages.EMPRESA_NOT_FOUND);
                });

        return empresaMapper.toResponseDTO(empresa);
    }

    @Override
    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO request, MultipartFile logo) {
        log.info("Actualizando empresa ID: {}", id);

        // Buscar empresa existente
        Empresa empresa = empresaRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Empresa no encontrada con ID: {}", id);
                    return new EmpresaNotFoundException(ValidationMessages.EMPRESA_NOT_FOUND);
                });

        // Validar NIT único (excluyendo la empresa actual)
        if (!empresa.getEmpresaNit().equals(request.empresaNit()) &&
                empresaRepository.existsByNitAndNotId(request.empresaNit(), id)) {
            log.warn("Intento de actualizar con NIT duplicado: {}", request.empresaNit());
            throw new DuplicateNitException(ValidationMessages.EMPRESA_NIT_DUPLICATE);
        }

        // Actualizar datos básicos
        empresaMapper.updateEntityFromDTO(request, empresa);

        // Manejar logo
        if (!FileUtils.isFileEmpty(logo)) {
            // Eliminar logo anterior si existe
            if (empresa.getLogoEmpresaUrl() != null) {
                fileStorageService.deleteFile(empresa.getLogoEmpresaUrl());
                log.debug("Logo anterior eliminado: {}", empresa.getLogoEmpresaUrl());
            }

            // Guardar nuevo logo
            String logoPath = fileStorageService.storeFile(logo, empresa.getEmpresaId());
            empresa.setLogoEmpresaUrl(logoPath);
            log.info("Nuevo logo guardado: {}", logoPath);
        }

        Empresa empresaActualizada = empresaRepository.save(empresa);

        log.info("Empresa actualizada exitosamente: {}", empresaActualizada.getEmpresaId());

        return empresaMapper.toResponseDTO(empresaActualizada);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando (soft delete) empresa ID: {}", id);

        Empresa empresa = empresaRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Empresa no encontrada con ID: {}", id);
                    return new EmpresaNotFoundException(ValidationMessages.EMPRESA_NOT_FOUND);
                });

        // Soft delete
        empresa.softDelete();
        empresaRepository.save(empresa);

        log.info("Empresa eliminada (soft delete) exitosamente: {}", id);
    }
}
