package org.example.hive.service;

import org.example.hive.model.Company;
import org.example.hive.model.Role;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserCompanyRepository userCompanyRepository,
                       CompanyRepository companyRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> list(Long companyId, Pageable pageable) {
        return userCompanyRepository.findAllByCompany_IdAndActiveTrue(companyId, pageable)
                .map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id, Long companyId) {
        return UserMapper.toResponse(findMembership(id, companyId));
    }

    @Transactional
    public UserResponseDto create(CreateUserRequest req, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new UserException("Company not found", HttpStatus.NOT_FOUND));

        Role role = resolveAssignableRole(req.getRoleName());

        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user != null) {
            if (userCompanyRepository.existsByUser_IdAndCompany_Id(user.getId(), companyId)) {
                throw new UserException("User already belongs to this company", HttpStatus.CONFLICT);
            }
        } else {
            user = User.builder()
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .email(req.getEmail())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .build();
            user = userRepository.save(user);
        }

        UserCompany membership = UserCompany.builder()
                .user(user)
                .company(company)
                .role(role)
                .build();
        membership = userCompanyRepository.save(membership);

        return UserMapper.toResponse(membership);
    }

    @Transactional
    public UserResponseDto update(Long id, UpdateUserRequest req, Long companyId, Long currentUserId) {
        UserCompany membership = findMembership(id, companyId);
        User user = membership.getUser();

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
        userRepository.save(user);

        if (req.getRoleName() != null) {
            membership.setRole(resolveAssignableRole(req.getRoleName()));
        }
        if (req.getActive() != null) {
            if (id.equals(currentUserId) && !req.getActive()) {
                throw new UserException("Cannot remove yourself from this company", HttpStatus.BAD_REQUEST);
            }
            membership.setActive(req.getActive());
        }
        membership = userCompanyRepository.save(membership);

        return UserMapper.toResponse(membership);
    }

    @Transactional
    public void delete(Long id, Long companyId, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new UserException("Cannot remove yourself from this company", HttpStatus.BAD_REQUEST);
        }

        UserCompany membership = findMembership(id, companyId);
        membership.setActive(false);
        userCompanyRepository.save(membership);
    }

    private UserCompany findMembership(Long userId, Long companyId) {
        UserCompany membership = userCompanyRepository.findByUser_IdAndCompany_Id(userId, companyId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(membership.getActive())) {
            throw new UserException("User not found", HttpStatus.NOT_FOUND);
        }

        return membership;
    }

    private Role resolveAssignableRole(String roleName) {
        if (RoleNames.MANAGER.equals(roleName) || RoleNames.PLATFORM_ADMIN.equals(roleName)) {
            throw new UserException("Cannot assign this role", HttpStatus.BAD_REQUEST);
        }

        return roleRepository.findByNameAndCompanyIsNull(roleName)
                .orElseThrow(() -> new UserException("Invalid role", HttpStatus.BAD_REQUEST));
    }
}