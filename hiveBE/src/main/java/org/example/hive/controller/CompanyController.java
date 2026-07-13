package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.CompanyRequest;
import org.example.hive.dto.response.CompanyResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.CompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CompanyRequest req) {
        return companyService.create(req);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public CompanyResponse getMine(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return companyService.getById(principal.getCompanyId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public CompanyResponse updateMine(@Valid @RequestBody CompanyRequest req,
                                      @AuthenticationPrincipal AuthUserPrincipal principal) {
        return companyService.update(principal.getCompanyId(), req);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveMine(@AuthenticationPrincipal AuthUserPrincipal principal) {
        companyService.archive(principal.getCompanyId());
    }
}