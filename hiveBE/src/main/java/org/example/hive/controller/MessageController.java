package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.SendMessageRequest;
import org.example.hive.dto.response.MessageResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).MESSAGE_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable Long conversationId,
                                @AuthenticationPrincipal AuthUserPrincipal principal,
                                @Valid @RequestBody SendMessageRequest req) {
        return messageService.send(conversationId, principal.getUserId(), principal.getCompanyId(), req);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).CONVERSATION_VIEW)")
    public Page<MessageResponse> list(@PathVariable Long conversationId,
                                      @AuthenticationPrincipal AuthUserPrincipal principal,
                                      Pageable pageable) {
        return messageService.list(conversationId, principal.getUserId(), principal.getCompanyId(), pageable);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long conversationId,
                       @PathVariable Long messageId,
                       @AuthenticationPrincipal AuthUserPrincipal principal) {
        messageService.delete(conversationId, messageId, principal.getUserId(), principal.getCompanyId());
    }
}