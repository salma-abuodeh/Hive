package org.example.hive.service;

import org.example.hive.config.AppEnums.AttachmentContext;
import org.example.hive.dto.response.AttachmentResponse;
import org.example.hive.exception.AttachmentException;
import org.example.hive.model.Attachment;
import org.example.hive.model.Company;
import org.example.hive.model.User;
import org.example.hive.repository.AttachmentRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.storage.StorageService;
import org.example.hive.storage.StorageService.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             UserRepository userRepository,
                             CompanyRepository companyRepository,
                             StorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.storageService = storageService;
    }

    @Transactional
    public AttachmentResponse uploadAvatar(AuthUserPrincipal principal, MultipartFile file) {
        Long companyId = principal.getCompanyId();
        if (companyId == null) {
            throw new AttachmentException("You must belong to a company to upload a photo", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AttachmentException("Company not found", HttpStatus.NOT_FOUND));

        Attachment old = user.getAvatar();

        StoredFile stored = storageService.store(file, companyId, AttachmentContext.AVATAR);
        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .company(company)
                .uploadedBy(user)
                .context(AttachmentContext.AVATAR)
                .storageKey(stored.storageKey())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .build());

        user.setAvatar(attachment);
        userRepository.save(user);

        if (old != null) {
            storageService.delete(old.getStorageKey());
            attachmentRepository.delete(old);
        }

        return toResponse(attachment);
    }

    @Transactional
    public void deleteAvatar(AuthUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));

        Attachment avatar = user.getAvatar();
        if (avatar == null) {
            return;
        }

        user.setAvatar(null);
        userRepository.save(user);

        storageService.delete(avatar.getStorageKey());
        attachmentRepository.delete(avatar);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long attachmentId, Long companyId) {
        if (companyId == null) {
            throw new AttachmentException("Attachment not found", HttpStatus.NOT_FOUND);
        }

        Attachment attachment = attachmentRepository.findByIdAndCompany_Id(attachmentId, companyId)
                .orElseThrow(() -> new AttachmentException("Attachment not found", HttpStatus.NOT_FOUND));

        Resource resource = storageService.load(attachment.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                "/attachments/" + attachment.getId(),
                attachment.getContentType(),
                attachment.getSizeBytes());
    }
}