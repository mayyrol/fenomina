package com.fenomina.master_data_service.mappers;

import com.fenomina.master_data_service.dto.request.ParametroGeneralRequestDTO;
import com.fenomina.master_data_service.dto.response.ParametroGeneralResponseDTO;
import com.fenomina.master_data_service.entity.ParametroGeneral;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ParametroGeneralMapper {

    @Mapping(target = "paramGeneralId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    ParametroGeneral toEntity(ParametroGeneralRequestDTO dto);

    ParametroGeneralResponseDTO toResponseDTO(ParametroGeneral parametroGeneral);

    List<ParametroGeneralResponseDTO> toResponseDTOList(List<ParametroGeneral> parametros);
}
