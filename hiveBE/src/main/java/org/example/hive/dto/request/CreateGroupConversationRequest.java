package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateGroupConversationRequest {

    @NotBlank(message = "Group name is required")
    @Size(max = 255)
    private String name;

    @NotEmpty(message = "At least one other participant is required")
    private List<Long> participantUserIds;
}