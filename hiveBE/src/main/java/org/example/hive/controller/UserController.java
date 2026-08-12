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
    @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')")
    public Page<UserResponseDto> listUsersByAdmin(
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return userService.listUsersByAdmin(active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')")
    public UserResponseDto getUserByAdmin(@PathVariable Long id) {
        return userService.getUserByAdmin(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')")
    public UserResponseDto createUserByAdmin(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUserByAdmin(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')")
    public UserResponseDto updateUserByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.updateUserByAdmin(id, principal.getUserId(), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission(null, 'PLATFORM_MANAGE')")
    public void deleteUserByAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        userService.deleteUserByAdmin(id, principal.getUserId());
    }

    @GetMapping("/company")
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public Page<UserResponseDto> listUsersByManager(
            @RequestParam(required = false) Boolean active,
            Pageable pageable,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.listUsersByManager(principal, active, pageable);
    }

    @GetMapping("/company/members")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<UserResponseDto> listCompanyMembers(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.listCompanyMembers(principal);
    }

    @GetMapping("/company/{id}")
    @PreAuthorize("hasPermission(null, 'USER_VIEW')")
    public UserResponseDto getUserByManager(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.getUserByManager(principal, id);
    }

    @PostMapping("/company")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'USER_INVITE')")
    public UserResponseDto createUserByManager(
            @Valid @RequestBody CreateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.createUserByManager(principal, req);
    }

    @PutMapping("/company/{id}")
    @PreAuthorize("hasPermission(null, 'USER_UPDATE')")
    public UserResponseDto updateUserByManager(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.updateUserByManager(principal, id, req);
    }

    @DeleteMapping("/company/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission(null, 'USER_DEACTIVATE')")
    public void deleteUserByManager(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        userService.deleteUserByManager(principal, id);
    }
}
