package org.example.hive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDirectConversationRequest {

    @NotNull(message = "targetUserId is required")
    private Long targetUserId;
}