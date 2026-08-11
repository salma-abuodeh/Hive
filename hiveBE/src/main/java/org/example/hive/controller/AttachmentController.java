package org.example.hive.controller;

import org.example.hive.dto.response.AttachmentResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/users/me/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public AttachmentResponse uploadAvatar(@AuthenticationPrincipal AuthUserPrincipal principal,
                                           @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadAvatar(principal, file);
    }

    @DeleteMapping("/users/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(@AuthenticationPrincipal AuthUserPrincipal principal) {
        attachmentService.deleteAvatar(principal);
    }

    @PostMapping(value = "/events/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public AttachmentResponse uploadEventCover(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthUserPrincipal principal,
                                               @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadEventCover(principal, id, file);
    }

    @DeleteMapping("/events/{id}/cover")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventCover(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        attachmentService.deleteEventCover(principal, id);
    }

    @PostMapping(value = "/posts/{id}/attachments", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadPostAttachment(@PathVariable Long id,
                                                   @AuthenticationPrincipal AuthUserPrincipal principal,
                                                   @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadPostAttachment(principal, id, file);
    }

    @DeleteMapping("/posts/{id}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePostAttachment(@PathVariable Long id,
                                     @PathVariable Long attachmentId,
                                     @AuthenticationPrincipal AuthUserPrincipal principal) {
        attachmentService.deletePostAttachment(principal, id, attachmentId);
    }

    @PostMapping(value = "/posts/comments/{commentId}/attachments", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadCommentAttachment(@PathVariable Long commentId,
                                                      @AuthenticationPrincipal AuthUserPrincipal principal,
                                                      @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadCommentAttachment(principal, commentId, file);
    }

    @DeleteMapping("/posts/comments/{commentId}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentAttachment(@PathVariable Long commentId,
                                        @PathVariable Long attachmentId,
                                        @AuthenticationPrincipal AuthUserPrincipal principal) {
        attachmentService.deleteCommentAttachment(principal, commentId, attachmentId);
    }

    @GetMapping("/attachments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthUserPrincipal principal) {
        return attachmentService.download(id, principal.getCompanyId());
    }
}