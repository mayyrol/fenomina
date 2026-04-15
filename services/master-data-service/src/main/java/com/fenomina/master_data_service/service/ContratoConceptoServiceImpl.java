package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.ContratoConceptoRequestDTO;
import com.fenomina.master_data_service.dto.response.ContratoConceptoResponseDTO;
import com.fenomina.master_data_service.entity.ConceptoNomina;
import com.fenomina.master_data_service.entity.ContratoConcepto;
import com.fenomina.master_data_service.entity.Empleado;
import com.fenomina.master_data_service.exceptions.ConceptoNominaNotFoundException;
import com.fenomina.master_data_service.exceptions.ContratoConceptoNotFoundException;
import com.fenomina.master_data_service.exceptions.DuplicateContratoConceptoException;
import com.fenomina.master_data_service.exceptions.EmpleadoNotFoundException;
import com.fenomina.master_data_service.mappers.ContratoConceptoMapper;
import com.fenomina.master_data_service.repository.ConceptoNominaRepository;
import com.fenomina.master_data_service.repository.ContratoConceptoRepository;
import com.fenomina.master_data_service.repository.EmpleadoRepository;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratoConceptoServiceImpl implements ContratoConceptoService {

    private final ContratoConceptoRepository contratoConceptoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ConceptoNominaRepository conceptoNominaRepository;
    private final ContratoConceptoMapper contratoConceptoMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ContratoConceptoResponseDTO create(ContratoConceptoRequestDTO request) {
        log.info("Asignando concepto ID: {} a empleado ID: {}",
                request.conceptoNominaId(), request.empleadoId());

        // 1. Validar que el empleado existe
        Empleado empleado = empleadoRepository.findByIdActiveWithEmpresa(request.empleadoId())
                .orElseThrow(() -> {
                    log.warn("Empleado no encontrado con ID: {}", request.empleadoId());
                    return new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
                });

        // 2. Validar que el concepto existe
        ConceptoNomina concepto = conceptoNominaRepository.findById(request.conceptoNominaId())
                .orElseThrow(() -> {
                    log.warn("Concepto de nómina no encontrado con ID: {}", request.conceptoNominaId());
                    return new ConceptoNominaNotFoundException(ValidationMessages.CONCEPTO_NOT_FOUND);
                });

        // 3. Validar que el empleado no tenga ya asignado ese concepto
        if (contratoConceptoRepository.existsByEmpleadoAndConcepto(
                request.empleadoId(),
                request.conceptoNominaId()
        )) {
            log.warn("El empleado {} ya tiene asignado el concepto {}",
                    request.empleadoId(), request.conceptoNominaId());
            throw new DuplicateContratoConceptoException(
                    ValidationMessages.CONTRATO_CONCEPTO_DUPLICATE
            );
        }

        // 4. Mapear y asignar relaciones
        ContratoConcepto contratoConcepto = contratoConceptoMapper.toEntity(request);
        contratoConcepto.setEmpleado(empleado);
        contratoConcepto.setConceptoNomina(concepto);

        // 5. Guardar
        ContratoConcepto contratoConceptoGuardado = contratoConceptoRepository.save(contratoConcepto);

        log.info("Concepto asignado exitosamente. Contrato concepto ID: {}",
                contratoConceptoGuardado.getContratoConceptId());

        return contratoConceptoMapper.toResponseDTO(contratoConceptoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoConceptoResponseDTO> findByEmpleadoId(Long empleadoId) {
        log.debug("Buscando conceptos asignados al empleado ID: {}", empleadoId);

        // Validar que el empleado existe
        if (!empleadoRepository.existsById(empleadoId)) {
            log.warn("Empleado no encontrado con ID: {}", empleadoId);
            throw new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
        }

        List<ContratoConcepto> contratos = contratoConceptoRepository
                .findByEmpleadoIdActiveWithRelations(empleadoId);

        log.debug("Conceptos encontrados para empleado {}: {}", empleadoId, contratos.size());

        return contratoConceptoMapper.toResponseDTOList(contratos);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoConceptoResponseDTO findById(Long id) {
        log.debug("Buscando contrato concepto por ID: {}", id);

        ContratoConcepto contratoConcepto = contratoConceptoRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Contrato concepto no encontrado con ID: {}", id);
                    return new ContratoConceptoNotFoundException(
                            ValidationMessages.CONTRATO_CONCEPTO_NOT_FOUND
                    );
                });

        return contratoConceptoMapper.toResponseDTO(contratoConcepto);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando (soft delete) contrato concepto ID: {}", id);

        ContratoConcepto contratoConcepto = contratoConceptoRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.warn("Contrato concepto no encontrado con ID: {}", id);
                    return new ContratoConceptoNotFoundException(
                            ValidationMessages.CONTRATO_CONCEPTO_NOT_FOUND
                    );
                });

        // Soft delete
        contratoConcepto.softDelete();
        contratoConceptoRepository.saveAndFlush(contratoConcepto);

        log.info("Contrato concepto eliminado (soft delete) exitosamente: {}", id);
    }

    // En ContratoConceptoServiceImpl:
    @Override
    @Transactional
    public ContratoConceptoResponseDTO update(Long id, ContratoConceptoRequestDTO request) {
        ContratoConcepto contratoConcepto = contratoConceptoRepository.findByIdActive(id)
                .orElseThrow(() -> new ContratoConceptoNotFoundException(
                        ValidationMessages.CONTRATO_CONCEPTO_NOT_FOUND));

        // Si cambió el concepto, validar que no exista ya ese concepto para el empleado
        if (!contratoConcepto.getConceptoNomina().getConcepNominaId()
                .equals(request.conceptoNominaId())) {
            if (contratoConceptoRepository.existsByEmpleadoAndConceptoAndNotId(
                    contratoConcepto.getEmpleado().getEmpleadoId(),
                    request.conceptoNominaId(),
                    id)) {
                throw new DuplicateContratoConceptoException(
                        ValidationMessages.CONTRATO_CONCEPTO_DUPLICATE);
            }
            ConceptoNomina nuevoConcepto = conceptoNominaRepository
                    .findById(request.conceptoNominaId())
                    .orElseThrow(() -> new ConceptoNominaNotFoundException(
                            ValidationMessages.CONCEPTO_NOT_FOUND));
            contratoConcepto.setConceptoNomina(nuevoConcepto);
        }

        if (request.valorFijo() != null) {
            contratoConcepto.setValorFijo(request.valorFijo());
        }

        return contratoConceptoMapper.toResponseDTO(
                contratoConceptoRepository.save(contratoConcepto));
    }
}
