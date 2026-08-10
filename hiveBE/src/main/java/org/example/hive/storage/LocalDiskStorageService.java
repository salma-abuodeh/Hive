package org.example.hive.storage;

import org.example.hive.config.AppEnums.AttachmentContext;
import org.example.hive.config.AppEnums.AttachmentType;
import org.example.hive.exception.AttachmentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalDiskStorageService implements StorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final Set<AttachmentContext> IMAGE_ONLY_CONTEXTS =
            Set.of(AttachmentContext.AVATAR, AttachmentContext.EVENT_COVER);

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    @Value("${app.storage.local.base-dir}")
    private String baseDir;

    @Override
    public StoredFile store(MultipartFile file, Long companyId, AttachmentContext context) {
        AttachmentType type = validate(file, context);

        String extension = extensionOf(file.getOriginalFilename());
        String storageKey = companyId + "/" + context.name().toLowerCase() + "/" + UUID.randomUUID() + extension;

        Path target = basePath().resolve(storageKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new AttachmentException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new StoredFile(storageKey, file.getOriginalFilename(), file.getContentType(), file.getSize(), type);
    }

    @Override
    public Resource load(String storageKey) {
        Path path = basePath().resolve(storageKey).normalize();
        if (!path.startsWith(basePath())) {
            throw new AttachmentException("Invalid storage key", HttpStatus.BAD_REQUEST);
        }

        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new AttachmentException("File not found", HttpStatus.NOT_FOUND);
        }
        return resource;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(basePath().resolve(storageKey).normalize());
        } catch (IOException ignored) {
        }
    }

    private AttachmentType validate(MultipartFile file, AttachmentContext context) {
        if (file == null || file.isEmpty()) {
            throw new AttachmentException("File is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new AttachmentException("File exceeds 5MB limit", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        AttachmentType type = resolveType(contentType);

        if (IMAGE_ONLY_CONTEXTS.contains(context) && type != AttachmentType.IMAGE) {
            throw new AttachmentException("Only JPEG, PNG, WEBP, or GIF images are allowed here", HttpStatus.BAD_REQUEST);
        }

        return type;
    }

    private AttachmentType resolveType(String contentType) {
        if (contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return AttachmentType.IMAGE;
        }
        if (contentType != null && ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            return AttachmentType.DOCUMENT;
        }
        throw new AttachmentException(
                "Unsupported file type. Allowed: JPEG, PNG, WEBP, GIF, PDF, DOC, DOCX, XLS, XLSX",
                HttpStatus.BAD_REQUEST);
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private Path basePath() {
        return Path.of(baseDir).normalize();
    }
}