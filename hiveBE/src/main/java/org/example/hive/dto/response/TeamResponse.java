package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private long memberCount;
    private List<TeamMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
