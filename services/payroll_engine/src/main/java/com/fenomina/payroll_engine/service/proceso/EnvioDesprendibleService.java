package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.dto.response.EnvioDesprendibleResponseDTO;
import com.fenomina.payroll_engine.dto.response.PreviewEnvioResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface EnvioDesprendibleService {

    PreviewEnvioResponseDTO obtenerPreview(Long procesoId);

    EnvioDesprendibleResponseDTO enviar(Long procesoId, Long usuarioId, MultipartFile desprendiblePdf);
}