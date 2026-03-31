package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.ParametroGeneralRequestDTO;
import com.fenomina.master_data_service.dto.response.ParametroGeneralResponseDTO;
import com.fenomina.master_data_service.entity.ParametroGeneral;
import com.fenomina.master_data_service.exceptions.ParametroGeneralNotFoundException;
import com.fenomina.master_data_service.mappers.ParametroGeneralMapper;
import com.fenomina.master_data_service.repository.ParametroGeneralRepository;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParametroGeneralServiceImpl implements ParametroGeneralService {

    private final ParametroGeneralRepository parametroGeneralRepository;
    private final ParametroGeneralMapper parametroGeneralMapper;

    @Override
    @Transactional
    public ParametroGeneralResponseDTO create(ParametroGeneralRequestDTO request) {
        log.info("Creando parámetro general: {}", request.nombreParamGeneral());

        // Mapear DTO a entidad
        ParametroGeneral parametro = parametroGeneralMapper.toEntity(request);

        // Guardar
        ParametroGeneral parametroGuardado = parametroGeneralRepository.save(parametro);

        log.info("Parámetro general creado exitosamente con ID: {}", parametroGuardado.getParamGeneralId());

        return parametroGeneralMapper.toResponseDTO(parametroGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroGeneralResponseDTO> findAll() {
        log.debug("Listando todos los parámetros generales");

        List<ParametroGeneral> parametros = parametroGeneralRepository.findAllByOrderByFechaParamGeneralDesc();

        log.debug("Parámetros encontrados: {}", parametros.size());

        return parametroGeneralMapper.toResponseDTOList(parametros);
    }

    @Override
    @Transactional(readOnly = true)
    public ParametroGeneralResponseDTO findById(Long id) {
        log.debug("Buscando parámetro general por ID: {}", id);

        ParametroGeneral parametro = parametroGeneralRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Parámetro general no encontrado con ID: {}", id);
                    return new ParametroGeneralNotFoundException(ValidationMessages.PARAMETRO_NOT_FOUND);
                });

        return parametroGeneralMapper.toResponseDTO(parametro);
    }
}
