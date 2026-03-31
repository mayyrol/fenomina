package com.fenomina.master_data_service.mappers;

import com.fenomina.master_data_service.dto.request.EmpresaRequestDTO;
import com.fenomina.master_data_service.dto.response.EmpresaBasicInfoDTO;
import com.fenomina.master_data_service.dto.response.EmpresaResponseDTO;
import com.fenomina.master_data_service.entity.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmpresaMapper {

    @Mapping(target = "empresaId", ignore = true)
    @Mapping(target = "logoEmpresaUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Empresa toEntity(EmpresaRequestDTO dto);

    EmpresaResponseDTO toResponseDTO(Empresa empresa);

    List<EmpresaResponseDTO> toResponseDTOList(List<Empresa> empresas);

    EmpresaBasicInfoDTO toBasicInfoDTO(Empresa empresa);

    @Mapping(target = "empresaId", ignore = true)
    @Mapping(target = "logoEmpresaUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDTO(EmpresaRequestDTO dto, @MappingTarget Empresa empresa);
}
