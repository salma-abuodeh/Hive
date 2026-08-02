package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.hive.config.AppEnums.EventVisibility;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PollResponse {
    private Long id;
    private Long companyId;
    private Long createdByUserId;
    private String createdByName;
    private String question;
    private String description;
    private Boolean allowMultiple;
    private LocalDateTime closesAt;
    private EventVisibility visibility;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long totalVotes;
    private List<PollOptionResponse> options;
}