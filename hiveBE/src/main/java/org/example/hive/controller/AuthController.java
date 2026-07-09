package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.LoginRequestDto;
import org.example.hive.dto.request.RegisterRequestDto;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.dto.response.RegisterResponseDto;
import org.example.hive.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto register(@Valid @RequestBody RegisterRequestDto req) {
        return authService.register(req);
    }
}
