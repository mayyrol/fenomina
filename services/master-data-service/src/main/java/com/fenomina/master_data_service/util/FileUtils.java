package com.fenomina.master_data_service.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.UUID;

public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException("Clase de utilidades");
    }

    public static boolean isFileEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static boolean hasAllowedExtension(String filename, String[] allowedExtensions) {
        String extension = getFileExtension(filename);
        return Arrays.asList(allowedExtensions).contains(extension);
    }

    public static String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    public static String generateEmpresaLogoFilename(Long empresaId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return "logo_" + empresaId + "." + extension;
    }

    public static boolean isFileSizeValid(MultipartFile file, long maxSizeInBytes) {
        return file != null && file.getSize() <= maxSizeInBytes;
    }
}
