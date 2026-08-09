package org.example.hive.storage;

import org.example.hive.config.AppEnums.AttachmentContext;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile store(MultipartFile file, Long companyId, AttachmentContext context);

    Resource load(String storageKey);

    void delete(String storageKey);

    record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
    }
}