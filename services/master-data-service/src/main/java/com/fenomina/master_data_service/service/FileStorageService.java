package com.fenomina.master_data_service.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file, Long empresaId);

    Resource loadFileAsResource(String fileName);

    void deleteFile(String fileName);
}
