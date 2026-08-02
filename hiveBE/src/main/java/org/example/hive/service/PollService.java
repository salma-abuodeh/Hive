package org.example.hive.service;

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
import org.example.hive.model.User;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.PollOptionRepository;
import org.example.hive.repository.PollRepository;
import org.example.hive.repository.PollVoteRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.Permissions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public PollService(PollRepository pollRepository,
                       PollOptionRepository pollOptionRepository,
                       PollVoteRepository pollVoteRepository,
                       UserRepository userRepository,
                       CompanyRepository companyRepository) {
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public PollResponse create(Long userId, Long companyId, PollRequest req) {
        User creator = findUser(userId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new PollException("Company not found", HttpStatus.NOT_FOUND));

        Poll poll = Poll.builder()
                .company(company)
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
        return pollRepository.findAllByCompany_IdAndActiveTrue(companyId, pageable)
                .map(poll -> toResponse(poll, requesterId));
    }

    @Transactional(readOnly = true)
    public PollResponse getById(Long pollId, Long requesterId, Long companyId) {
        Poll poll = findCompanyPoll(pollId, companyId);
        return toResponse(poll, requesterId);
    }

    @Transactional
    public PollResponse update(Long pollId, Long requesterId, Long companyId, PollRequest req) {
        Poll poll = findCompanyPoll(pollId, companyId);
        assertCanManage(poll, requesterId, Permissions.POLL_UPDATE);

        poll.setQuestion(req.getQuestion());
        poll.setDescription(req.getDescription());
        if (req.getAllowMultiple() != null) {
            poll.setAllowMultiple(req.getAllowMultiple());
        }
        poll.setClosesAt(req.getClosesAt());
        if (req.getVisibility() != null) {
            poll.setVisibility(req.getVisibility());
        }
        poll = pollRepository.save(poll);

        // Replacing options wipes existing votes for this poll — acceptable
        // since editing the option set makes prior vote choices meaningless.
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
        boolean hasOverride = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(overridePermission::equals);
        if (!hasOverride) {
            throw new PollException("Only the poll owner or a manager can perform this action", HttpStatus.FORBIDDEN);
        }
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