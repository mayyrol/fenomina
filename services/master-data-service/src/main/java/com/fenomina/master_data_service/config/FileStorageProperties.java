package com.fenomina.master_data_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Getter
@Setter
public class FileStorageProperties {

    /**
     * Directorio base donde se almacenan los archivos subidos.
     * logos -> /opt/nomina-app/uploads/logos (Linux)
     */
    private String dir;

    private long maxFileSize = 5242880;

    private String[] allowedExtensions = {"png", "jpg", "jpeg"};
}
