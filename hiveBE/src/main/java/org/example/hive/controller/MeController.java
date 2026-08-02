package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.SwitchCompanyRequest;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.AuthService;
import org.example.hive.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final UserService userService;
    private final AuthService authService;

    public MeController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponseDto getMe(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.getMe(principal);
    }

    @PatchMapping("/me")
    public UserResponseDto updateMe(@AuthenticationPrincipal AuthUserPrincipal principal,
                                    @Valid @RequestBody UpdateMeRequest req) {
        return userService.updateMe(principal, req);
    }

    @PostMapping("/me/switch-company")
    public LoginResponseDto switchCompany(@AuthenticationPrincipal AuthUserPrincipal principal,
                                          @Valid @RequestBody SwitchCompanyRequest req) {
        return authService.switchCompany(principal, req.getCompanyId());
    }
}
