package org.example.hive.service;

import org.example.hive.dto.request.LoginRequestDto;
import org.example.hive.dto.request.RegisterRequestDto;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.dto.response.RegisterResponseDto;
import org.example.hive.exception.AuthException;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserCompanyService userCompanyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       UserCompanyRepository userCompanyRepository,
                       UserCompanyService userCompanyService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CustomUserDetailsService customUserDetailsService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.userCompanyService = userCompanyService;
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

        return buildLoginResponse(user, null);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto switchCompany(AuthUserPrincipal principal, Long companyId) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED));

        userCompanyRepository.findByUser_IdAndCompany_IdAndActiveTrue(user.getId(), companyId)
                .orElseThrow(() -> new AuthException("You are not a member of that company", HttpStatus.FORBIDDEN));

        return buildLoginResponse(user, companyId);
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .active(true)
                .build();
        user = userRepository.save(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new RegisterResponseDto(
                token,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                RoleNames.EMPLOYEE,
                user.getActive(),
                permissionNames(userDetails)
        );
    }

    LoginResponseDto buildLoginResponse(User user, Long companyId) {
        UserDetails userDetails = customUserDetailsService.loadUser(user.getEmail(), companyId);
        String token = jwtService.generateToken(userDetails);

        Long activeCompanyId = userDetails instanceof AuthUserPrincipal principal
                ? principal.getCompanyId()
                : null;

        var memberships = userCompanyService.listActiveForUser(user.getId());
        String role = resolveDisplayRole(user, memberships, activeCompanyId);

        return new LoginResponseDto(
                token,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                role,
                user.getActive(),
                activeCompanyId,
                userCompanyService.listCompanySummaries(user.getId()),
                permissionNames(userDetails)
        );
    }

    private List<String> permissionNames(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }

    private String resolveDisplayRole(User user, List<UserCompany> memberships, Long activeCompanyId) {
        if (user.getPlatformRole() != null) {
            return user.getPlatformRole().getName();
        }
        if (activeCompanyId != null) {
            return memberships.stream()
                    .filter(m -> m.getCompany().getId().equals(activeCompanyId))
                    .map(m -> m.getRole() != null ? m.getRole().getName() : RoleNames.EMPLOYEE)
                    .findFirst()
                    .orElse(RoleNames.EMPLOYEE);
        }
        if (!memberships.isEmpty() && memberships.get(0).getRole() != null) {
            return memberships.get(0).getRole().getName();
        }
        return RoleNames.EMPLOYEE;
    }
}
