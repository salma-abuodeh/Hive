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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponseDto> list(Pageable pageable,
                                      @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.list(principal.getCompanyId(), pageable);
    }

    @GetMapping("/{id}")
    public UserResponseDto getById(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.getById(id, principal.getCompanyId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@Valid @RequestBody CreateUserRequest req,
                                  @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.create(req, principal.getCompanyId());
    }

    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRequest req,
                                    @AuthenticationPrincipal AuthUserPrincipal principal) {
        return userService.update(id, req, principal.getCompanyId(), principal.getUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                         @AuthenticationPrincipal AuthUserPrincipal principal) {
        userService.delete(id, principal.getCompanyId(), principal.getUserId());
    }
}
