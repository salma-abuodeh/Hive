package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.AddConversationMembersRequest;
import org.example.hive.dto.request.CreateDirectConversationRequest;
import org.example.hive.dto.request.CreateGroupConversationRequest;
import org.example.hive.dto.response.ConversationResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).CONVERSATION_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createDirect(@AuthenticationPrincipal AuthUserPrincipal principal,
                                             @Valid @RequestBody CreateDirectConversationRequest req) {
        return conversationService.createDirect(principal.getUserId(), principal.getCompanyId(), req);
    }

    @PostMapping("/group")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).CONVERSATION_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createGroup(@AuthenticationPrincipal AuthUserPrincipal principal,
                                            @Valid @RequestBody CreateGroupConversationRequest req) {
        return conversationService.createGroup(principal.getUserId(), principal.getCompanyId(), req);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).CONVERSATION_VIEW)")
    public List<ConversationResponse> list(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return conversationService.list(principal.getUserId(), principal.getCompanyId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).CONVERSATION_VIEW)")
    public ConversationResponse getById(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        return conversationService.getById(id, principal.getUserId(), principal.getCompanyId());
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse addMembers(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthUserPrincipal principal,
                                           @Valid @RequestBody AddConversationMembersRequest req) {
        return conversationService.addMembers(id, principal.getUserId(), principal.getCompanyId(), req);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse removeMember(@PathVariable Long id,
                                             @PathVariable Long userId,
                                             @AuthenticationPrincipal AuthUserPrincipal principal) {
        return conversationService.removeMember(id, principal.getUserId(), principal.getCompanyId(), userId);
    }

    @DeleteMapping("/{id}/leave")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        conversationService.leave(id, principal.getUserId(), principal.getCompanyId());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        conversationService.markRead(id, principal.getUserId(), principal.getCompanyId());
    }
}