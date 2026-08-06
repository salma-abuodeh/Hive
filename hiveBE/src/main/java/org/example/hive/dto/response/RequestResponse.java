package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class RequestResponse {
    private Long id; private String status; private String requesterName; private String requesterEmail;
    private Long companyId; private String companyName; private String rejectionReason;
    private LocalDateTime createdAt; private LocalDateTime reviewedAt;
}
