package org.example.hive.service;

import org.example.hive.config.AppEnums.AttachmentContext;
import org.example.hive.dto.response.AttachmentResponse;
import org.example.hive.exception.AttachmentException;
import org.example.hive.model.Attachment;
import org.example.hive.model.Comment;
import org.example.hive.model.Company;
import org.example.hive.model.Event;
import org.example.hive.model.Message;
import org.example.hive.model.Post;
import org.example.hive.model.User;
import org.example.hive.repository.AttachmentRepository;
import org.example.hive.repository.CommentRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.EventRepository;
import org.example.hive.repository.MessageRepository;
import org.example.hive.repository.PostRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.security.Permissions;
import org.example.hive.storage.StorageService;
import org.example.hive.storage.StorageService.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final int MAX_ATTACHMENTS_PER_POST = 10;
    private static final int MAX_ATTACHMENTS_PER_COMMENT = 5;
    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MessageRepository messageRepository;
    private final StorageService storageService;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             UserRepository userRepository,
                             CompanyRepository companyRepository,
                             EventRepository eventRepository,
                             PostRepository postRepository,
                             CommentRepository commentRepository,
                             MessageRepository messageRepository,
                             StorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
        this.storageService = storageService;
    }

    @Transactional
    public AttachmentResponse uploadAvatar(AuthUserPrincipal principal, MultipartFile file) {
        Long companyId = requireCompany(principal);

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
                .attachmentType(stored.attachmentType())
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

    @Transactional
    public AttachmentResponse uploadEventCover(AuthUserPrincipal principal, Long eventId, MultipartFile file) {
        Long companyId = requireCompany(principal);

        Event event = eventRepository.findByIdAndCompany_Id(eventId, companyId)
                .orElseThrow(() -> new AttachmentException("Event not found", HttpStatus.NOT_FOUND));
        assertCanManageEvent(event, principal.getUserId());

        User uploader = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AttachmentException("Company not found", HttpStatus.NOT_FOUND));

        Attachment old = event.getCover();

        StoredFile stored = storageService.store(file, companyId, AttachmentContext.EVENT_COVER);
        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .company(company)
                .uploadedBy(uploader)
                .context(AttachmentContext.EVENT_COVER)
                .attachmentType(stored.attachmentType())
                .storageKey(stored.storageKey())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .build());

        event.setCover(attachment);
        eventRepository.save(event);

        if (old != null) {
            storageService.delete(old.getStorageKey());
            attachmentRepository.delete(old);
        }

        return toResponse(attachment);
    }

    @Transactional
    public void deleteEventCover(AuthUserPrincipal principal, Long eventId) {
        Long companyId = requireCompany(principal);

        Event event = eventRepository.findByIdAndCompany_Id(eventId, companyId)
                .orElseThrow(() -> new AttachmentException("Event not found", HttpStatus.NOT_FOUND));
        assertCanManageEvent(event, principal.getUserId());

        Attachment cover = event.getCover();
        if (cover == null) {
            return;
        }

        event.setCover(null);
        eventRepository.save(event);

        storageService.delete(cover.getStorageKey());
        attachmentRepository.delete(cover);
    }

    @Transactional
    public AttachmentResponse uploadPostAttachment(AuthUserPrincipal principal, Long postId, MultipartFile file) {
        Long companyId = requireCompany(principal);

        Post post = postRepository.findByIdAndCompany_IdAndActiveTrue(postId, companyId)
                .orElseThrow(() -> new AttachmentException("Post not found", HttpStatus.NOT_FOUND));

        if (!post.getAuthor().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the author can add attachments to this post", HttpStatus.FORBIDDEN);
        }
        if (attachmentRepository.countByPost_Id(postId) >= MAX_ATTACHMENTS_PER_POST) {
            throw new AttachmentException(
                    "A post can have at most " + MAX_ATTACHMENTS_PER_POST + " attachments", HttpStatus.BAD_REQUEST);
        }

        User uploader = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AttachmentException("Company not found", HttpStatus.NOT_FOUND));

        StoredFile stored = storageService.store(file, companyId, AttachmentContext.POST);
        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .company(company)
                .uploadedBy(uploader)
                .context(AttachmentContext.POST)
                .attachmentType(stored.attachmentType())
                .storageKey(stored.storageKey())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .post(post)
                .build());

        return toResponse(attachment);
    }

    @Transactional
    public void deletePostAttachment(AuthUserPrincipal principal, Long postId, Long attachmentId) {
        Long companyId = requireCompany(principal);

        Post post = postRepository.findByIdAndCompany_IdAndActiveTrue(postId, companyId)
                .orElseThrow(() -> new AttachmentException("Post not found", HttpStatus.NOT_FOUND));

        if (!post.getAuthor().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the author can remove attachments from this post", HttpStatus.FORBIDDEN);
        }

        Attachment attachment = attachmentRepository.findByIdAndPost_Id(attachmentId, postId)
                .orElseThrow(() -> new AttachmentException("Attachment not found", HttpStatus.NOT_FOUND));

        storageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public AttachmentResponse uploadCommentAttachment(AuthUserPrincipal principal, Long commentId, MultipartFile file) {
        Long companyId = requireCompany(principal);

        Comment comment = commentRepository.findByIdAndActiveTrue(commentId)
                .orElseThrow(() -> new AttachmentException("Comment not found", HttpStatus.NOT_FOUND));
        assertSameCompany(comment, companyId);

        if (!comment.getUser().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the comment author can add attachments", HttpStatus.FORBIDDEN);
        }
        if (attachmentRepository.countByComment_Id(commentId) >= MAX_ATTACHMENTS_PER_COMMENT) {
            throw new AttachmentException(
                    "A comment can have at most " + MAX_ATTACHMENTS_PER_COMMENT + " attachments", HttpStatus.BAD_REQUEST);
        }

        User uploader = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AttachmentException("Company not found", HttpStatus.NOT_FOUND));

        StoredFile stored = storageService.store(file, companyId, AttachmentContext.COMMENT);
        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .company(company)
                .uploadedBy(uploader)
                .context(AttachmentContext.COMMENT)
                .attachmentType(stored.attachmentType())
                .storageKey(stored.storageKey())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .comment(comment)
                .build());

        return toResponse(attachment);
    }

    @Transactional
    public void deleteCommentAttachment(AuthUserPrincipal principal, Long commentId, Long attachmentId) {
        Long companyId = requireCompany(principal);

        Comment comment = commentRepository.findByIdAndActiveTrue(commentId)
                .orElseThrow(() -> new AttachmentException("Comment not found", HttpStatus.NOT_FOUND));
        assertSameCompany(comment, companyId);

        if (!comment.getUser().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the comment author can remove attachments", HttpStatus.FORBIDDEN);
        }

        Attachment attachment = attachmentRepository.findByIdAndComment_Id(attachmentId, commentId)
                .orElseThrow(() -> new AttachmentException("Attachment not found", HttpStatus.NOT_FOUND));

        storageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public AttachmentResponse uploadMessageAttachment(AuthUserPrincipal principal, Long messageId, MultipartFile file) {
        Long companyId = requireCompany(principal);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AttachmentException("Message not found", HttpStatus.NOT_FOUND));
        assertSameCompany(message, companyId);

        if (!message.getSender().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the sender can add attachments to this message", HttpStatus.FORBIDDEN);
        }
        if (attachmentRepository.countByMessage_Id(messageId) >= MAX_ATTACHMENTS_PER_MESSAGE) {
            throw new AttachmentException(
                    "A message can have at most " + MAX_ATTACHMENTS_PER_MESSAGE + " attachments", HttpStatus.BAD_REQUEST);
        }

        User uploader = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AttachmentException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AttachmentException("Company not found", HttpStatus.NOT_FOUND));

        StoredFile stored = storageService.store(file, companyId, AttachmentContext.CHAT_MESSAGE);
        Attachment attachment = attachmentRepository.save(Attachment.builder()
                .company(company)
                .uploadedBy(uploader)
                .context(AttachmentContext.CHAT_MESSAGE)
                .attachmentType(stored.attachmentType())
                .storageKey(stored.storageKey())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .message(message)
                .build());

        return toResponse(attachment);
    }

    @Transactional
    public void deleteMessageAttachment(AuthUserPrincipal principal, Long messageId, Long attachmentId) {
        Long companyId = requireCompany(principal);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AttachmentException("Message not found", HttpStatus.NOT_FOUND));
        assertSameCompany(message, companyId);

        if (!message.getSender().getId().equals(principal.getUserId())) {
            throw new AttachmentException("Only the sender can remove attachments from this message", HttpStatus.FORBIDDEN);
        }

        Attachment attachment = attachmentRepository.findByIdAndMessage_Id(attachmentId, messageId)
                .orElseThrow(() -> new AttachmentException("Attachment not found", HttpStatus.NOT_FOUND));

        storageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
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

    private Long requireCompany(AuthUserPrincipal principal) {
        if (principal.getCompanyId() == null) {
            throw new AttachmentException("You must belong to a company to upload a file", HttpStatus.BAD_REQUEST);
        }
        return principal.getCompanyId();
    }

    private void assertCanManageEvent(Event event, Long requesterId) {
        if (event.getCreatedBy().getId().equals(requesterId)) {
            return;
        }
        boolean hasOverride = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Permissions.EVENT_UPDATE::equals);
        if (!hasOverride) {
            throw new AttachmentException("Only the event owner or a manager can update the cover", HttpStatus.FORBIDDEN);
        }
    }

    private void assertSameCompany(Comment comment, Long companyId) {
        if (!comment.getPost().getCompany().getId().equals(companyId)) {
            throw new AttachmentException("Comment not found", HttpStatus.NOT_FOUND);
        }
    }

    private void assertSameCompany(Message message, Long companyId) {
        if (!message.getConversation().getCompany().getId().equals(companyId)) {
            throw new AttachmentException("Message not found", HttpStatus.NOT_FOUND);
        }
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                "/attachments/" + attachment.getId(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getAttachmentType());
    }
}