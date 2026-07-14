package org.example.hive.service;

import org.example.hive.domain.Role;
import org.example.hive.domain.User;
import org.example.hive.domain.UserCompanyMembership;
import org.example.hive.dto.request.LoginRequestDto;
import org.example.hive.dto.request.RegisterRequestDto;
import org.example.hive.dto.response.CompanySummaryDto;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.dto.response.RegisterResponseDto;
import org.example.hive.exception.AuthException;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CustomUserDetailsService customUserDetailsService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        String role = user.getRole() != null ? user.getRole().getName() : null;
        List<CompanySummaryDto> companies = user.getMemberships().stream()
                .map(UserCompanyMembership::getCompany)
                .map(c -> new CompanySummaryDto(c.getId(), c.getName()))
                .toList();

        return new LoginResponseDto(
                token,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                role,
                user.getActive(),
                companies
        );
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT);
        }

        Role employeeRole = roleRepository.findByNameAndCompanyIsNull("Employee")
                .orElseThrow(() -> new AuthException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .createdAt(LocalDateTime.now())
                .role(employeeRole)
                .active(true)
                .build();
        userRepository.save(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        String role = user.getRole() != null ? user.getRole().getName() : null;

        return new RegisterResponseDto(
                token,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                role,
                user.getActive()
        );
    }
}