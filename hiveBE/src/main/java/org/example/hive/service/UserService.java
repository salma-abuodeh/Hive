package org.example.hive.service;

import org.example.hive.domain.Company;
import org.example.hive.domain.Role;
import org.example.hive.domain.User;
import org.example.hive.domain.UserCompanyMembership;
import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

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

    // ===================== ME =====================

    @Transactional(readOnly = true)
    public UserResponseDto getMe(Long userId) {
        return UserMapper.toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponseDto updateMe(Long userId, UpdateMeRequest req) {
        User user = requireUser(userId);

        boolean changingEmail = req.getEmail() != null && !req.getEmail().equals(user.getEmail());
        boolean changingPassword = req.getPassword() != null && !req.getPassword().isBlank();

        if (changingEmail || changingPassword) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                throw new UserException("Current password is required", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new UserException("Current password is incorrect", HttpStatus.BAD_REQUEST);
            }
        }

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());

        if (changingEmail) {
            if (userRepository.existsByEmailAndIdNot(req.getEmail(), userId)) {
                throw new UserException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(req.getEmail());
        }
        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    // ===================== PLATFORM ADMIN =====================

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listAll(Boolean active, Pageable pageable) {
        Page<User> page = active == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByActive(active, pageable);
        return page.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getByIdAsPlatform(Long id) {
        return UserMapper.toResponse(requireUser(id));
    }

    @Transactional
    public UserResponseDto createAsPlatform(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserException("Email already exists", HttpStatus.CONFLICT);
        }

        Role role = assignableRole(req.getRoleName());
        Set<Long> companyIds = req.getCompanyIds() == null
                ? Set.of()
                : new HashSet<>(req.getCompanyIds());

        User user = newUser(req, role);
        addMemberships(user, companyIds);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateAsPlatform(Long id, Long currentUserId, UpdateUserRequest req) {
        blockSelfDeactivate(currentUserId, id, req);
        User user = requireUser(id);
        applyUpdate(user, id, req);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteAsPlatform(Long id, Long currentUserId) {
        blockSelfDelete(currentUserId, id);
        if (!userRepository.existsById(id)) {
            throw new UserException("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    // ===================== COMPANY ADMIN =====================

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listForCompany(AuthUserPrincipal principal, Boolean active, Pageable pageable) {
        Set<Long> companyIds = companyIdsOf(principal.getUserId());
        if (companyIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<User> page = active == null
                ? userRepository.findMembersInCompanies(companyIds, pageable)
                : userRepository.findMembersInCompaniesAndActive(companyIds, active, pageable);
        return page.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getByIdAsCompany(AuthUserPrincipal principal, Long id) {
        return UserMapper.toResponse(requireInCompanies(principal.getUserId(), id));
    }

    @Transactional
    public UserResponseDto createAsCompany(AuthUserPrincipal principal, CreateUserRequest req) {
        Set<Long> mine = companyIdsOf(principal.getUserId());
        if (mine.isEmpty()) {
            throw new UserException("Manager has no company membership", HttpStatus.BAD_REQUEST);
        }

        Set<Long> target = (req.getCompanyIds() == null || req.getCompanyIds().isEmpty())
                ? mine
                : new HashSet<>(req.getCompanyIds());

        for (Long companyId : target) {
            if (!mine.contains(companyId)) {
                throw new UserException("Cannot assign membership outside your companies", HttpStatus.FORBIDDEN);
            }
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserException("Email already exists", HttpStatus.CONFLICT);
        }

        Role role = assignableRole(req.getRoleName());
        User user = newUser(req, role);
        addMemberships(user, target);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateAsCompany(AuthUserPrincipal principal, Long id, UpdateUserRequest req) {
        blockSelfDeactivate(principal.getUserId(), id, req);
        User user = requireInCompanies(principal.getUserId(), id);
        applyUpdate(user, id, req);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteAsCompany(AuthUserPrincipal principal, Long id) {
        blockSelfDelete(principal.getUserId(), id);
        User user = requireInCompanies(principal.getUserId(), id);
        userRepository.delete(user);
    }


    private User newUser(CreateUserRequest req, Role role) {
        return User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .createdAt(LocalDateTime.now())
                .role(role)
                .active(true)
                .build();
    }

    private void addMemberships(User user, Set<Long> companyIds) {
        for (Long companyId : companyIds) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new UserException("Company not found: " + companyId, HttpStatus.NOT_FOUND));
            user.getMemberships().add(UserCompanyMembership.builder()
                    .user(user)
                    .company(company)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    private void applyUpdate(User user, Long id, UpdateUserRequest req) {
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
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
            user.setRole(assignableRole(req.getRoleName()));
        }
        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }
    }

    private void blockSelfDelete(Long currentUserId, Long targetId) {
        if (currentUserId.equals(targetId)) {
            throw new UserException("Cannot delete your own account", HttpStatus.BAD_REQUEST);
        }
    }

    private void blockSelfDeactivate(Long currentUserId, Long targetId, UpdateUserRequest req) {
        if (currentUserId.equals(targetId) && Boolean.FALSE.equals(req.getActive())) {
            throw new UserException("Cannot deactivate your own account", HttpStatus.BAD_REQUEST);
        }
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
    }

    private User requireInCompanies(Long adminUserId, Long targetId) {
        Set<Long> companyIds = companyIdsOf(adminUserId);
        return userRepository.findByIdInCompanies(targetId, companyIds)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
    }

    private Set<Long> companyIdsOf(Long userId) {
        User user = requireUser(userId);
        return user.getMemberships().stream()
                .map(m -> m.getCompany().getId())
                .collect(Collectors.toSet());
    }

    private Role assignableRole(String roleName) {
        if ("Platform Admin".equalsIgnoreCase(roleName) || "PLATFORM_ADMIN".equalsIgnoreCase(roleName)) {
            throw new UserException("Cannot assign Platform Admin role", HttpStatus.BAD_REQUEST);
        }
        boolean isManager = "Manager".equalsIgnoreCase(roleName)
                || "MANAGER".equalsIgnoreCase(roleName)
                || "Company Admin".equalsIgnoreCase(roleName)
                || "COMPANY_ADMIN".equalsIgnoreCase(roleName);
        String name = isManager ? "Manager" : "Employee";
        return roleRepository.findByNameAndCompanyIsNull(name)
                .orElseThrow(() -> new UserException("Invalid role", HttpStatus.BAD_REQUEST));
    }
}