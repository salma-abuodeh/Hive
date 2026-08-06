package org.example.hive.service;

import java.time.LocalDateTime;
import java.util.List;
import org.example.hive.dto.request.CreateCompanyRequest;
import org.example.hive.dto.request.MembershipRequestCreate;
import org.example.hive.dto.request.ReviewRequest;
import org.example.hive.dto.response.RequestResponse;
import org.example.hive.exception.UserException;
import org.example.hive.model.*;
import org.example.hive.repository.*;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {
    private final CompanyApplicationRepository companyApplications;
    private final MembershipRequestRepository membershipRequests;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final RoleRepository roles;
    private final UserCompanyRepository memberships;
    private final UserCompanyService userCompanyService;

    public OnboardingService(CompanyApplicationRepository companyApplications, MembershipRequestRepository membershipRequests,
                             UserRepository users, CompanyRepository companies, RoleRepository roles,
                             UserCompanyRepository memberships, UserCompanyService userCompanyService) {
        this.companyApplications = companyApplications; this.membershipRequests = membershipRequests;
        this.users = users; this.companies = companies; this.roles = roles; this.memberships = memberships;
        this.userCompanyService = userCompanyService;
    }

    @Transactional
    public RequestResponse applyForCompany(AuthUserPrincipal principal, CreateCompanyRequest req) {
        if (companyApplications.existsByRequester_IdAndStatus(principal.getUserId(), "PENDING")) throw error("You already have a pending company application", HttpStatus.CONFLICT);
        String domain = domain(req.getDomain());
        if (domain != null && companies.existsByDomain(domain)) throw error("A company with this domain already exists", HttpStatus.CONFLICT);
        CompanyApplication item = new CompanyApplication();
        item.setRequester(user(principal.getUserId())); item.setName(req.getName().trim()); item.setType(req.getType()); item.setDomain(domain);
        return company(companyApplications.save(item));
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> myCompanyApplications(AuthUserPrincipal p) { return companyApplications.findByRequester_IdOrderByCreatedAtDesc(p.getUserId()).stream().map(this::company).toList(); }
    @Transactional(readOnly = true)
    public List<RequestResponse> pendingCompanyApplications() { return companyApplications.findByStatusOrderByCreatedAtAsc("PENDING").stream().map(this::company).toList(); }

    @Transactional
    public RequestResponse approveCompany(Long id, AuthUserPrincipal p) {
        CompanyApplication app = pendingCompany(id); String domain = domain(app.getDomain());
        if (domain != null && companies.existsByDomain(domain)) throw error("A company with this domain already exists", HttpStatus.CONFLICT);
        Company c = new Company(); c.setName(app.getName()); c.setType(app.getType()); c.setDomain(domain); c.setStatus("active"); c.setActive(true); c = companies.save(c);
        userCompanyService.addOrReactivate(app.getRequester(), c, role(RoleNames.MANAGER));
        review(app, p, "APPROVED", null); return company(app);
    }

    @Transactional
    public RequestResponse rejectCompany(Long id, AuthUserPrincipal p, ReviewRequest req) { CompanyApplication app = pendingCompany(id); review(app, p, "REJECTED", req.getReason()); return company(app); }

    @Transactional
    public RequestResponse requestMembership(AuthUserPrincipal p, MembershipRequestCreate req) {
        User requester = user(p.getUserId());
        Company c = companies.findById(req.getCompanyId()).orElseThrow(() -> error("Company not found", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(c.getActive()) || !"active".equalsIgnoreCase(c.getStatus())) throw error("Company is not accepting requests", HttpStatus.BAD_REQUEST);
        if (memberships.findByUser_IdAndCompany_IdAndActiveTrue(requester.getId(), c.getId()).isPresent()) throw error("You already belong to this company", HttpStatus.CONFLICT);
        if (membershipRequests.existsByRequester_IdAndCompany_IdAndStatus(requester.getId(), c.getId(), "PENDING")) throw error("You already requested to join this company", HttpStatus.CONFLICT);
        MembershipRequest item = new MembershipRequest(); item.setRequester(requester); item.setCompany(c); return membership(membershipRequests.save(item));
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> myMembershipRequests(AuthUserPrincipal p) { return membershipRequests.findByRequester_IdOrderByCreatedAtDesc(p.getUserId()).stream().map(this::membership).toList(); }
    @Transactional(readOnly = true)
    public List<RequestResponse> pendingMembershipRequests(AuthUserPrincipal p) { return membershipRequests.findByCompany_IdAndStatusOrderByCreatedAtAsc(companyId(p), "PENDING").stream().map(this::membership).toList(); }

    @Transactional
    public RequestResponse approveMembership(Long id, AuthUserPrincipal p, ReviewRequest req) {
        MembershipRequest item = pendingMembership(id, p); userCompanyService.addOrReactivate(item.getRequester(), item.getCompany(), role(req.getRoleName())); review(item, p, "APPROVED", null); return membership(item);
    }
    @Transactional
    public RequestResponse rejectMembership(Long id, AuthUserPrincipal p, ReviewRequest req) { MembershipRequest item = pendingMembership(id, p); review(item, p, "REJECTED", req.getReason()); return membership(item); }

    private CompanyApplication pendingCompany(Long id) { CompanyApplication item = companyApplications.findById(id).orElseThrow(() -> error("Application not found", HttpStatus.NOT_FOUND)); if (!"PENDING".equals(item.getStatus())) throw error("Application has already been reviewed", HttpStatus.CONFLICT); return item; }
    private MembershipRequest pendingMembership(Long id, AuthUserPrincipal p) { MembershipRequest item = membershipRequests.findById(id).orElseThrow(() -> error("Request not found", HttpStatus.NOT_FOUND)); if (!item.getCompany().getId().equals(companyId(p))) throw error("Request is outside your company", HttpStatus.FORBIDDEN); if (!"PENDING".equals(item.getStatus())) throw error("Request has already been reviewed", HttpStatus.CONFLICT); return item; }
    private Long companyId(AuthUserPrincipal p) { if (p.getCompanyId() == null) throw error("Select a company workspace first", HttpStatus.BAD_REQUEST); return p.getCompanyId(); }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> error("User not found", HttpStatus.NOT_FOUND)); }
    private Role role(String roleName) { boolean manager = "MANAGER".equalsIgnoreCase(roleName) || RoleNames.MANAGER.equalsIgnoreCase(roleName); return roles.findByNameAndCompanyIsNull(manager ? RoleNames.MANAGER : RoleNames.EMPLOYEE).orElseThrow(() -> error("Role not found", HttpStatus.INTERNAL_SERVER_ERROR)); }
    private String domain(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(); }
    private void review(CompanyApplication item, AuthUserPrincipal p, String status, String reason) { item.setReviewer(user(p.getUserId())); item.setStatus(status); item.setRejectionReason(reason); item.setReviewedAt(LocalDateTime.now()); companyApplications.save(item); }
    private void review(MembershipRequest item, AuthUserPrincipal p, String status, String reason) { item.setReviewer(user(p.getUserId())); item.setStatus(status); item.setRejectionReason(reason); item.setReviewedAt(LocalDateTime.now()); membershipRequests.save(item); }
    private RequestResponse company(CompanyApplication item) { return RequestResponse.builder().id(item.getId()).status(item.getStatus()).requesterName(item.getRequester().getFirstName()+" "+item.getRequester().getLastName()).requesterEmail(item.getRequester().getEmail()).companyName(item.getName()).rejectionReason(item.getRejectionReason()).createdAt(item.getCreatedAt()).reviewedAt(item.getReviewedAt()).build(); }
    private RequestResponse membership(MembershipRequest item) { return RequestResponse.builder().id(item.getId()).status(item.getStatus()).requesterName(item.getRequester().getFirstName()+" "+item.getRequester().getLastName()).requesterEmail(item.getRequester().getEmail()).companyId(item.getCompany().getId()).companyName(item.getCompany().getName()).rejectionReason(item.getRejectionReason()).createdAt(item.getCreatedAt()).reviewedAt(item.getReviewedAt()).build(); }
    private UserException error(String message, HttpStatus status) { return new UserException(message, status); }
}
