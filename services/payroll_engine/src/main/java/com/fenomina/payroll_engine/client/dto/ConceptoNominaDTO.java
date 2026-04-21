package com.fenomina.payroll_engine.client.dto;

public record ConceptoNominaDTO(
        Long concepNominaId,
        String nombreConcepNomina,
        String categoriaConcNomina,
        Boolean esSalario,
        Boolean esIbc,
        Boolean esInformativo
) {}