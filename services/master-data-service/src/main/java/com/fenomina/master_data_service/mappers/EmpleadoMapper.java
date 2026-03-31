package com.fenomina.master_data_service.mappers;

import com.fenomina.master_data_service.dto.request.EmpleadoRequestDTO;
import com.fenomina.master_data_service.dto.request.EmpleadoUpdateRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoDetalleResponseDTO;
import com.fenomina.master_data_service.dto.response.EmpleadoResponseDTO;
import com.fenomina.master_data_service.entity.Empleado;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {EmpresaMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmpleadoMapper {

    @Mapping(target = "empleadoId", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "esSalarioIntegral", ignore = true)
    @Mapping(target = "tieneAuxTransporte", ignore = true)
    @Mapping(target = "fechaRetiroEmp", ignore = true)
    @Mapping(target = "estadoEmp", constant = "ACTIVO")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Empleado toEntity(EmpleadoRequestDTO dto);

    @Mapping(source = "empresa.empresaId", target = "empresaId")
    @Mapping(source = "empresa.nombreEmpresa", target = "empresaNombre")
    EmpleadoResponseDTO toResponseDTO(Empleado empleado);

    List<EmpleadoResponseDTO> toResponseDTOList(List<Empleado> empleados);

    @Mapping(source = "empresa", target = "empresa")
    EmpleadoDetalleResponseDTO toDetalleResponseDTO(Empleado empleado);

    @Mapping(target = "empleadoId", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "esSalarioIntegral", ignore = true)
    @Mapping(target = "tieneAuxTransporte", ignore = true)
    @Mapping(target = "fechaRetiroEmp", ignore = true)
    @Mapping(target = "estadoEmp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDTO(EmpleadoUpdateRequestDTO dto, @MappingTarget Empleado empleado);
}