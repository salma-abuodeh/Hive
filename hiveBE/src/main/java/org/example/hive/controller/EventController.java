package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.EventRequest;
import org.example.hive.dto.request.InviteUsersRequest;
import org.example.hive.dto.request.RsvpUpdateRequest;
import org.example.hive.dto.response.EventResponse;
import org.example.hive.dto.response.EventRsvpResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).EVENT_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@AuthenticationPrincipal AuthUserPrincipal principal,
                                @Valid @RequestBody EventRequest req) {
        return eventService.create(principal.getUserId(), principal.getCompanyId(), req);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).EVENT_VIEW)")
    public Page<EventResponse> list(@AuthenticationPrincipal AuthUserPrincipal principal, Pageable pageable) {
        return eventService.list(principal.getUserId(), principal.getCompanyId(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).EVENT_VIEW)")
    public EventResponse getById(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        return eventService.getById(id, principal.getUserId(), principal.getCompanyId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EventResponse update(@PathVariable Long id,
                                @AuthenticationPrincipal AuthUserPrincipal principal,
                                @Valid @RequestBody EventRequest req) {
        return eventService.update(id, principal.getUserId(), principal.getCompanyId(), req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal principal) {
        eventService.delete(id, principal.getUserId(), principal.getCompanyId());
    }

    @PostMapping("/{id}/invitations")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public void invite(@PathVariable Long id,
                       @AuthenticationPrincipal AuthUserPrincipal principal,
                       @Valid @RequestBody InviteUsersRequest req) {
        eventService.invite(id, principal.getUserId(), principal.getCompanyId(), req);
    }

    @GetMapping("/{id}/invitations")
    @PreAuthorize("isAuthenticated()")
    public List<EventRsvpResponse> listInvitations(@PathVariable Long id,
                                                   @AuthenticationPrincipal AuthUserPrincipal principal) {
        return eventService.listInvitations(id, principal.getUserId(), principal.getCompanyId());
    }

    @DeleteMapping("/{id}/invitations/{userId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeInvitation(@PathVariable Long id,
                                 @PathVariable Long userId,
                                 @AuthenticationPrincipal AuthUserPrincipal principal) {
        eventService.removeInvitation(id, principal.getUserId(), principal.getCompanyId(), userId);
    }

    @PutMapping("/{id}/rsvp")
    @PreAuthorize("isAuthenticated()")
    public EventRsvpResponse respondToInvitation(@PathVariable Long id,
                                                 @AuthenticationPrincipal AuthUserPrincipal principal,
                                                 @Valid @RequestBody RsvpUpdateRequest req) {
        return eventService.respondToInvitation(id, principal.getUserId(), req);
    }
}