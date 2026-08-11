package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationParticipantResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String jobTitle;
}