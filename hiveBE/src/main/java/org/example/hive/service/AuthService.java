package org.example.hive.service;

import org.example.hive.model.Company;
import org.example.hive.model.Role;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.dto.request.LoginRequestDto;
import org.example.hive.dto.request.RegisterRequestDto;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.dto.response.RegisterResponseDto;
import org.example.hive.exception.AuthException;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserCompanyRepository;
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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       UserCompanyRepository userCompanyRepository,
                       RoleRepository roleRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CustomUserDetailsService customUserDetailsService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponseDto login(LoginRequestDto req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponseDto(token);
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT);
        }

        if (req.getCompanyDomain() != null && !req.getCompanyDomain().isBlank()
                && companyRepository.existsByDomain(req.getCompanyDomain())) {
            throw new AuthException("Company domain already exists", HttpStatus.CONFLICT);
        }

        Company company = Company.builder()
                .name(req.getCompanyName())
                .type(req.getCompanyType())
                .domain(req.getCompanyDomain())
                .build();
        company = companyRepository.save(company);

        Role adminRole = roleRepository.findByNameAndCompanyIsNull(RoleNames.MANAGER)
                .orElseThrow(() -> new AuthException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();
        user = userRepository.save(user);

        UserCompany membership = UserCompany.builder()
                .user(user)
                .company(company)
                .role(adminRole)
                .build();
        userCompanyRepository.save(membership);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new RegisterResponseDto(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                company.getId(),
                company.getName()
        );
    }
}