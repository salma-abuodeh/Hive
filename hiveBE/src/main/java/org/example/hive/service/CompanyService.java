package org.example.hive.service;

import org.example.hive.domain.Company;
import org.example.hive.domain.Role;
import org.example.hive.domain.User;
import org.example.hive.domain.UserCompanyMembership;
import org.example.hive.dto.request.CreateCompanyRequest;
import org.example.hive.dto.response.CompanySummaryDto;
import org.example.hive.dto.response.LoginResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository,
                          RoleRepository roleRepository, CustomUserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponseDto create(Long userId, CreateCompanyRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
        String domain = req.getDomain() == null ? null : req.getDomain().trim().toLowerCase();
        if (domain != null && !domain.isBlank() && companyRepository.existsByDomain(domain)) {
            throw new UserException("A company with this domain already exists", HttpStatus.CONFLICT);
        }

        Company company = companyRepository.save(Company.builder()
                .name(req.getName().trim())
                .type(req.getType())
                .domain(domain == null || domain.isBlank() ? null : domain)
                .createdAt(LocalDateTime.now())
                .build());

        boolean platformAdmin = isPlatformAdmin(user);
        if (!platformAdmin) {
            Role manager = roleRepository.findByNameAndCompanyIsNull("Manager")
                    .orElseThrow(() -> new UserException("Manager role not found", HttpStatus.INTERNAL_SERVER_ERROR));
            user.setRole(manager);
            user.getMemberships().add(UserCompanyMembership.builder()
                    .user(user).company(company).createdAt(LocalDateTime.now()).build());
            userRepository.save(user);
        }

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        List<CompanySummaryDto> companies = user.getMemberships().stream()
                .map(UserCompanyMembership::getCompany)
                .map(c -> new CompanySummaryDto(c.getId(), c.getName()))
                .toList();
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        return new LoginResponseDto(jwtService.generateToken(details), user.getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), roleName, user.getActive(), companies);
    }

    private boolean isPlatformAdmin(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        String role = user.getRole().getName().trim().replace(' ', '_').toUpperCase();
        return "PLATFORM_ADMIN".equals(role);
    }
}
