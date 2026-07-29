package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.client.MasterDataClientWrapper;
import com.fenomina.payroll_engine.client.dto.EmpresaCorreoDTO;
import com.fenomina.payroll_engine.client.dto.EmpresaDTO;
import com.fenomina.payroll_engine.dto.response.EnvioDesprendibleResponseDTO;
import com.fenomina.payroll_engine.dto.response.PreviewEnvioResponseDTO;
import com.fenomina.payroll_engine.entity.EnvioDesprendible;
import com.fenomina.payroll_engine.entity.EnvioDesprendibleDetalle;
import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoDetalleEnvio;
import com.fenomina.payroll_engine.enums.EstadoEnvio;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import com.fenomina.payroll_engine.exception.InvalidStateTransitionException;
import com.fenomina.payroll_engine.exception.SinCorreosRegistradosException;
import com.fenomina.payroll_engine.repository.EnvioDesprendibleDetalleRepository;
import com.fenomina.payroll_engine.repository.EnvioDesprendibleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvioDesprendibleServiceImpl implements EnvioDesprendibleService {

    private static final String NOMBRE_MES_ARRAY_KEY = "";

    private final ProcesoLiquidacionService procesoService;
    private final MasterDataClientWrapper masterDataClient;
    private final EmailService emailService;
    private final EnvioDesprendibleRepository envioRepository;
    private final EnvioDesprendibleDetalleRepository detalleRepository;

    @Override
    public PreviewEnvioResponseDTO obtenerPreview(Long procesoId) {
        ProcesoLiquidacion proceso = procesoService.findById(procesoId);
        validarEstadoPagado(proceso);

        EmpresaDTO empresa = masterDataClient.findEmpresaById(proceso.getFkIdEmpresa());
        List<String> correos = extraerCorreos(empresa);

        String asunto = construirAsunto(proceso, empresa);
        String cuerpo = construirCuerpo(proceso, empresa);

        return new PreviewEnvioResponseDTO(correos, asunto, cuerpo);
    }

    @Override
    @Transactional
    public EnvioDesprendibleResponseDTO enviar(Long procesoId, Long usuarioId, MultipartFile desprendiblePdf) {
        ProcesoLiquidacion proceso = procesoService.findById(procesoId);
        validarEstadoPagado(proceso);

        EmpresaDTO empresa = masterDataClient.findEmpresaById(proceso.getFkIdEmpresa());
        List<String> correos = extraerCorreos(empresa);

        String asunto = construirAsunto(proceso, empresa);
        String cuerpo = construirCuerpo(proceso, empresa);

        byte[] pdfBytes;
        try {
            pdfBytes = desprendiblePdf.getBytes();
        } catch (java.io.IOException ex) {
            throw new RuntimeException("No se pudo leer el archivo PDF adjunto", ex);
        }
        String nombreArchivo = desprendiblePdf.getOriginalFilename() != null
                ? desprendiblePdf.getOriginalFilename()
                : "desprendibles.pdf";

        EnvioDesprendible envio = new EnvioDesprendible();
        envio.setFkProcesoLiquiId(procesoId);
        envio.setFkUsuarioId(usuarioId);
        envio.setAsuntoCorreo(asunto);
        envio.setEstadoEnvio(EstadoEnvio.PENDIENTE);
        envio.setFechaEnvio(LocalDateTime.now());
        EnvioDesprendible envioGuardado = envioRepository.save(envio);

        int exitosos = 0;
        List<EnvioDesprendibleDetalle> detalles = new ArrayList<>();

        for (String correo : correos) {
            EmailService.ResultadoEnvioCorreo resultado =
                    emailService.enviar(correo, asunto, cuerpo, pdfBytes, nombreArchivo);

            EnvioDesprendibleDetalle detalle = new EnvioDesprendibleDetalle();
            detalle.setFkEnvioDesprendibleId(envioGuardado.getEnvioDesprendibleId());
            detalle.setCorreoDestino(correo);

            if (resultado.exitoso()) {
                detalle.setEstadoDetalle(EstadoDetalleEnvio.ENVIADO);
                exitosos++;
            } else {
                detalle.setEstadoDetalle(EstadoDetalleEnvio.FALLIDO);
                detalle.setMensajeError(resultado.mensajeError());
            }
            detalles.add(detalleRepository.save(detalle));
        }

        EstadoEnvio estadoFinal;
        if (exitosos == correos.size()) {
            estadoFinal = EstadoEnvio.ENVIADO;
        } else if (exitosos == 0) {
            estadoFinal = EstadoEnvio.FALLIDO;
        } else {
            estadoFinal = EstadoEnvio.PARCIAL;
        }

        envioGuardado.setEstadoEnvio(estadoFinal);
        envioGuardado = envioRepository.save(envioGuardado);

        log.info("Envío de desprendibles proceso {}: {} exitosos de {} - estado final: {}",
                procesoId, exitosos, correos.size(), estadoFinal);

        return EnvioDesprendibleResponseDTO.desde(envioGuardado, detalles);
    }


    private void validarEstadoPagado(ProcesoLiquidacion proceso) {
        if (proceso.getEstadoProcNomina() != EstadoProceso.PAGADO) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden enviar desprendibles de procesos en estado Finalizado (Pagado)."
            );
        }
    }

    private List<String> extraerCorreos(EmpresaDTO empresa) {
        List<EmpresaCorreoDTO> correosDTO = empresa.correos();
        if (correosDTO == null || correosDTO.isEmpty()) {
            throw new SinCorreosRegistradosException(
                    "Esta empresa no tiene correos de notificación registrados. " +
                            "Dirígete a Información de la empresa para registrar al menos uno."
            );
        }
        return correosDTO.stream().map(EmpresaCorreoDTO::correo).toList();
    }

    private static final String[] NOMBRE_MES = {
            "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private String etiquetaPeriodo(ProcesoLiquidacion proceso) {
        if (proceso.getTipoProceso() == TipoProceso.NOMINA_MENSUAL ||
                proceso.getTipoProceso() == TipoProceso.NOMINA_QUINCENAL) {
            Integer periodo = proceso.getPeriodo();
            if (periodo != null && periodo >= 1 && periodo <= 12) {
                return NOMBRE_MES[periodo];
            }
        }
        if (proceso.getTipoProceso() == TipoProceso.PRIMA_SEMESTRAL) {
            return proceso.getPeriodo() != null && proceso.getPeriodo() == 1
                    ? "Primer semestre"
                    : "Segundo semestre";
        }
        return null;
    }

    private String construirAsunto(ProcesoLiquidacion proceso, EmpresaDTO empresa) {
        String tipoLabel = etiquetaTipoProceso(proceso.getTipoProceso());
        String periodoTexto = etiquetaPeriodo(proceso);
        String periodoCompleto = periodoTexto != null
                ? String.format("%s %d", periodoTexto, proceso.getAnio())
                : String.format("Año %d", proceso.getAnio());
        return String.format("%s - %s - %s", tipoLabel, empresa.nombreEmpresa(), periodoCompleto);
    }

    private String construirCuerpo(ProcesoLiquidacion proceso, EmpresaDTO empresa) {
        String tipoLabel = etiquetaTipoProceso(proceso.getTipoProceso());
        String periodoTexto = etiquetaPeriodo(proceso);
        String periodoCompleto = periodoTexto != null
                ? String.format("%s %d", periodoTexto, proceso.getAnio())
                : String.format("año %d", proceso.getAnio());
        return """
            <p>Estimados, %s 👋</p>
            <p>📎 Adjunto encontrarán los <strong>%s</strong> correspondientes
            al Periodo %s de la empresa <strong>%s</strong>.</p>
            <p>✉️ Este es un mensaje automático, por favor no responder directamente a este correo.</p>
            <p>🔧 En caso de solicitud de cambios o ajustes, por favor comunicarse con el Departamento de Recursos Humanos a través de:</p>
            <p>
                📱 WhatsApp: <strong>+57 316 564 8415</strong><br/>
                📧 Correo: <strong>seguridadsfuncion@gmail.com</strong>
            </p>
            <p>Saludos, 🙌<br/>Función Empresarial SAS.</p>
            """.formatted(empresa.nombreEmpresa(), tipoLabel, periodoCompleto, empresa.nombreEmpresa());
    }

    private String etiquetaTipoProceso(TipoProceso tipoProceso) {
        return switch (tipoProceso) {
            case NOMINA_MENSUAL, NOMINA_QUINCENAL -> "Desprendibles de Nómina";
            case PRIMA_SEMESTRAL -> "Desprendibles de Prima";
            case CESANTIAS_ANUAL -> "Desprendibles de Cesantías e Intereses";
            case INTERESES_CESANTIAS_ANUAL -> "Desprendibles de Intereses sobre Cesantías";
        };
    }
}
