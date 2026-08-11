package org.example.hive.service;

import org.example.hive.config.AppEnums.ConversationType;
import org.example.hive.dto.request.CreateDirectConversationRequest;
import org.example.hive.dto.response.ConversationResponse;
import org.example.hive.model.Company;
import org.example.hive.model.Conversation;
import org.example.hive.model.ConversationMember;
import org.example.hive.model.User;
import org.example.hive.repository.AttachmentRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.ConversationMemberRepository;
import org.example.hive.repository.ConversationRepository;
import org.example.hive.repository.MessageRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Company company;
    private User requester;
    private User target;
    private Conversation conversation;

    private void commonStubs(LocalDateTime requesterLastReadAt) {
        company = Company.builder().id(1L).name("Acme").build();

        requester = User.builder().id(10L).firstName("Req").lastName("Uester")
                .email("req@acme.test").build();
        target = User.builder().id(20L).firstName("Tar").lastName("Get")
                .email("tar@acme.test").build();

        conversation = Conversation.builder()
                .id(99L)
                .company(company)
                .conversationType(ConversationType.DIRECT)
                .directKey("10_20")
                .build();

        ConversationMember requesterMember = ConversationMember.builder()
                .id(1L).conversation(conversation).user(requester).lastReadAt(requesterLastReadAt).build();
        ConversationMember targetMember = ConversationMember.builder()
                .id(2L).conversation(conversation).user(target).lastReadAt(null).build();

        when(userCompanyRepository.existsByUser_IdAndCompany_Id(target.getId(), company.getId())).thenReturn(true);
        when(conversationRepository.findByCompany_IdAndDirectKey(company.getId(), "10_20"))
                .thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findAllByConversation_Id(conversation.getId()))
                .thenReturn(List.of(requesterMember, targetMember));
        when(messageRepository.findTopByConversation_IdOrderByCreatedAtDesc(conversation.getId()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Uses countByConversation_Id (no nullable param) when the member has never read the conversation")
    void toResponse_neverRead_usesNonNullableCountQuery() {
        commonStubs(null);
        when(messageRepository.countByConversation_Id(conversation.getId())).thenReturn(3L);

        CreateDirectConversationRequest req = new CreateDirectConversationRequest();
        req.setTargetUserId(target.getId());

        ConversationResponse response = conversationService.createDirect(requester.getId(), company.getId(), req);

        assertThat(response.getUnreadCount()).isEqualTo(3L);
        verify(messageRepository).countByConversation_Id(conversation.getId());
        verify(messageRepository, never()).countByConversation_IdAndCreatedAtAfter(anyLong(), any());
    }

    @Test
    @DisplayName("Uses countByConversation_IdAndCreatedAtAfter when the member has a lastReadAt")
    void toResponse_hasReadBefore_usesCreatedAtAfterQuery() {
        LocalDateTime lastReadAt = LocalDateTime.now().minusHours(1);
        commonStubs(lastReadAt);
        when(messageRepository.countByConversation_IdAndCreatedAtAfter(conversation.getId(), lastReadAt))
                .thenReturn(1L);

        CreateDirectConversationRequest req = new CreateDirectConversationRequest();
        req.setTargetUserId(target.getId());

        ConversationResponse response = conversationService.createDirect(requester.getId(), company.getId(), req);

        assertThat(response.getUnreadCount()).isEqualTo(1L);
        verify(messageRepository).countByConversation_IdAndCreatedAtAfter(eq(conversation.getId()), eq(lastReadAt));
        verify(messageRepository, never()).countByConversation_Id(anyLong());
    }
}