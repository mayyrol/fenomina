package com.fenomina.master_data_service.mappers;

import com.fenomina.master_data_service.dto.request.ContratoConceptoRequestDTO;
import com.fenomina.master_data_service.dto.response.ContratoConceptoResponseDTO;
import com.fenomina.master_data_service.entity.ContratoConcepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ContratoConceptoMapper {

    @Mapping(target = "contratoConceptId", ignore = true)
    @Mapping(target = "empleado", ignore = true)
    @Mapping(target = "conceptoNomina", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ContratoConcepto toEntity(ContratoConceptoRequestDTO dto);

    @Mapping(source = "empleado.empleadoId", target = "empleadoId")
    @Mapping(source = "empleado", target = "empleadoNombreCompleto", qualifiedByName = "getNombreCompleto")
    @Mapping(source = "empleado.documentoEmp", target = "empleadoDocumento")
    @Mapping(source = "conceptoNomina.concepNominaId", target = "conceptoNominaId")
    @Mapping(source = "conceptoNomina.nombreConcepNomina", target = "conceptoNombre")
    @Mapping(source = "conceptoNomina.categoriaConcNomina", target = "conceptoCategoria")
    ContratoConceptoResponseDTO toResponseDTO(ContratoConcepto contratoConcepto);

    List<ContratoConceptoResponseDTO> toResponseDTOList(List<ContratoConcepto> contratos);

    @Named("getNombreCompleto")
    default String getNombreCompleto(com.fenomina.master_data_service.entity.Empleado empleado) {
        if (empleado == null) {
            return null;
        }
        return empleado.getNombresEmp() + " " + empleado.getApellidosEmp();
    }
}
