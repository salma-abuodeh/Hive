package org.example.hive.auth;

import org.example.hive.company.Company;
import org.example.hive.company.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CustomUserDetailsService customUserDetailsService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
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
    public LoginResponseDto register(RegisterRequestDto req) {
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
                .createdAt(LocalDateTime.now())
                .build();
        company = companyRepository.save(company);

        Role adminRole = roleRepository.findByNameAndCompanyIsNull("Company Admin")
                .orElseThrow(() -> new AuthException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .createdAt(LocalDateTime.now())
                .company(company)
                .role(adminRole)
                .build();
        userRepository.save(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponseDto(token);
    }
}
