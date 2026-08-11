package org.example.hive.service;

import org.example.hive.config.AppEnums.ConversationType;
import org.example.hive.dto.request.AddConversationMembersRequest;
import org.example.hive.dto.request.CreateDirectConversationRequest;
import org.example.hive.dto.request.CreateGroupConversationRequest;
import org.example.hive.dto.response.ConversationResponse;
import org.example.hive.dto.response.MessageResponse;
import org.example.hive.exception.ConversationException;
import org.example.hive.mapper.ConversationMapper;
import org.example.hive.mapper.MessageMapper;
import org.example.hive.model.Company;
import org.example.hive.model.Conversation;
import org.example.hive.model.ConversationMember;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.repository.AttachmentRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.ConversationMemberRepository;
import org.example.hive.repository.ConversationRepository;
import org.example.hive.repository.MessageRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.Permissions;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository conversationMemberRepository,
                               MessageRepository messageRepository,
                               AttachmentRepository attachmentRepository,
                               UserRepository userRepository,
                               CompanyRepository companyRepository,
                               UserCompanyRepository userCompanyRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public ConversationResponse createDirect(Long requesterId, Long companyId, CreateDirectConversationRequest req) {
        Long targetUserId = req.getTargetUserId();
        if (targetUserId.equals(requesterId)) {
            throw new ConversationException("You cannot start a direct conversation with yourself", HttpStatus.BAD_REQUEST);
        }
        if (!userCompanyRepository.existsByUser_IdAndCompany_Id(targetUserId, companyId)) {
            throw new ConversationException("User not found in this company", HttpStatus.NOT_FOUND);
        }

        String directKey = directKey(requesterId, targetUserId);
        Conversation conversation = conversationRepository.findByCompany_IdAndDirectKey(companyId, directKey)
                .orElseGet(() -> {
                    Conversation created = conversationRepository.save(Conversation.builder()
                            .company(findCompany(companyId))
                            .conversationType(ConversationType.DIRECT)
                            .directKey(directKey)
                            .build());
                    conversationMemberRepository.save(ConversationMember.builder()
                            .conversation(created).user(findUser(requesterId)).build());
                    conversationMemberRepository.save(ConversationMember.builder()
                            .conversation(created).user(findUser(targetUserId)).build());
                    return created;
                });

        return toResponse(conversation, requesterId);
    }

    @Transactional
    public ConversationResponse createGroup(Long requesterId, Long companyId, CreateGroupConversationRequest req) {
        Company company = findCompany(companyId);
        User creator = findUser(requesterId);

        List<Long> participantIds = req.getParticipantUserIds().stream()
                .filter(id -> id != null && !id.equals(requesterId))
                .distinct()
                .toList();

        for (Long userId : participantIds) {
            if (!userCompanyRepository.existsByUser_IdAndCompany_Id(userId, companyId)) {
                throw new ConversationException("User not found in this company: " + userId, HttpStatus.NOT_FOUND);
            }
        }

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .company(company)
                .conversationType(ConversationType.GROUP)
                .name(req.getName().trim())
                .createdBy(creator)
                .build());

        conversationMemberRepository.save(ConversationMember.builder().conversation(conversation).user(creator).build());
        for (Long userId : participantIds) {
            conversationMemberRepository.save(ConversationMember.builder()
                    .conversation(conversation).user(findUser(userId)).build());
        }

        return toResponse(conversation, requesterId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long requesterId, Long companyId) {
        return conversationRepository.findAllForUser(requesterId, companyId).stream()
                .map(c -> toResponse(c, requesterId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(Long conversationId, Long requesterId, Long companyId) {
        Conversation conversation = findCompanyConversation(conversationId, companyId);
        assertMember(conversation, requesterId);
        return toResponse(conversation, requesterId);
    }

    @Transactional
    public ConversationResponse addMembers(Long conversationId, Long requesterId, Long companyId, AddConversationMembersRequest req) {
        Conversation conversation = findCompanyConversation(conversationId, companyId);
        assertGroupManageable(conversation);
        assertMember(conversation, requesterId);

        for (Long userId : req.getUserIds()) {
            if (userId == null || userId.equals(requesterId)) {
                continue;
            }
            if (!userCompanyRepository.existsByUser_IdAndCompany_Id(userId, companyId)) {
                throw new ConversationException("User not found in this company: " + userId, HttpStatus.NOT_FOUND);
            }
            if (conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId)) {
                continue;
            }
            conversationMemberRepository.save(ConversationMember.builder()
                    .conversation(conversation).user(findUser(userId)).build());
        }

        return toResponse(conversation, requesterId);
    }

    @Transactional
    public ConversationResponse removeMember(Long conversationId, Long requesterId, Long companyId, Long targetUserId) {
        Conversation conversation = findCompanyConversation(conversationId, companyId);
        assertGroupManageable(conversation);

        boolean isCreator = conversation.getCreatedBy() != null
                && conversation.getCreatedBy().getId().equals(requesterId);
        if (!isCreator && !hasOverride(Permissions.CONVERSATION_MANAGE_MEMBERS)) {
            throw new ConversationException("Only the group creator or a manager can remove other members", HttpStatus.FORBIDDEN);
        }

        if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, targetUserId)) {
            throw new ConversationException("User is not in this conversation", HttpStatus.NOT_FOUND);
        }
        conversationMemberRepository.deleteByConversation_IdAndUser_Id(conversationId, targetUserId);

        return toResponse(conversation, requesterId);
    }

    @Transactional
    public void leave(Long conversationId, Long requesterId, Long companyId) {
        Conversation conversation = findCompanyConversation(conversationId, companyId);
        assertGroupManageable(conversation);
        assertMember(conversation, requesterId);
        conversationMemberRepository.deleteByConversation_IdAndUser_Id(conversationId, requesterId);
    }

    @Transactional
    public void markRead(Long conversationId, Long requesterId, Long companyId) {
        findCompanyConversation(conversationId, companyId);
        ConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, requesterId)
                .orElseThrow(() -> new ConversationException("You are not a member of this conversation", HttpStatus.FORBIDDEN));
        member.setLastReadAt(LocalDateTime.now());
        conversationMemberRepository.save(member);
    }

    /** Used by MessageService so membership enforcement lives in one place. */
    @Transactional(readOnly = true)
    public Conversation requireMemberConversation(Long conversationId, Long requesterId, Long companyId) {
        Conversation conversation = findCompanyConversation(conversationId, companyId);
        assertMember(conversation, requesterId);
        return conversation;
    }

    // ==================== Team auto-sync (called from TeamService) ====================

    @Transactional
    public void createForTeam(Team team, User creator) {
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .company(team.getCompany())
                .conversationType(ConversationType.TEAM)
                .team(team)
                .build());
        conversationMemberRepository.save(ConversationMember.builder()
                .conversation(conversation).user(creator).build());
    }

    @Transactional
    public void syncAddTeamMember(Team team, User user) {
        conversationRepository.findByTeam_Id(team.getId()).ifPresent(conversation -> {
            if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversation.getId(), user.getId())) {
                conversationMemberRepository.save(ConversationMember.builder()
                        .conversation(conversation).user(user).build());
            }
        });
    }

    @Transactional
    public void syncRemoveTeamMember(Long teamId, Long userId) {
        conversationRepository.findByTeam_Id(teamId).ifPresent(conversation ->
                conversationMemberRepository.deleteByConversation_IdAndUser_Id(conversation.getId(), userId));
    }

    // ==================== helpers ====================

    private String directKey(Long a, Long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        return lo + "_" + hi;
    }

    private void assertGroupManageable(Conversation conversation) {
        if (conversation.getConversationType() != ConversationType.GROUP) {
            throw new ConversationException(
                    "Membership for this conversation type is managed automatically", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertMember(Conversation conversation, Long userId) {
        if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversation.getId(), userId)) {
            throw new ConversationException("You do not have access to this conversation", HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasOverride(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permission::equals);
    }

    private ConversationResponse toResponse(Conversation conversation, Long requesterId) {
        List<ConversationMember> members = conversationMemberRepository.findAllByConversation_Id(conversation.getId());

        MessageResponse lastMessage = messageRepository.findTopByConversation_IdOrderByCreatedAtDesc(conversation.getId())
                .map(m -> MessageMapper.toResponse(m, attachmentRepository.findAllByMessage_IdOrderByIdAsc(m.getId())))
                .orElse(null);

        LocalDateTime lastReadAt = members.stream()
                .filter(m -> m.getUser().getId().equals(requesterId))
                .findFirst()
                .map(ConversationMember::getLastReadAt)
                .orElse(null);
        long unreadCount = lastReadAt == null
                ? messageRepository.countByConversation_Id(conversation.getId())
                : messageRepository.countByConversation_IdAndCreatedAtAfter(conversation.getId(), lastReadAt);

        return ConversationMapper.toResponse(conversation, members, lastMessage, unreadCount);
    }

    private Conversation findCompanyConversation(Long conversationId, Long companyId) {
        return conversationRepository.findByIdAndCompany_Id(conversationId, companyId)
                .orElseThrow(() -> new ConversationException("Conversation not found", HttpStatus.NOT_FOUND));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ConversationException("Company not found", HttpStatus.NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ConversationException("User not found", HttpStatus.NOT_FOUND));
    }
}