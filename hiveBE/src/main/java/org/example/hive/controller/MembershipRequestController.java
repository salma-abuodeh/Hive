package org.example.hive.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.hive.dto.request.MembershipRequestCreate;
import org.example.hive.dto.request.ReviewRequest;
import org.example.hive.dto.response.CompanyResponse;
import org.example.hive.dto.response.RequestResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.CompanyService;
import org.example.hive.service.OnboardingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/membership-requests")
public class MembershipRequestController {
    private final OnboardingService service; private final CompanyService companies;
    public MembershipRequestController(OnboardingService service, CompanyService companies) { this.service = service; this.companies = companies; }
    @GetMapping("/companies") @PreAuthorize("isAuthenticated()") public List<CompanyResponse> joinable(@AuthenticationPrincipal AuthUserPrincipal p) { return companies.listJoinable(p.getUserId()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("isAuthenticated()") public RequestResponse request(@AuthenticationPrincipal AuthUserPrincipal p, @Valid @RequestBody MembershipRequestCreate req) { return service.requestMembership(p, req); }
    @GetMapping("/me") @PreAuthorize("isAuthenticated()") public List<RequestResponse> mine(@AuthenticationPrincipal AuthUserPrincipal p) { return service.myMembershipRequests(p); }
    @GetMapping @PreAuthorize("hasPermission(null, 'USER_INVITE')") public List<RequestResponse> pending(@AuthenticationPrincipal AuthUserPrincipal p) { return service.pendingMembershipRequests(p); }
    @PostMapping("/{id}/approve") @PreAuthorize("hasPermission(null, 'USER_INVITE')") public RequestResponse approve(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal p, @Valid @RequestBody ReviewRequest req) { return service.approveMembership(id, p, req); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasPermission(null, 'USER_INVITE')") public RequestResponse reject(@PathVariable Long id, @AuthenticationPrincipal AuthUserPrincipal p, @Valid @RequestBody ReviewRequest req) { return service.rejectMembership(id, p, req); }
}
