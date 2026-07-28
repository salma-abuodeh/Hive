package org.example.hive.service;

import org.example.hive.config.AppEnums.RsvpStatus;
import org.example.hive.dto.request.EventRequest;
import org.example.hive.dto.request.InviteUsersRequest;
import org.example.hive.dto.request.RsvpUpdateRequest;
import org.example.hive.dto.response.EventResponse;
import org.example.hive.dto.response.EventRsvpResponse;
import org.example.hive.exception.EventException;
import org.example.hive.mapper.EventMapper;
import org.example.hive.model.Company;
import org.example.hive.model.Event;
import org.example.hive.model.EventRsvp;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.EventRepository;
import org.example.hive.repository.EventRsvpRepository;
import org.example.hive.repository.UserCompanyRepository;
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
public class EventService {

    private final EventRepository eventRepository;
    private final EventRsvpRepository eventRsvpRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;

    public EventService(EventRepository eventRepository,
                        EventRsvpRepository eventRsvpRepository,
                        UserRepository userRepository,
                        CompanyRepository companyRepository,
                        UserCompanyRepository userCompanyRepository) {
        this.eventRepository = eventRepository;
        this.eventRsvpRepository = eventRsvpRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public EventResponse create(Long userId, Long companyId, EventRequest req) {
        User creator = findUser(userId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EventException("Company not found", HttpStatus.NOT_FOUND));

        validateTimes(req);

        Event event = Event.builder()
                .company(company)
                .createdBy(creator)
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .visibility(req.getVisibility())
                .build();

        if (req.getTeamId() != null) {
            event.setTeam(Team.builder().id(req.getTeamId()).build());
        }

        event = eventRepository.save(event);
        return EventMapper.toResponse(event, null);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> list(Long requesterId, Long companyId, Pageable pageable) {
        return eventRepository.findAllByCompany_IdAndActiveTrue(companyId, pageable)
                .map(event -> toResponseWithMyRsvp(event, requesterId));
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long eventId, Long requesterId, Long companyId) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanView(event, requesterId);
        return toResponseWithMyRsvp(event, requesterId);
    }

    @Transactional
    public EventResponse update(Long eventId, Long requesterId, Long companyId, EventRequest req) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanManage(event, requesterId, Permissions.EVENT_UPDATE);
        validateTimes(req);

        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setLocation(req.getLocation());
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        if (req.getVisibility() != null) {
            event.setVisibility(req.getVisibility());
        }

        event = eventRepository.save(event);
        return toResponseWithMyRsvp(event, requesterId);
    }

    @Transactional
    public void delete(Long eventId, Long requesterId, Long companyId) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanManage(event, requesterId, Permissions.EVENT_DELETE);
        event.setActive(false);
        eventRepository.save(event);
    }

    @Transactional
    public void invite(Long eventId, Long requesterId, Long companyId, InviteUsersRequest req) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanManage(event, requesterId, Permissions.EVENT_INVITE);

        for (Long invitedUserId : req.getUserIds()) {
            if (eventRsvpRepository.existsByEvent_IdAndUser_Id(eventId, invitedUserId)) {
                continue;
            }
            if (!userCompanyRepository.existsByUser_IdAndCompany_Id(invitedUserId, companyId)) {
                throw new EventException(
                        "User " + invitedUserId + " is not a member of this company", HttpStatus.BAD_REQUEST);
            }
            User invitee = findUser(invitedUserId);
            eventRsvpRepository.save(EventRsvp.builder()
                    .event(event)
                    .user(invitee)
                    .status(RsvpStatus.INVITED)
                    .build());
        }
    }

    @Transactional
    public void removeInvitation(Long eventId, Long requesterId, Long companyId, Long targetUserId) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanManage(event, requesterId, Permissions.EVENT_INVITE);
        eventRsvpRepository.deleteByEvent_IdAndUser_Id(eventId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<EventRsvpResponse> listInvitations(Long eventId, Long requesterId, Long companyId) {
        Event event = findCompanyEvent(eventId, companyId);
        assertCanManage(event, requesterId, Permissions.EVENT_INVITE);
        return eventRsvpRepository.findAllByEvent_Id(eventId).stream()
                .map(EventMapper::toRsvpResponse)
                .toList();
    }

    @Transactional
    public EventRsvpResponse respondToInvitation(Long eventId, Long userId, RsvpUpdateRequest req) {
        if (req.getStatus() == RsvpStatus.INVITED) {
            throw new EventException("Invalid RSVP status", HttpStatus.BAD_REQUEST);
        }

        EventRsvp rsvp = eventRsvpRepository.findByEvent_IdAndUser_Id(eventId, userId)
                .orElseThrow(() -> new EventException("You are not invited to this event", HttpStatus.FORBIDDEN));

        rsvp.setStatus(req.getStatus());
        rsvp = eventRsvpRepository.save(rsvp);
        return EventMapper.toRsvpResponse(rsvp);
    }

    private EventResponse toResponseWithMyRsvp(Event event, Long requesterId) {
        RsvpStatus myStatus = eventRsvpRepository.findByEvent_IdAndUser_Id(event.getId(), requesterId)
                .map(EventRsvp::getStatus)
                .orElse(null);
        return EventMapper.toResponse(event, myStatus);
    }

    private void assertCanManage(Event event, Long requesterId, String overridePermission) {
        if (event.getCreatedBy().getId().equals(requesterId)) {
            return;
        }
        boolean hasOverride = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(overridePermission::equals);
        if (!hasOverride) {
            throw new EventException("Only the event owner or a manager can perform this action", HttpStatus.FORBIDDEN);
        }
    }

    private void assertCanView(Event event, Long requesterId) {
        if (event.getCreatedBy().getId().equals(requesterId)) {
            return;
        }
        switch (event.getVisibility()) {
            case COMPANY, TEAM -> {
                // TEAM treated same as COMPANY for now — no TeamRepository/
                // UserTeamRepository exists yet to check real membership.
                //when we will merge the code that contains teams we will change this
            }
            case PRIVATE -> {
                if (!eventRsvpRepository.existsByEvent_IdAndUser_Id(event.getId(), requesterId)) {
                    throw new EventException("You do not have access to this event", HttpStatus.FORBIDDEN);
                }
            }
        }
    }

    private void validateTimes(EventRequest req) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new EventException("End time must be after start time", HttpStatus.BAD_REQUEST);
        }
    }

    private Event findCompanyEvent(Long eventId, Long companyId) {
        return eventRepository.findByIdAndCompany_Id(eventId, companyId)
                .orElseThrow(() -> new EventException("Event not found", HttpStatus.NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EventException("User not found", HttpStatus.NOT_FOUND));
    }
}