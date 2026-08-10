package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.hive.config.AppEnums.EventVisibility;
import org.example.hive.config.AppEnums.RsvpStatus;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private Long companyId;
    private Long teamId;
    private Long createdByUserId;
    private String createdByName;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventVisibility visibility;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RsvpStatus myRsvpStatus;
    private String coverUrl;
}