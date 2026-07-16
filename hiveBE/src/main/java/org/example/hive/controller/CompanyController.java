package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.CreateCompanyRequest;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.CompanyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public LoginResponseDto create(@AuthenticationPrincipal AuthUserPrincipal principal,
                                   @Valid @RequestBody CreateCompanyRequest req) {
        return companyService.create(principal.getUserId(), req);
    }
}
