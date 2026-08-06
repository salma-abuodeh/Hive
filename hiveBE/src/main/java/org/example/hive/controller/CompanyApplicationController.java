package org.example.hive.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.hive.dto.request.CreateCompanyRequest;
import org.example.hive.dto.request.ReviewRequest;
import org.example.hive.dto.response.RequestResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.OnboardingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/company-applications")
public class CompanyApplicationController {
    private final OnboardingService service;
    public CompanyApplicationController(OnboardingService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("isAuthenticated()")
    public RequestResponse apply(@AuthenticationPrincipal AuthUserPrincipal p, @Valid @RequestBody CreateCompanyRequest req) { return service.applyForCompany(p, req); }
    @GetMapping("/me") @PreAuthorize("isAuthenticated()") public List<RequestResponse> mine(@AuthenticationPrincipal AuthUserPrincipal p) { return service.myCompanyApplications(p); }
    @GetMapping @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')") public List<RequestResponse> pending() { return service.pendingCompanyApplications(); }
    @PostMapping("/{id}/approve") @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')") public RequestResponse approve(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal p) { return service.approveCompany(id, p); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')") public RequestResponse reject(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal p, @Valid @RequestBody ReviewRequest req) { return service.rejectCompany(id, p, req); }
}
