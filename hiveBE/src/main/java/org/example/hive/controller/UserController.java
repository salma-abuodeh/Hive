package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Page<UserResponseDto> listAll(
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return userService.listAll(active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserResponseDto getAsPlatform(@PathVariable Long id) {
        return userService.getByIdAsPlatform(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserResponseDto createAsPlatform(@Valid @RequestBody CreateUserRequest req) {
        return userService.createAsPlatform(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserResponseDto updateAsPlatform(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.updateAsPlatform(id, principal.getUserId(), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public void deleteAsPlatform(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        userService.deleteAsPlatform(id, principal.getUserId());
    }

    @GetMapping("/company")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<UserResponseDto> listCompany(
            @RequestParam(required = false) Boolean active,
            Pageable pageable,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.listForCompany(principal, active, pageable);
    }

    @GetMapping("/company/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public UserResponseDto getAsCompany(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.getByIdAsCompany(principal, id);
    }

    @PostMapping("/company")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public UserResponseDto createAsCompany(
            @Valid @RequestBody CreateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.createAsCompany(principal, req);
    }

    @PutMapping("/company/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public UserResponseDto updateAsCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.updateAsCompany(principal, id, req);
    }

    @DeleteMapping("/company/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteAsCompany(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        userService.deleteAsCompany(principal, id);
    }
}