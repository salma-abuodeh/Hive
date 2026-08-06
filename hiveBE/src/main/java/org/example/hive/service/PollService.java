package org.example.hive.service;

import org.example.hive.config.AppEnums.EventVisibility;
import org.example.hive.dto.request.PollOptionRequest;
import org.example.hive.dto.request.PollRequest;
import org.example.hive.dto.request.VoteRequest;
import org.example.hive.dto.response.PollResponse;
import org.example.hive.exception.PollException;
import org.example.hive.mapper.PollMapper;
import org.example.hive.model.Company;
import org.example.hive.model.Poll;
import org.example.hive.model.PollOption;
import org.example.hive.model.PollVote;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.PollOptionRepository;
import org.example.hive.repository.PollRepository;
import org.example.hive.repository.PollVoteRepository;
import org.example.hive.repository.TeamRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.repository.UserTeamRepository;
import org.example.hive.security.Permissions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class PollService {

    // Sentinel used when a user belongs to no teams, so the "team.id IN :teamIds"
    // clause always has a non-empty collection to bind (an empty IN list is invalid
    // JPQL) — same trick PostRepository/PostService already use for feed visibility.
    private static final Long NO_TEAM_SENTINEL = -1L;

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;

    public PollService(PollRepository pollRepository,
                       PollOptionRepository pollOptionRepository,
                       PollVoteRepository pollVoteRepository,
                       UserRepository userRepository,
                       CompanyRepository companyRepository,
                       TeamRepository teamRepository,
                       UserTeamRepository userTeamRepository) {
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.teamRepository = teamRepository;
        this.userTeamRepository = userTeamRepository;
    }

    @Transactional
    public PollResponse create(Long userId, Long companyId, PollRequest req) {
        User creator = findUser(userId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new PollException("Company not found", HttpStatus.NOT_FOUND));

        Team team = resolveTeam(companyId, userId, req.getVisibility(), req.getTeamId());

        Poll poll = Poll.builder()
                .company(company)
                .team(team)
                .createdBy(creator)
                .question(req.getQuestion())
                .description(req.getDescription())
                .allowMultiple(Boolean.TRUE.equals(req.getAllowMultiple()))
                .closesAt(req.getClosesAt())
                .visibility(req.getVisibility())
                .build();
        poll = pollRepository.save(poll);

        saveOptions(poll, req.getOptions());

        return toResponse(poll, userId);
    }

    @Transactional(readOnly = true)
    public Page<PollResponse> list(Long requesterId, Long companyId, Pageable pageable) {
        Collection<Long> teamIds = teamIdsForQuery(requesterId, companyId);
        boolean hasOverride = hasOverride(Permissions.POLL_UPDATE);
        return pollRepository.findVisibleForUser(companyId, requesterId, teamIds, hasOverride, pageable)
                .map(poll -> toResponse(poll, requesterId));
    }

    @Transactional(readOnly = true)
    public PollResponse getById(Long pollId, Long requesterId, Long companyId) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanView(poll, requesterId);
        return toResponse(poll, requesterId);
    }

    @Transactional
    public PollResponse update(Long pollId, Long requesterId, Long companyId, PollRequest req) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanManage(poll, requesterId, Permissions.POLL_UPDATE);

        EventVisibility newVisibility = req.getVisibility() != null ? req.getVisibility() : poll.getVisibility();
        Team team = resolveTeam(companyId, requesterId, newVisibility, req.getTeamId());

        poll.setQuestion(req.getQuestion());
        poll.setDescription(req.getDescription());
        if (req.getAllowMultiple() != null) {
            poll.setAllowMultiple(req.getAllowMultiple());
        }
        poll.setClosesAt(req.getClosesAt());
        poll.setVisibility(newVisibility);
        poll.setTeam(team);
        poll = pollRepository.save(poll);

        // Replacing options wipes existing votes for this poll — acceptable
        // since editing the option set makes prior vote choices meaningless.
        // Votes must be deleted before options: poll_votes.option_id is a
        // NOT NULL FK with no ON DELETE CASCADE, so deleting options first
        // throws a DataIntegrityViolationException once anyone has voted.
        pollVoteRepository.deleteAllByPoll_Id(poll.getId());
        pollOptionRepository.deleteAllByPoll_Id(poll.getId());
        saveOptions(poll, req.getOptions());

        return toResponse(poll, requesterId);
    }

    @Transactional
    public void delete(Long pollId, Long requesterId, Long companyId) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanManage(poll, requesterId, Permissions.POLL_DELETE);
        poll.setActive(false);
        pollRepository.save(poll);
    }

    @Transactional
    public PollResponse vote(Long pollId, Long userId, Long companyId, VoteRequest req) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanView(poll, userId);

        if (poll.getClosesAt() != null && poll.getClosesAt().isBefore(java.time.LocalDateTime.now())) {
            throw new PollException("This poll is closed", HttpStatus.BAD_REQUEST);
        }

        List<PollOption> options = pollOptionRepository.findAllByPoll_IdOrderByDisplayOrderAsc(pollId);
        List<Long> validOptionIds = options.stream().map(PollOption::getId).toList();

        for (Long optionId : req.getOptionIds()) {
            if (!validOptionIds.contains(optionId)) {
                throw new PollException("Invalid option for this poll", HttpStatus.BAD_REQUEST);
            }
        }

        if (!Boolean.TRUE.equals(poll.getAllowMultiple()) && req.getOptionIds().size() > 1) {
            throw new PollException("This poll only allows a single choice", HttpStatus.BAD_REQUEST);
        }

        User voter = findUser(userId);

        // Replace any previous vote(s) by this user on this poll — keeps
        // both single-choice and multi-choice "change my vote" simple.
        pollVoteRepository.deleteAllByPoll_IdAndUser_Id(pollId, userId);

        for (Long optionId : req.getOptionIds()) {
            PollOption option = options.stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new PollException("Invalid option for this poll", HttpStatus.BAD_REQUEST));
            pollVoteRepository.save(PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .user(voter)
                    .build());
        }

        return toResponse(poll, userId);
    }

    @Transactional
    public PollResponse unvote(Long pollId, Long userId, Long companyId) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanView(poll, userId);

        if (poll.getClosesAt() != null && poll.getClosesAt().isBefore(java.time.LocalDateTime.now())) {
            throw new PollException("This poll is closed", HttpStatus.BAD_REQUEST);
        }

        pollVoteRepository.deleteAllByPoll_IdAndUser_Id(pollId, userId);
        return toResponse(poll, userId);
    }

    private void saveOptions(Poll poll, List<PollOptionRequest> optionRequests) {
        int order = 0;
        for (PollOptionRequest optionReq : optionRequests) {
            pollOptionRepository.save(PollOption.builder()
                    .poll(poll)
                    .optionText(optionReq.getText())
                    .displayOrder(order++)
                    .build());
        }
    }

    private PollResponse toResponse(Poll poll, Long requesterId) {
        List<PollOption> options = pollOptionRepository.findAllByPoll_IdOrderByDisplayOrderAsc(poll.getId());
        List<PollVote> votes = pollVoteRepository.findAllByPoll_Id(poll.getId());
        return PollMapper.toResponse(poll, options, votes, requesterId);
    }

    private void assertCanManage(Poll poll, Long requesterId, String overridePermission) {
        if (poll.getCreatedBy().getId().equals(requesterId)) {
            return;
        }
        if (!hasOverride(overridePermission)) {
            throw new PollException("Only the poll owner or a manager can perform this action", HttpStatus.FORBIDDEN);
        }
    }

    private void assertCanView(Poll poll, Long requesterId) {
        if (poll.getCreatedBy().getId().equals(requesterId)) {
            return;
        }
        if (hasOverride(Permissions.POLL_UPDATE)) {
            return;
        }
        switch (poll.getVisibility()) {
            case COMPANY -> {
                // No further restriction — every company member can view.
            }
            case TEAM -> {
                if (poll.getTeam() == null
                        || !userTeamRepository.existsByUser_IdAndTeam_Id(requesterId, poll.getTeam().getId())) {
                    throw new PollException("You do not have access to this poll", HttpStatus.FORBIDDEN);
                }
            }
            case PRIVATE -> throw new PollException("You do not have access to this poll", HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasOverride(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permission::equals);
    }

    // Mirrors PostService.resolveTeam(): COMPANY/PRIVATE polls carry no team,
    // TEAM polls must reference a real, active team in this company that the
    // creator actually belongs to.
    private Team resolveTeam(Long companyId, Long userId, EventVisibility visibility, Long teamId) {
        if (visibility != EventVisibility.TEAM) {
            return null;
        }
        if (teamId == null) {
            throw new PollException("Team is required for team visibility", HttpStatus.BAD_REQUEST);
        }
        Team team = teamRepository.findByIdAndCompany_IdAndActiveTrue(teamId, companyId)
                .orElseThrow(() -> new PollException("Team not found", HttpStatus.NOT_FOUND));
        boolean member = userTeamRepository.existsByUser_IdAndTeam_Id(userId, teamId);
        if (!member) {
            throw new PollException("You must be a member of the team", HttpStatus.FORBIDDEN);
        }
        return team;
    }

    private Collection<Long> teamIdsForQuery(Long userId, Long companyId) {
        List<Long> ids = userTeamRepository.findTeamIdsByUserAndCompany(userId, companyId);
        if (ids.isEmpty()) {
            return Collections.singletonList(NO_TEAM_SENTINEL);
        }
        return ids;
    }

    private Poll findCompanyPoll(Long pollId, Long companyId) {
        return pollRepository.findByIdAndCompany_Id(pollId, companyId)
                .orElseThrow(() -> new PollException("Poll not found", HttpStatus.NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new PollException("User not found", HttpStatus.NOT_FOUND));
    }
}