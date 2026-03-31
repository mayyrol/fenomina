package com.fenomina.master_data_service.controller;

import com.fenomina.master_data_service.dto.request.ParametroGeneralRequestDTO;
import com.fenomina.master_data_service.dto.response.ParametroGeneralResponseDTO;
import com.fenomina.master_data_service.service.ParametroGeneralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master/parametros")
@RequiredArgsConstructor
@Slf4j
public class ParametroGeneralController {

    private final ParametroGeneralService parametroGeneralService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ParametroGeneralResponseDTO> create(
            @Valid @RequestBody ParametroGeneralRequestDTO request) {

        log.info("Solicitud de creación de parámetro: {}", request.nombreParamGeneral());

        ParametroGeneralResponseDTO response = parametroGeneralService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<ParametroGeneralResponseDTO>> findAll() {
        log.debug("Listando todos los parámetros generales");

        List<ParametroGeneralResponseDTO> parametros = parametroGeneralService.findAll();

        return ResponseEntity.ok(parametros);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParametroGeneralResponseDTO> findById(@PathVariable Long id) {
        log.debug("Consultando parámetro ID: {}", id);

        ParametroGeneralResponseDTO parametro = parametroGeneralService.findById(id);

        return ResponseEntity.ok(parametro);
    }
}
