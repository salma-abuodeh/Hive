package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobTitleResponse {
    private Long id;
    private String title;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
