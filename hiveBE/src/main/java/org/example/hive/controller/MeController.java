package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponseDto getMe(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.getMe(principal.getUserId());
    }

    @PatchMapping("/me")
    public UserResponseDto updateMe(@AuthenticationPrincipal AuthUserPrincipal principal,
                                    @Valid @RequestBody UpdateMeRequest req) {
        return userService.updateMe(principal.getUserId(), req);
    }
}