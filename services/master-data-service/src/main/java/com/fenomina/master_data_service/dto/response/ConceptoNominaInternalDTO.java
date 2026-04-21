package com.fenomina.master_data_service.dto.response;

public record ConceptoNominaInternalDTO(
        Long concepNominaId,
        String nombreConcepNomina,
        String categoriaConcNomina,
        Boolean esSalario,
        Boolean esIbc,
        Boolean esInformativo
) {}
