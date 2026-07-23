package org.example.hive.service;

import org.example.hive.dto.request.CreateUserRequest;
import org.example.hive.dto.request.UpdateMeRequest;
import org.example.hive.dto.request.UpdateUserRequest;
import org.example.hive.dto.response.CompanyMembershipDto;
import org.example.hive.dto.response.TeamSummaryDto;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.UserMapper;
import org.example.hive.model.CompanyJobTitle;
import org.example.hive.model.Role;
import org.example.hive.model.RoleNames;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.model.UserTeam;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.TeamRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.repository.UserTeamRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCompanyService userCompanyService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobTitleService jobTitleService;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;

    public UserService(UserRepository userRepository,
                       UserCompanyService userCompanyService,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JobTitleService jobTitleService,
                       TeamRepository teamRepository,
                       UserTeamRepository userTeamRepository) {
        this.userRepository = userRepository;
        this.userCompanyService = userCompanyService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobTitleService = jobTitleService;
        this.teamRepository = teamRepository;
        this.userTeamRepository = userTeamRepository;
    }

    // ===================== ME =====================

    @Transactional(readOnly = true)
    public UserResponseDto getMe(AuthUserPrincipal principal) {
        User user = requireUser(principal.getUserId());
        return toProfileResponse(user, principal);
    }

    @Transactional
    public UserResponseDto updateMe(AuthUserPrincipal principal, UpdateMeRequest req) {
        Long userId = principal.getUserId();
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
        return toProfileResponse(user, principal);
    }

    private UserResponseDto toProfileResponse(User user, AuthUserPrincipal principal) {
        List<UserCompany> memberships = userCompanyService.listActiveForUser(user.getId());
        List<CompanyMembershipDto> companies = memberships.stream()
                .sorted(Comparator.comparing(m -> m.getCompany().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(m -> UserMapper.toCompanyMembership(
                        m,
                        teamSummaries(user.getId(), m.getCompany().getId())))
                .toList();

        UserCompany active = resolveMembership(principal);
        if (active == null && !memberships.isEmpty()) {
            active = memberships.get(0);
        }

        Long activeCompanyId = principal.getCompanyId() != null
                ? principal.getCompanyId()
                : (active != null ? active.getCompany().getId() : null);

        List<TeamSummaryDto> activeTeams = activeCompanyId != null
                ? teamSummaries(user.getId(), activeCompanyId)
                : List.of();

        return UserMapper.toProfile(user, active, activeTeams, activeCompanyId, companies);
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
            applyJobTitle(user, last, req.getJobTitleId(), companyId);
            if (req.getTeamIds() != null) {
                syncTeams(user, companyId, req.getTeamIds());
            }
        }

        user = userRepository.save(user);

        if (last != null) {
            return UserMapper.toResponse(last, teamSummaries(user.getId(), last.getCompany().getId()));
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

        List<UserCompany> memberships = userCompanyService.listActiveForUser(user.getId());
        if (!memberships.isEmpty()) {
            UserCompany membership = memberships.get(0);
            if (req.getJobTitleId() != null) {
                applyJobTitle(user, membership, req.getJobTitleId(), membership.getCompany().getId());
                userCompanyService.save(membership);
            }
            if (req.getTeamIds() != null) {
                syncTeams(user, membership.getCompany().getId(), req.getTeamIds());
            }
            user = userRepository.save(user);
            return UserMapper.toResponse(membership, teamSummaries(user.getId(), membership.getCompany().getId()));
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
                .map(uc -> UserMapper.toResponse(uc, teamSummaries(uc.getUser().getId(), uc.getCompany().getId())));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByManager(AuthUserPrincipal principal, Long id) {
        UserCompany membership = userCompanyService.requireInPrincipalCompanies(principal, id);
        return UserMapper.toResponse(membership, teamSummaries(id, membership.getCompany().getId()));
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
            applyJobTitle(user, last, req.getJobTitleId(), companyId);
            if (req.getTeamIds() != null) {
                syncTeams(user, companyId, req.getTeamIds());
            }
        }

        if (last == null) {
            throw new UserException("No company membership created", HttpStatus.BAD_REQUEST);
        }
        userRepository.save(user);
        return UserMapper.toResponse(last, teamSummaries(user.getId(), last.getCompany().getId()));
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
        if (req.getJobTitleId() != null) {
            applyJobTitle(user, membership, req.getJobTitleId(), membership.getCompany().getId());
            userRepository.save(user);
        }
        if (req.getTeamIds() != null) {
            syncTeams(user, membership.getCompany().getId(), req.getTeamIds());
        }

        UserCompany saved = userCompanyService.save(membership);
        return UserMapper.toResponse(saved, teamSummaries(id, saved.getCompany().getId()));
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

    private void applyJobTitle(User user, UserCompany membership, Long jobTitleId, Long companyId) {
        if (jobTitleId == null) {
            return;
        }
        CompanyJobTitle jobTitle = jobTitleService.requireActiveForCompany(jobTitleId, companyId);
        membership.setJobTitle(jobTitle);
        user.setJobTitle(jobTitle.getTitle());
        userCompanyService.save(membership);
    }

    private void syncTeams(User user, Long companyId, List<Long> teamIds) {
        Set<Long> desired = new HashSet<>();
        for (Long teamId : teamIds) {
            if (teamId != null) {
                desired.add(teamId);
            }
        }

        List<UserTeam> existing = userTeamRepository.findAllByUser_IdAndTeam_Company_Id(user.getId(), companyId);
        for (UserTeam membership : existing) {
            Long currentTeamId = membership.getTeam().getId();
            if (desired.remove(currentTeamId)) {
                continue; 
            }
            userTeamRepository.delete(membership);
        }
        userTeamRepository.flush();

        for (Long teamId : desired) {
            Team team = teamRepository.findByIdAndCompany_IdAndActiveTrue(teamId, companyId)
                    .orElseThrow(() -> new UserException("Team not found: " + teamId, HttpStatus.BAD_REQUEST));
            userTeamRepository.save(UserTeam.builder()
                    .user(user)
                    .team(team)
                    .build());
        }
    }

    private List<TeamSummaryDto> teamSummaries(Long userId, Long companyId) {
        return userTeamRepository.findAllByUser_IdAndTeam_Company_IdAndTeam_ActiveTrue(userId, companyId)
                .stream()
                .map(ut -> new TeamSummaryDto(ut.getTeam().getId(), ut.getTeam().getName()))
                .sorted(Comparator.comparing(TeamSummaryDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private UserCompany resolveMembership(AuthUserPrincipal principal) {
        if (principal.getCompanyId() != null) {
            return userCompanyService.listActiveForUser(principal.getUserId()).stream()
                    .filter(m -> m.getCompany().getId().equals(principal.getCompanyId()))
                    .findFirst()
                    .orElse(null);
        }
        List<UserCompany> memberships = userCompanyService.listActiveForUser(principal.getUserId());
        return memberships.isEmpty() ? null : memberships.get(0);
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
