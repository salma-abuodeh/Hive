package org.example.hive.service;

import org.example.hive.domain.Company;
import org.example.hive.domain.Role;
import org.example.hive.domain.User;
import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private static final String COMPANY_ADMIN_ROLE = "Company Admin";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CompanyRepository companyRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> list(Long companyId, Pageable pageable) {
        return userRepository.findAllByCompanyIdAndActiveTrue(companyId, pageable)
                .map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id, Long companyId) {
        User user = findUserInCompany(id, companyId);
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UserException("User not found", HttpStatus.NOT_FOUND);
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDto create(CreateUserRequest req, Long companyId) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserException("Email already exists", HttpStatus.CONFLICT);
        }

        Role role = resolveAssignableRole(req.getRoleName());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new UserException("Company not found", HttpStatus.NOT_FOUND));

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .createdAt(LocalDateTime.now())
                .company(company)
                .role(role)
                .active(true)
                .build();

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto update(Long id, UpdateUserRequest req, Long companyId, Long currentUserId) {
        User user = findUserInCompany(id, companyId);

        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
                throw new UserException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(req.getEmail());
        }
        if (req.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getRoleName() != null) {
            user.setRole(resolveAssignableRole(req.getRoleName()));
        }
        if (req.getActive() != null) {
            if (id.equals(currentUserId) && !req.getActive()) {
                throw new UserException("Cannot deactivate your own account", HttpStatus.BAD_REQUEST);
            }
            user.setActive(req.getActive());
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, Long companyId, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new UserException("Cannot deactivate your own account", HttpStatus.BAD_REQUEST);
        }

        User user = findUserInCompany(id, companyId);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserInCompany(Long id, Long companyId) {
        return userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
    }

    private Role resolveAssignableRole(String roleName) {
        if (COMPANY_ADMIN_ROLE.equals(roleName)) {
            throw new UserException("Cannot assign Company Admin role", HttpStatus.BAD_REQUEST);
        }

        return roleRepository.findByNameAndCompanyIsNull(roleName)
                .orElseThrow(() -> new UserException("Invalid role", HttpStatus.BAD_REQUEST));
    }
}
