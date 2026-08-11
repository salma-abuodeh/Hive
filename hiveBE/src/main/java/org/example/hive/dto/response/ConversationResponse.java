package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.hive.config.AppEnums.ConversationType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ConversationResponse {
    private Long id;
    private ConversationType conversationType;
    private Long companyId;
    private Long teamId;
    private Long createdByUserId;
    private String name;
    private List<ConversationParticipantResponse> participants;
    private MessageResponse lastMessage;
    private long unreadCount;
    private LocalDateTime createdAt;
}