package com.fenomina.master_data_service.dto.response;

public record ConceptoNominaResponseDTO(
        Long conceptoNominaId,
        String nombreConcepNomina,
        String descrConcepNomina,
        String categoriaConcNomina
) {}
