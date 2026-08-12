package org.example.hive.mapper;

import org.example.hive.dto.response.ConversationParticipantResponse;
import org.example.hive.dto.response.ConversationResponse;
import org.example.hive.dto.response.MessageResponse;
import org.example.hive.model.Conversation;
import org.example.hive.model.ConversationMember;

import java.util.List;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationResponse toResponse(
            Conversation conversation,
            List<ConversationMember> members,
            MessageResponse lastMessage,
            long unreadCount) {

        List<ConversationParticipantResponse> participants = members.stream()
                .map(m -> ConversationParticipantResponse.builder()
                        .userId(m.getUser().getId())
                        .firstName(m.getUser().getFirstName())
                        .lastName(m.getUser().getLastName())
                        .email(m.getUser().getEmail())
                        .jobTitle(m.getUser().getJobTitle())
                        .build())
                .toList();

        return ConversationResponse.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .companyId(conversation.getCompany().getId())
                .teamId(conversation.getTeam() != null ? conversation.getTeam().getId() : null)
                .createdByUserId(conversation.getCreatedBy() != null ? conversation.getCreatedBy().getId() : null)
                // Team conversations are named after their team. This also covers
                // conversations created before the name was persisted on the
                // conversation record.
                .name(conversation.getTeam() != null ? conversation.getTeam().getName() : conversation.getName())
                .participants(participants)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
