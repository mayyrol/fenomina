package com.fenomina.master_data_service.service;
import com.fenomina.master_data_service.dto.response.ConceptoNominaInternalDTO;
import com.fenomina.master_data_service.entity.ConceptoNomina;
import com.fenomina.master_data_service.repository.ConceptoNominaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConceptoNominaInternalService {

    private final ConceptoNominaRepository conceptoNominaRepository;

    @Transactional(readOnly = true)
    public List<ConceptoNominaInternalDTO> findAll() {
        log.debug("Consultando catálogo completo de conceptos de nómina para uso interno");
        return conceptoNominaRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ConceptoNominaInternalDTO toDTO(ConceptoNomina concepto) {
        return new ConceptoNominaInternalDTO(
                concepto.getConcepNominaId(),
                concepto.getNombreConcepNomina(),
                concepto.getCategoriaConcNomina() != null
                        ? concepto.getCategoriaConcNomina().name()
                        : null,
                concepto.getEsSalario(),
                concepto.getEsIbc(),
                concepto.getEsInformativo()
        );
    }
}
