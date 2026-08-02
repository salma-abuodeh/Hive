package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.hive.config.AppEnums.RsvpStatus;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventRsvpResponse {
    private Long userId;
    private String userName;
    private RsvpStatus status;
    private LocalDateTime respondedAt;
}