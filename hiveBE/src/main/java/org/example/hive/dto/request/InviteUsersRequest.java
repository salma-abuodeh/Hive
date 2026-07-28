package org.example.hive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InviteUsersRequest {

    @NotEmpty(message = "At least one user id is required")
    private List<Long> userIds;
}