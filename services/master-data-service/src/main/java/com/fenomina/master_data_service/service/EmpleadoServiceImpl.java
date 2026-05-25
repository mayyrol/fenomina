package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.dto.request.CambiarEstadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoUpdateRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoDetalleResponseDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoResponseDTO;
import com.fenomina.master_data_service.entity.Empleado;
import com.fenomina.master_data_service.entity.Empresa;
import com.fenomina.master_data_service.entity.HistorialSalario;
import com.fenomina.master_data_service.enums.EstadoEmpleado;
import com.fenomina.master_data_service.exceptions.DuplicateDocumentException;
import com.fenomina.master_data_service.exceptions.EmpleadoNotFoundException;
import com.fenomina.master_data_service.exceptions.EmpresaNotFoundException;
import com.fenomina.master_data_service.exceptions.InvalidStateTransitionException;
import com.fenomina.master_data_service.mappers.EmpleadoMapper;
import com.fenomina.master_data_service.repository.EmpleadoRepository;
import com.fenomina.master_data_service.repository.EmpresaRepository;
import com.fenomina.master_data_service.repository.HistorialSalarioRepository;
import com.fenomina.master_data_service.util.SalarioCalculator;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final HistorialSalarioRepository historialSalarioRepository;
    private final EmpleadoMapper empleadoMapper;
    private final SalarioCalculator salarioCalculator;

    @Override
    @Transactional
    public EmpleadoResponseDTO create(EmpleadoRequestDTO request) {
        log.info("Creando empleado con documento: {} para empresa ID: {}",
                request.documentoEmp(), request.empresaId());

        // 1. Validar que la empresa existe
        Empresa empresa = empresaRepository.findByIdActive(request.empresaId())
                .orElseThrow(() -> {
                    log.warn("Empresa no encontrada con ID: {}", request.empresaId());
                    return new EmpresaNotFoundException(ValidationMessages.EMPRESA_NOT_FOUND);
                });

        // 2. Validar documento único en la empresa
        if (empleadoRepository.existsByEmpresaIdAndTipoDocumentoAndDocumento(
                request.empresaId(), request.tipoDocumento(), request.documentoEmp())) {
            log.warn("Documento duplicado {} en empresa ID: {}",
                    request.documentoEmp(), request.empresaId());
            throw new DuplicateDocumentException(ValidationMessages.EMPLEADO_DOCUMENTO_DUPLICATE);
        }

        // 3. Mapear DTO a entidad
        Empleado empleado = empleadoMapper.toEntity(request);
        empleado.setEmpresa(empresa);

        // 4. Calcular si es salario integral
        boolean esSalarioIntegral = Boolean.TRUE.equals(request.esSalarioIntegral());

        if (esSalarioIntegral) {
            BigDecimal smmlv = salarioCalculator.obtenerSMMLVVigente(request.fechaIngresoEmp());
            BigDecimal tope = smmlv.multiply(BigDecimal.valueOf(13));
            if (request.salarioBascMensual().compareTo(tope) < 0) {
                throw new IllegalArgumentException(
                        String.format(
                                "El salario integral debe ser igual o superior a 13 SMMLV ($%s). " +
                                        "El salario ingresado ($%s) no cumple el requisito mínimo legal.",
                                tope.setScale(0, RoundingMode.HALF_UP),
                                request.salarioBascMensual().setScale(0, RoundingMode.HALF_UP)
                        )
                );
            }
        }

        empleado.setEsSalarioIntegral(esSalarioIntegral);

        // 5.
        empleado.setTieneAuxTransporte(request.tieneAuxTransporte() != null ? request.tieneAuxTransporte() : true);

        // 6. Guardar empleado
        Empleado empleadoGuardado = empleadoRepository.save(empleado);

        log.info("Empleado creado exitosamente con ID: {} - Salario integral: {} ",
                empleadoGuardado.getEmpleadoId(), esSalarioIntegral);

        // 7. Crear primer registro en historial de salarios
        crearHistorialSalario(empleadoGuardado, BigDecimal.ZERO, empleadoGuardado.getSalarioBascMensual());

        return empleadoMapper.toResponseDTO(empleadoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoResponseDTO> findByFilters(Long empresaId, EstadoEmpleado estado, String documento) {
        log.debug("Buscando empleados con filtros - Empresa: {}, Estado: {}, Documento: {}",
                empresaId, estado, documento);

        List<Empleado> empleados = empleadoRepository.findByFilters(empresaId, estado, documento);

        log.debug("Empleados encontrados: {}", empleados.size());

        return empleadoMapper.toResponseDTOList(empleados);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpleadoDetalleResponseDTO findById(Long id) {
        log.debug("Buscando empleado por ID: {}", id);

        Empleado empleado = empleadoRepository.findByIdActiveWithEmpresa(id)
                .orElseThrow(() -> {
                    log.warn("Empleado no encontrado con ID: {}", id);
                    return new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
                });

        return empleadoMapper.toDetalleResponseDTO(empleado);
    }

    @Override
    @Transactional
    public EmpleadoResponseDTO update(Long id, EmpleadoUpdateRequestDTO request) {
        log.info("Actualizando empleado ID: {}", id);

        // Buscar empleado existente
        Empleado empleado = empleadoRepository.findByIdActiveWithEmpresa(id)
                .orElseThrow(() -> {
                    log.warn("Empleado no encontrado con ID: {}", id);
                    return new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
                });

        // Validar documento único si cambió
        if (request.documentoEmp() != null &&
                (!empleado.getDocumentoEmp().equals(request.documentoEmp()) ||
                        (request.tipoDocumento() != null && !empleado.getTipoDocumento().equals(request.tipoDocumento()))) &&
                empleadoRepository.existsByEmpresaIdAndTipoDocumentoAndDocumentoAndNotId(
                        empleado.getEmpresa().getEmpresaId(),
                        request.tipoDocumento() != null ? request.tipoDocumento() : empleado.getTipoDocumento(),
                        request.documentoEmp(),
                        id)) {
            log.warn("Documento duplicado {} en empresa ID: {}",
                    request.documentoEmp(), empleado.getEmpresa().getEmpresaId());
            throw new DuplicateDocumentException(ValidationMessages.EMPLEADO_DOCUMENTO_DUPLICATE);
        }

        // Guardar salario anterior
        BigDecimal salarioAnterior = empleado.getSalarioBascMensual();

        // Actualizar campos
        empleadoMapper.updateEntityFromDTO(request, empleado);
//
        // Si cambió el salario, recalcular salario integral y auxilio transporte
        if (request.salarioBascMensual() != null &&
                salarioAnterior.compareTo(request.salarioBascMensual()) != 0) {

            log.info("Salario modificado de {} a {} para empleado ID: {}",
                    salarioAnterior, request.salarioBascMensual(), id);

            if (request.esSalarioIntegral() != null) {
                boolean esSalarioIntegral = Boolean.TRUE.equals(request.esSalarioIntegral());
                if (esSalarioIntegral) {
                    BigDecimal smmlv = salarioCalculator.obtenerSMMLVVigente(LocalDate.now());
                    BigDecimal tope = smmlv.multiply(BigDecimal.valueOf(13));
                    if (request.salarioBascMensual().compareTo(tope) < 0) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "El salario integral debe ser igual o superior a 13 SMMLV ($%s). " +
                                                "El salario ingresado ($%s) no cumple el requisito mínimo legal.",
                                        tope.setScale(0, RoundingMode.HALF_UP),
                                        request.salarioBascMensual().setScale(0, RoundingMode.HALF_UP)
                                )
                        );
                    }
                }
                empleado.setEsSalarioIntegral(esSalarioIntegral);
            }

            crearHistorialSalario(empleado, salarioAnterior, request.salarioBascMensual());
        }

        if (request.tieneAuxTransporte() != null) {
            empleado.setTieneAuxTransporte(request.tieneAuxTransporte());
        }

        // Validación 1: fecha de retiro debe ser posterior a fecha de ingreso
        if (request.fechaRetiroEmp() != null) {
            LocalDate fechaIngreso = empleado.getFechaIngresoEmp();
            if (fechaIngreso != null && !request.fechaRetiroEmp().isAfter(fechaIngreso)) {
                throw new IllegalArgumentException(
                        "La fecha de retiro debe ser posterior a la fecha de ingreso (" + fechaIngreso + ")"
                );
            }

            // Validación 2: fecha de retiro no puede ser futura
            if (request.fechaRetiroEmp().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "La fecha de retiro no puede ser una fecha futura. " +
                                "El empleado solo puede marcarse como retirado a partir de la fecha actual o fechas anteriores"
                );
            }
        }

        if (request.fechaRetiroEmp() != null && empleado.getFechaRetiroEmp() != null) {
            empleado.setEstadoEmp(EstadoEmpleado.RETIRADO);
            log.info("Empleado ID: {} marcado como RETIRADO por fecha de retiro: {}",
                    id, empleado.getFechaRetiroEmp());
        }


        Empleado empleadoActualizado = empleadoRepository.save(empleado);

        log.info("Empleado actualizado exitosamente: {}", empleadoActualizado.getEmpleadoId());

        return empleadoMapper.toResponseDTO(empleadoActualizado);
    }

    @Override
    @Transactional
    public EmpleadoResponseDTO cambiarEstado(Long id, CambiarEstadoRequestDTO request) {
        log.info("Cambiando estado de empleado ID: {} a {}", id, request.nuevoEstado());

        Empleado empleado = empleadoRepository.findByIdActiveWithEmpresa(id)
                .orElseThrow(() -> {
                    log.warn("Empleado no encontrado con ID: {}", id);
                    return new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
                });

        EstadoEmpleado estadoActual = empleado.getEstadoEmp();
        EstadoEmpleado nuevoEstado = request.nuevoEstado();

        // Validar transición de estado
        validarTransicionEstado(estadoActual, nuevoEstado);

        // Actualizar estado
        empleado.setEstadoEmp(nuevoEstado);

        if (estadoActual == EstadoEmpleado.RETIRADO && nuevoEstado == EstadoEmpleado.ACTIVO) {
            empleado.setFechaRetiroEmp(null);
            log.info("Fecha de retiro limpiada para empleado ID: {} al reactivarse", id);
        }


        Empleado empleadoActualizado = empleadoRepository.save(empleado);

        log.info("Estado de empleado {} cambiado de {} a {}",
                id, estadoActual, nuevoEstado);

        return empleadoMapper.toResponseDTO(empleadoActualizado);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando (soft delete) empleado ID: {}", id);

        Empleado empleado = empleadoRepository.findByIdActiveWithEmpresa(id)
                .orElseThrow(() -> {
                    log.warn("Empleado no encontrado con ID: {}", id);
                    return new EmpleadoNotFoundException(ValidationMessages.EMPLEADO_NOT_FOUND);
                });

        // Soft delete
        empleado.softDelete();
        empleadoRepository.save(empleado);

        log.info("Empleado eliminado (soft delete) exitosamente: {}", id);
    }

    /**
     * Valida que la transición de estado sea permitida.
     * Regla: ACTIVO → INACTIVO → RETIRADO
     */
    private void validarTransicionEstado(EstadoEmpleado estadoActual, EstadoEmpleado nuevoEstado) {
        if (estadoActual == nuevoEstado) {
            throw new InvalidStateTransitionException(
                    "El empleado ya se encuentra en estado " + estadoActual
            );
        }

        boolean transicionValida = switch (estadoActual) {
            case ACTIVO   -> nuevoEstado == EstadoEmpleado.INACTIVO;
            case INACTIVO -> nuevoEstado == EstadoEmpleado.ACTIVO;
            case RETIRADO -> nuevoEstado == EstadoEmpleado.ACTIVO;
        };

        if (!transicionValida) {
            throw new InvalidStateTransitionException(
                    ValidationMessages.EMPLEADO_INVALID_STATE_TRANSITION +
                            ": " + estadoActual + " → " + nuevoEstado
            );
        }
    }

    /**
     * Crea un registro en el historial de salarios.
     */
    private void crearHistorialSalario(Empleado empleado, BigDecimal salarioAnterior, BigDecimal salarioActual) {
        HistorialSalario historial = new HistorialSalario();
        historial.setEmpleado(empleado);
        historial.setSalarioAnterior(salarioAnterior);
        historial.setSalarioActual(salarioActual);

        historialSalarioRepository.save(historial);

        log.debug("Historial de salario creado para empleado ID: {} - Anterior: {} - Actual: {}",
                empleado.getEmpleadoId(), salarioAnterior, salarioActual);
    }
}
