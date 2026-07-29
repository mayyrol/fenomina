package com.fenomina.payroll_engine.service.proceso;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public ResultadoEnvioCorreo enviar(
            String destinatario,
            String asunto,
            String cuerpoHtml,
            byte[] adjuntoPdf,
            String nombreArchivoAdjunto
    ) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            if (adjuntoPdf != null && adjuntoPdf.length > 0) {
                helper.addAttachment(nombreArchivoAdjunto, new ByteArrayResource(adjuntoPdf));
            }

            mailSender.send(mensaje);

            log.info("Correo enviado exitosamente a: {}", destinatario);
            return new ResultadoEnvioCorreo(true, null);

        } catch (Exception ex) {
            log.error("Error enviando correo a {}: {}", destinatario, ex.getMessage());
            return new ResultadoEnvioCorreo(false, ex.getMessage());
        }
    }

    public record ResultadoEnvioCorreo(boolean exitoso, String mensajeError) {}
}