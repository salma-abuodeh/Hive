package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.AddTeamMembersRequest;
import org.example.hive.dto.request.CreateTeamRequest;
import org.example.hive.dto.request.UpdateTeamRequest;
import org.example.hive.dto.response.TeamResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_VIEW)")
    public List<TeamResponse> list(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.list(principal);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<TeamResponse> listMine(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.listMine(principal);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_VIEW)")
    public TeamResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.getById(principal, id);
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_CREATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(
            @Valid @RequestBody CreateTeamRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.create(principal, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_UPDATE)")
    public TeamResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.update(principal, id, req);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_MANAGE_MEMBERS)")
    public TeamResponse addMembers(
            @PathVariable Long id,
            @Valid @RequestBody AddTeamMembersRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.addMembers(principal, id, req);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).TEAM_MANAGE_MEMBERS)")
    public TeamResponse removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return teamService.removeMember(principal, id, userId);
    }
}
