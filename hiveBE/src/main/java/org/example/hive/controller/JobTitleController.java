package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.CreateJobTitleRequest;
import org.example.hive.dto.request.UpdateJobTitleRequest;
import org.example.hive.dto.response.JobTitleResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.JobTitleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/companies/me/job-titles")
public class JobTitleController {

    private final JobTitleService jobTitleService;

    public JobTitleController(JobTitleService jobTitleService) {
        this.jobTitleService = jobTitleService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).USER_VIEW)")
    public List<JobTitleResponse> list(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return jobTitleService.listMine(principal, includeInactive);
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).USER_UPDATE)")
    @ResponseStatus(HttpStatus.CREATED)
    public JobTitleResponse create(
            @Valid @RequestBody CreateJobTitleRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return jobTitleService.create(principal, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).USER_UPDATE)")
    public JobTitleResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobTitleRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return jobTitleService.update(principal, id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, T(org.example.hive.security.Permissions).USER_UPDATE)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        jobTitleService.deactivate(principal, id);
    }
}
