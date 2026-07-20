package org.example.hive.service;

import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.model.Role;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCompanyService userCompanyService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserCompanyService userCompanyService,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCompanyService = userCompanyService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===================== ME =====================

    @Transactional(readOnly = true)
    public UserResponseDto getMe(Long userId) {
        User user = requireUser(userId);
        List<UserCompany> memberships = userCompanyService.listActiveForUser(userId);
        if (!memberships.isEmpty()) {
            return UserMapper.toResponse(memberships.get(0));
        }
        return UserMapper.toResponse(user);
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

        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }

        if (changingEmail) {
            if (userRepository.existsByEmailAndIdNot(req.getEmail(), userId)) {
                throw new UserException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(req.getEmail());
        }
        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        user = userRepository.save(user);
        List<UserCompany> memberships = userCompanyService.listActiveForUser(userId);
        if (!memberships.isEmpty()) {
            return UserMapper.toResponse(memberships.get(0));
        }
        return UserMapper.toResponse(user);
    }

    // ===================== BY ADMIN =====================

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listUsersByAdmin(Boolean active, Pageable pageable) {
        Page<User> page = active == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByActive(active, pageable);
        return page.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByAdmin(Long id) {
        return UserMapper.toResponse(requireUser(id));
    }

    @Transactional
    public UserResponseDto createUserByAdmin(CreateUserRequest req) {
        Role role = resolveAssignableRole(req.getRoleName());
        Set<Long> companyIds = req.getCompanyIds() == null
                ? Set.of()
                : new HashSet<>(req.getCompanyIds());

        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null) {
            user = newUser(req);
            user = userRepository.save(user);
        } else if (companyIds.isEmpty()) {
            throw new UserException("Email already exists", HttpStatus.CONFLICT);
        }

        UserCompany last = null;
        for (Long companyId : companyIds) {
            last = userCompanyService.add(user, companyId, role);
        }

        if (last != null) {
            return UserMapper.toResponse(last);
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDto updateUserByAdmin(Long id, Long currentUserId, UpdateUserRequest req) {
        blockSelfDeactivate(currentUserId, id, req);
        User user = requireUser(id);
        applyUserFields(user, id, req, true);

        if (req.getRoleName() != null) {
            userCompanyService.updateRoleOnActiveMemberships(user.getId(), resolveAssignableRole(req.getRoleName()));
        }

        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void deleteUserByAdmin(Long id, Long currentUserId) {
        blockSelfDelete(currentUserId, id);
        if (!userRepository.existsById(id)) {
            throw new UserException("User not found", HttpStatus.NOT_FOUND);
        }
        userCompanyService.deleteAllForUser(id);
        userRepository.deleteById(id);
    }

    // ===================== BY MANAGER =====================

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listUsersByManager(AuthUserPrincipal principal, Boolean active, Pageable pageable) {
        Set<Long> companyIds = userCompanyService.companyIdsOf(principal);
        if (companyIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return userCompanyService.pageByCompanies(companyIds, active, pageable)
                .map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByManager(AuthUserPrincipal principal, Long id) {
        return UserMapper.toResponse(userCompanyService.requireInPrincipalCompanies(principal, id));
    }

    @Transactional
    public UserResponseDto createUserByManager(AuthUserPrincipal principal, CreateUserRequest req) {
        Set<Long> mine = userCompanyService.companyIdsOf(principal);
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

        Role role = resolveAssignableRole(req.getRoleName());
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user == null) {
            user = newUser(req);
            user = userRepository.save(user);
        }

        UserCompany last = null;
        for (Long companyId : target) {
            last = userCompanyService.add(user, companyId, role);
        }

        if (last == null) {
            throw new UserException("No company membership created", HttpStatus.BAD_REQUEST);
        }
        return UserMapper.toResponse(last);
    }

    @Transactional
    public UserResponseDto updateUserByManager(AuthUserPrincipal principal, Long id, UpdateUserRequest req) {
        blockSelfDeactivate(principal.getUserId(), id, req);
        UserCompany membership = userCompanyService.requireInPrincipalCompanies(principal, id);
        User user = membership.getUser();

        applyUserFields(user, id, req, false);
        userRepository.save(user);

        if (req.getRoleName() != null) {
            membership.setRole(resolveAssignableRole(req.getRoleName()));
        }
        if (req.getActive() != null) {
            membership.setActive(req.getActive());
        }

        return UserMapper.toResponse(userCompanyService.save(membership));
    }

    @Transactional
    public void deleteUserByManager(AuthUserPrincipal principal, Long id) {
        blockSelfDelete(principal.getUserId(), id);
        UserCompany membership = userCompanyService.requireInPrincipalCompanies(principal, id);
        userCompanyService.deactivate(membership);
    }

    // ===================== helpers =====================

    private User newUser(CreateUserRequest req) {
        return User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .active(true)
                .build();
    }

    private void applyUserFields(User user, Long id, UpdateUserRequest req, boolean applyActive) {
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
        if (applyActive && req.getActive() != null) {
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

    private Role resolveAssignableRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new UserException("Role is required", HttpStatus.BAD_REQUEST);
        }

        String normalized = roleName.trim().replace(' ', '_').toUpperCase();
        if ("PLATFORM_ADMIN".equals(normalized)
                || "PLATFORM_ADMINISTRATOR".equals(normalized)
                || RoleNames.PLATFORM_ADMIN.equalsIgnoreCase(roleName)) {
            throw new UserException("Cannot assign Platform Admin role", HttpStatus.BAD_REQUEST);
        }

        boolean isManager = "MANAGER".equals(normalized)
                || "COMPANY_ADMIN".equals(normalized)
                || RoleNames.MANAGER.equalsIgnoreCase(roleName);
        String name = isManager ? RoleNames.MANAGER : RoleNames.EMPLOYEE;

        return roleRepository.findByNameAndCompanyIsNull(name)
                .orElseThrow(() -> new UserException("Invalid role", HttpStatus.BAD_REQUEST));
    }
}
