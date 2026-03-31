package com.fenomina.master_data_service.service;

import com.fenomina.master_data_service.config.FileStorageProperties;
import com.fenomina.master_data_service.exceptions.FileStorageException;
import com.fenomina.master_data_service.util.FileUtils;
import com.fenomina.master_data_service.util.ValidationMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;

    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Directorio de almacenamiento creado/verificado: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Error al crear directorio de almacenamiento", ex);
            throw new FileStorageException("No se pudo crear el directorio de almacenamiento", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, Long empresaId) {
        // Validar que el archivo no esté vacío
        if (FileUtils.isFileEmpty(file)) {
            throw new FileStorageException(ValidationMessages.FILE_EMPTY);
        }

        // Validar tamaño del archivo
        if (!FileUtils.isFileSizeValid(file, fileStorageProperties.getMaxFileSize())) {
            throw new FileStorageException(
                    ValidationMessages.FILE_TOO_LARGE + " (máximo: " +
                            (fileStorageProperties.getMaxFileSize() / 1024 / 1024) + " MB)"
            );
        }

        // Validar extensión del archivo
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !FileUtils.hasAllowedExtension(
                originalFilename,
                fileStorageProperties.getAllowedExtensions()
        )) {
            throw new FileStorageException(
                    ValidationMessages.FILE_INVALID_EXTENSION +
                            String.join(", ", fileStorageProperties.getAllowedExtensions())
            );
        }

        try {
            // Generar nombre de archivo basado en el ID de la empresa
            String fileName = FileUtils.generateEmpresaLogoFilename(empresaId, originalFilename);

            // Crear subdirectorio para la empresa si no existe
            Path empresaDir = this.fileStorageLocation.resolve("empresa_" + empresaId);
            Files.createDirectories(empresaDir);

            // Ruta completa del archivo
            Path targetLocation = empresaDir.resolve(fileName);

            // Copiar archivo al destino (reemplaza si ya existe)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retornar ruta relativa
            String relativePath = "empresa_" + empresaId + "/" + fileName;

            log.info("Archivo almacenado exitosamente: {}", relativePath);

            return relativePath;

        } catch (IOException ex) {
            log.error("Error al almacenar archivo para empresa {}", empresaId, ex);
            throw new FileStorageException(ValidationMessages.FILE_UPLOAD_ERROR, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Archivo cargado: {}", fileName);
                return resource;
            } else {
                log.warn("Archivo no encontrado o no legible: {}", fileName);
                throw new FileStorageException("Archivo no encontrado: " + fileName);
            }
        } catch (MalformedURLException ex) {
            log.error("Error al cargar archivo: {}", fileName, ex);
            throw new FileStorageException("Archivo no encontrado: " + fileName, ex);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Intento de eliminar archivo con nombre nulo o vacío");
            return;
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Archivo eliminado: {}", fileName);
            } else {
                log.warn("Intento de eliminar archivo inexistente: {}", fileName);
            }
        } catch (IOException ex) {
            log.error("Error al eliminar archivo: {}", fileName, ex);
            throw new FileStorageException(ValidationMessages.FILE_DELETE_ERROR, ex);
        }
    }
}