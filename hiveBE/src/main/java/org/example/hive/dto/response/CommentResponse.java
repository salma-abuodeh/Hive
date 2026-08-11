package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private String content;
    private Long authorId;
    private String authorFirstName;
    private String authorLastName;
    private boolean ownedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;
}