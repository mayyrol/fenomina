package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.service.ConceptoNominaInternalService;
import com.fenomina.master_data_service.dto.response.ConceptoNominaInternalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/master/internal")
@RequiredArgsConstructor
@Slf4j
public class ConceptoNominaInternalController {

    private final ConceptoNominaInternalService conceptoNominaInternalService;

    @GetMapping("/conceptos-nomina")
    public ResponseEntity<List<ConceptoNominaInternalDTO>> findAll() {
        log.debug("Petición interna: catálogo de conceptos de nómina");
        return ResponseEntity.ok(conceptoNominaInternalService.findAll());
    }
}
