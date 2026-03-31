package com.fenomina.master_data_service.mappers;

import com.fenomina.master_data_service.dto.response.HistorialSalarioResponseDTO;
import com.fenomina.master_data_service.entity.HistorialSalario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface HistorialSalarioMapper {

    @Mapping(source = "empleado.empleadoId", target = "empleadoId")
    @Mapping(source = "empleado", target = "empleadoNombreCompleto", qualifiedByName = "getNombreCompleto")
    @Mapping(target = "diferencia", ignore = true)
    @Mapping(source = "createdBy", target = "creadoPor", qualifiedByName = "userIdToString")
    HistorialSalarioResponseDTO toResponseDTO(HistorialSalario historialSalario);

    List<HistorialSalarioResponseDTO> toResponseDTOList(List<HistorialSalario> historial);

    @Named("getNombreCompleto")
    default String getNombreCompleto(com.fenomina.master_data_service.entity.Empleado empleado) {
        if (empleado == null) {
            return null;
        }
        return empleado.getNombresEmp() + " " + empleado.getApellidosEmp();
    }

    @Named("userIdToString")
    default String userIdToString(Long userId) {
        return userId != null ? "Usuario ID: " + userId : "Sistema";
    }
}