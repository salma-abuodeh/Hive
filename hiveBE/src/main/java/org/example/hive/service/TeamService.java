package org.example.hive.service;

import org.example.hive.dto.request.AddTeamMembersRequest;
import org.example.hive.dto.request.CreateTeamRequest;
import org.example.hive.dto.request.UpdateTeamRequest;
import org.example.hive.dto.response.TeamMemberResponse;
import org.example.hive.dto.response.TeamResponse;
import org.example.hive.exception.TeamException;
import org.example.hive.model.Company;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.model.UserTeam;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.TeamRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.repository.UserTeamRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final ConversationService conversationService;

    public TeamService(
            TeamRepository teamRepository,
            UserTeamRepository userTeamRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            UserCompanyRepository userCompanyRepository,
            ConversationService conversationService) {
        this.teamRepository = teamRepository;
        this.userTeamRepository = userTeamRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.conversationService = conversationService;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> list(AuthUserPrincipal principal) {
        Long companyId = requireCompanyId(principal);
        List<Team> teams = teamRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId);
        Map<Long, Long> counts = memberCounts(teams.stream().map(Team::getId).toList());
        return teams.stream()
                .map(t -> toSummary(t, counts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getById(AuthUserPrincipal principal, Long teamId) {
        Team team = requireTeam(principal, teamId);
        return toDetail(team);
    }

    @Transactional
    public TeamResponse create(AuthUserPrincipal principal, CreateTeamRequest req) {
        Long companyId = requireCompanyId(principal);
        String name = req.getName().trim();

        if (teamRepository.existsByCompany_IdAndNameIgnoreCase(companyId, name)) {
            throw new TeamException("A team with this name already exists", HttpStatus.CONFLICT);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new TeamException("Company not found", HttpStatus.NOT_FOUND));

        Team team = teamRepository.save(Team.builder()
                .name(name)
                .description(req.getDescription() != null ? req.getDescription().trim() : null)
                .company(company)
                .active(true)
                .build());

        User creator = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new TeamException("User not found", HttpStatus.NOT_FOUND));
        userTeamRepository.save(UserTeam.builder()
                .user(creator)
                .team(team)
                .build());

        conversationService.createForTeam(team, creator);

        return toSummary(team, 1L);
    }

    @Transactional
    public TeamResponse update(AuthUserPrincipal principal, Long teamId, UpdateTeamRequest req) {
        Long companyId = requireCompanyId(principal);
        Team team = teamRepository.findByIdAndCompany_Id(teamId, companyId)
                .orElseThrow(() -> new TeamException("Team not found", HttpStatus.NOT_FOUND));

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (!name.equalsIgnoreCase(team.getName())
                    && teamRepository.existsByCompany_IdAndNameIgnoreCase(companyId, name)) {
                throw new TeamException("A team with this name already exists", HttpStatus.CONFLICT);
            }
            team.setName(name);
        }
        if (req.getDescription() != null) {
            team.setDescription(req.getDescription().isBlank() ? null : req.getDescription().trim());
        }
        if (req.getActive() != null) {
            team.setActive(req.getActive());
        }

        return toDetail(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse addMembers(AuthUserPrincipal principal, Long teamId, AddTeamMembersRequest req) {
        Team team = requireTeam(principal, teamId);
        Long companyId = team.getCompany().getId();

        for (Long userId : req.getUserIds()) {
            if (userId == null) {
                continue;
            }
            if (!userCompanyRepository.existsByUser_IdAndCompany_Id(userId, companyId)) {
                throw new TeamException("User is not a member of this company: " + userId, HttpStatus.BAD_REQUEST);
            }
            if (userTeamRepository.existsByUser_IdAndTeam_Id(userId, teamId)) {
                continue;
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new TeamException("User not found: " + userId, HttpStatus.NOT_FOUND));
            userTeamRepository.save(UserTeam.builder()
                    .user(user)
                    .team(team)
                    .build());
            conversationService.syncAddTeamMember(team, user);
        }

        return toDetail(team);
    }

    @Transactional
    public TeamResponse removeMember(AuthUserPrincipal principal, Long teamId, Long userId) {
        requireTeam(principal, teamId);
        if (!userTeamRepository.existsByUser_IdAndTeam_Id(userId, teamId)) {
            throw new TeamException("User is not on this team", HttpStatus.NOT_FOUND);
        }
        userTeamRepository.deleteByUser_IdAndTeam_Id(userId, teamId);
        conversationService.syncRemoveTeamMember(teamId, userId);
        return toDetail(requireTeam(principal, teamId));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMine(AuthUserPrincipal principal) {
        Long companyId = requireCompanyId(principal);
        List<Long> teamIds = userTeamRepository.findTeamIdsByUserAndCompany(principal.getUserId(), companyId);
        if (teamIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = memberCounts(teamIds);
        return teamIds.stream()
                .map(id -> teamRepository.findByIdAndCompany_IdAndActiveTrue(id, companyId).orElse(null))
                .filter(t -> t != null)
                .map(t -> toSummary(t, counts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    private Team requireTeam(AuthUserPrincipal principal, Long teamId) {
        Long companyId = requireCompanyId(principal);
        return teamRepository.findByIdAndCompany_IdAndActiveTrue(teamId, companyId)
                .orElseThrow(() -> new TeamException("Team not found", HttpStatus.NOT_FOUND));
    }

    private Long requireCompanyId(AuthUserPrincipal principal) {
        if (principal.getCompanyId() == null) {
            throw new TeamException("Join or create a company to manage teams", HttpStatus.BAD_REQUEST);
        }
        return principal.getCompanyId();
    }

    private Map<Long, Long> memberCounts(List<Long> teamIds) {
        Map<Long, Long> map = new HashMap<>();
        if (teamIds.isEmpty()) {
            return map;
        }
        for (Object[] row : userTeamRepository.countMembersByTeamIds(teamIds)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private TeamResponse toSummary(Team team, long memberCount) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .active(team.getActive())
                .memberCount(memberCount)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamResponse toDetail(Team team) {
        List<UserTeam> memberships = userTeamRepository.findAllByTeam_IdOrderByJoinedAtAsc(team.getId());
        List<TeamMemberResponse> members = memberships.stream()
                .map(ut -> TeamMemberResponse.builder()
                        .userId(ut.getUser().getId())
                        .firstName(ut.getUser().getFirstName())
                        .lastName(ut.getUser().getLastName())
                        .email(ut.getUser().getEmail())
                        .jobTitle(ut.getUser().getJobTitle())
                        .build())
                .toList();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .active(team.getActive())
                .memberCount(members.size())
                .members(members)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }
}