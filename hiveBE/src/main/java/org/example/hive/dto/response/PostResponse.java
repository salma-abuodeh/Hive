package org.example.hive.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.hive.config.AppEnums.PostType;
import org.example.hive.config.AppEnums.ReactionType;
import org.example.hive.config.AppEnums.VisibilityType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private String content;
    private PostType postType;
    private VisibilityType visibilityType;
    private Long teamId;
    private String teamName;
    private Long authorId;
    private String authorFirstName;
    private String authorLastName;
    private String authorRoleName;
    private String authorJobTitle;
    private long likeCount;
    private long commentCount;
    private boolean likedByMe;
    private ReactionType myReaction;
    private boolean savedByMe;
    private boolean ownedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;
}