package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.PollRequest;
import org.example.hive.dto.request.VoteRequest;
import org.example.hive.dto.response.PollResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.PollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/polls")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).POLL_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public PollResponse create(@AuthenticationPrincipal AuthUserPrincipal principal,
                               @Valid @RequestBody PollRequest req) {
        return pollService.create(principal.getUserId(), principal.getCompanyId(), req);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).POLL_VIEW)")
    public Page<PollResponse> list(@AuthenticationPrincipal AuthUserPrincipal principal, Pageable pageable) {
        return pollService.list(principal.getUserId(), principal.getCompanyId(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).POLL_VIEW)")
    public PollResponse getById(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        return pollService.getById(id, principal.getUserId(), principal.getCompanyId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PollResponse update(@PathVariable Long id,
                               @AuthenticationPrincipal AuthUserPrincipal principal,
                               @Valid @RequestBody PollRequest req) {
        return pollService.update(id, principal.getUserId(), principal.getCompanyId(), req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        pollService.delete(id, principal.getUserId(), principal.getCompanyId());
    }

    @PutMapping("/{id}/vote")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).POLL_VIEW)")
    public PollResponse vote(@PathVariable Long id,
                             @AuthenticationPrincipal AuthUserPrincipal principal,
                             @Valid @RequestBody VoteRequest req) {
        return pollService.vote(id, principal.getUserId(), principal.getCompanyId(), req);
    }

    @DeleteMapping("/{id}/vote")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).POLL_VIEW)")
    public PollResponse unvote(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        return pollService.unvote(id, principal.getUserId(), principal.getCompanyId());
    }
}