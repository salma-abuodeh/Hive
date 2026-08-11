package org.example.hive.conversations;

import org.example.hive.config.AppEnums.MessageType;
import org.example.hive.dto.request.CreateDirectConversationRequest;
import org.example.hive.integration.BaseIntegrationTest;
import org.example.hive.model.Company;
import org.example.hive.model.Conversation;
import org.example.hive.model.Message;
import org.example.hive.model.Role;
import org.example.hive.model.User;
import org.example.hive.repository.ConversationRepository;
import org.example.hive.repository.MessageRepository;
import org.example.hive.security.Permissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationCrudIT extends BaseIntegrationTest {

    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    void shouldCreateDirectConversation_whenNeitherMemberHasReadAnything() throws Exception {
        Company company = factory.createCompany();
        Role role = factory.createRole(company);

        User requester = factory.createUser();
        User target = factory.createUser();
        factory.assignUserToCompany(requester, company, role);
        factory.assignUserToCompany(target, company, role);

        String token = jwtHelper.generate(requester, company, List.of(Permissions.CONVERSATION_CREATE));

        CreateDirectConversationRequest request = new CreateDirectConversationRequest();
        request.setTargetUserId(target.getId());

        // Before the fix, this 500'd every time: lastReadAt is null for a brand-new member,
        // and the old single-query countUnread could not bind that null parameter on Postgres.
        mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationType").value("DIRECT"))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void shouldCountOnlyMessagesSentAfterLastRead() throws Exception {
        Company company = factory.createCompany();
        Role role = factory.createRole(company);

        User requester = factory.createUser();
        User target = factory.createUser();
        factory.assignUserToCompany(requester, company, role);
        factory.assignUserToCompany(target, company, role);

        String requesterToken = jwtHelper.generate(requester, company, List.of(Permissions.CONVERSATION_CREATE));
        String targetToken = jwtHelper.generate(target, company, List.of(Permissions.CONVERSATION_VIEW));

        CreateDirectConversationRequest request = new CreateDirectConversationRequest();
        request.setTargetUserId(target.getId());

        String body = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long conversationId = objectMapper.readTree(body).get("id").asLong();
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();

        // A message sent before target has ever read: with lastReadAt still null,
        // this exercises countByConversation_Id.
        messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(requester)
                .content("first message")
                .messageType(MessageType.TEXT)
                .build());

        mockMvc.perform(get("/conversations/{id}", conversationId)
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Mark read, then send a second message: now lastReadAt is non-null, exercising
        // countByConversation_IdAndCreatedAtAfter instead.
        mockMvc.perform(put("/conversations/{id}/read", conversationId)
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isNoContent());

        // Guard against timestamp ties: markRead's lastReadAt and the next message's createdAt
        // both use LocalDateTime.now(), and the query relies on strict '>' comparison.
        Thread.sleep(10);

        messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(requester)
                .content("second message")
                .messageType(MessageType.TEXT)
                .build());

        mockMvc.perform(get("/conversations/{id}", conversationId)
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }
}