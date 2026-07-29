package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.EmpresaCorreoDTO;
import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import com.fenomina.master_data_service.entity.Empresa;
import com.fenomina.master_data_service.entity.EmpresaCorreo;
import com.fenomina.master_data_service.exceptions.DuplicateCorreoException;
import com.fenomina.master_data_service.exceptions.DuplicateNitException;
import com.fenomina.master_data_service.exceptions.EmpresaNotFoundException;
import com.fenomina.master_data_service.mappers.EmpresaMapper;
import com.fenomina.master_data_service.repository.EmpresaCorreoRepository;
import com.fenomina.master_data_service.repository.EmpresaRepository;
import com.fenomina.master_data_service.util.FileUtils;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaCorreoRepository empresaCorreoRepository;
    private final EmpresaMapper empresaMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public EmpresaResponseDTO create(EmpresaRequestDTO request, MultipartFile logo) {
        log.info("Creando empresa con NIT: {}", request.empresaNit());

        if (empresaRepository.existsByNitActive(request.empresaNit())) {
            log.warn("Intento de crear empresa con NIT duplicado: {}", request.empresaNit());
            throw new DuplicateNitException(ValidationMessages.EMPRESA_NIT_DUPLICATE);
        }

        Empresa empresa = empresaMapper.toEntity(request);
        Empresa empresaGuardada = empresaRepository.save(empresa);

        if (!FileUtils.isFileEmpty(logo)) {
            String logoPath = fileStorageService.storeFile(logo, empresaGuardada.getEmpresaId());
            empresaGuardada.setLogoEmpresaUrl(logoPath);
            empresaGuardada = empresaRepository.save(empresaGuardada);
            log.info("Logo guardado para empresa ID: {} en ruta: {}",
                    empresaGuardada.getEmpresaId(), logoPath);
        }

        sincronizarCorreos(empresaGuardada, request.correos());

        log.info("Empresa creada exitosamente con ID: {}", empresaGuardada.getEmpresaId());

        return construirResponseConCorreos(empresaGuardada);
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

        return empresas.stream()
                .map(this::construirResponseConCorreos)
                .toList();
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

        return construirResponseConCorreos(empresa);
    }

    @Override
    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO request, MultipartFile logo) {
        log.info("Actualizando empresa ID: {}", id);

        Empresa empresa = empresaRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Empresa no encontrada con ID: {}", id);
                    return new EmpresaNotFoundException(ValidationMessages.EMPRESA_NOT_FOUND);
                });

        if (!empresa.getEmpresaNit().equals(request.empresaNit()) &&
                empresaRepository.existsByNitAndNotId(request.empresaNit(), id)) {
            log.warn("Intento de actualizar con NIT duplicado: {}", request.empresaNit());
            throw new DuplicateNitException(ValidationMessages.EMPRESA_NIT_DUPLICATE);
        }

        empresaMapper.updateEntityFromDTO(request, empresa);

        if (!FileUtils.isFileEmpty(logo)) {
            if (empresa.getLogoEmpresaUrl() != null) {
                fileStorageService.deleteFile(empresa.getLogoEmpresaUrl());
                log.debug("Logo anterior eliminado: {}", empresa.getLogoEmpresaUrl());
            }
            String logoPath = fileStorageService.storeFile(logo, empresa.getEmpresaId());
            empresa.setLogoEmpresaUrl(logoPath);
            log.info("Nuevo logo guardado: {}", logoPath);
        }

        Empresa empresaActualizada = empresaRepository.save(empresa);

        sincronizarCorreos(empresaActualizada, request.correos());

        log.info("Empresa actualizada exitosamente: {}", empresaActualizada.getEmpresaId());

        return construirResponseConCorreos(empresaActualizada);
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

        empresa.softDelete();
        empresaRepository.save(empresa);

        log.info("Empresa eliminada (soft delete) exitosamente: {}", id);
    }


    private void sincronizarCorreos(Empresa empresa, List<EmpresaCorreoDTO> correosDTO) {
        List<EmpresaCorreoDTO> correosNormalizados = correosDTO == null ? List.of() : correosDTO;

        Set<String> vistos = new HashSet<>();
        for (EmpresaCorreoDTO c : correosNormalizados) {
            String normalizado = c.correo().trim().toLowerCase();
            if (!vistos.add(normalizado)) {
                throw new DuplicateCorreoException(ValidationMessages.EMPRESA_CORREO_DUPLICADO);
            }
        }

        List<EmpresaCorreo> existentes = empresaCorreoRepository.findByEmpresaIdActive(empresa.getEmpresaId());

        Set<Long> idsEnRequest = correosNormalizados.stream()
                .map(EmpresaCorreoDTO::empresaCorreoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        existentes.stream()
                .filter(e -> !idsEnRequest.contains(e.getEmpresaCorreoId()))
                .forEach(e -> {
                    e.softDelete();
                    empresaCorreoRepository.save(e);
                });

        for (EmpresaCorreoDTO dto : correosNormalizados) {
            if (dto.empresaCorreoId() != null) {
                existentes.stream()
                        .filter(e -> e.getEmpresaCorreoId().equals(dto.empresaCorreoId()))
                        .findFirst()
                        .ifPresent(e -> {
                            e.setCorreo(dto.correo());
                            empresaCorreoRepository.save(e);
                        });
            } else {
                EmpresaCorreo nuevo = new EmpresaCorreo();
                nuevo.setEmpresaId(empresa.getEmpresaId());
                nuevo.setCorreo(dto.correo());
                empresaCorreoRepository.save(nuevo);
            }
        }
    }

    private EmpresaResponseDTO construirResponseConCorreos(Empresa empresa) {
        EmpresaResponseDTO base = empresaMapper.toResponseDTO(empresa);

        List<EmpresaCorreoDTO> correosDTO = empresaCorreoRepository
                .findByEmpresaIdActive(empresa.getEmpresaId())
                .stream()
                .map(ec -> new EmpresaCorreoDTO(ec.getEmpresaCorreoId(), ec.getCorreo()))
                .toList();

        return new EmpresaResponseDTO(
                base.empresaId(), base.empresaNit(), base.razonSocial(), base.nombreEmpresa(),
                base.esExoneradaLey1607(), base.logoEmpresaUrl(), base.aplicaNomina(),
                base.aplicaPrima(), base.aplicaCesantias(), correosDTO,
                base.createdAt(), base.updatedAt()
        );
    }
}

