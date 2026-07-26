package org.example.hive.mapper;

import org.example.hive.config.AppEnums.ReactionType;
import org.example.hive.dto.response.CommentResponse;
import org.example.hive.dto.response.PostResponse;
import org.example.hive.model.Comment;
import org.example.hive.model.Post;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toResponse(
            Post post,
            String authorRoleName,
            long likeCount,
            long commentCount,
            ReactionType myReaction,
            boolean savedByMe,
            Long currentUserId) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .postType(post.getPostType())
                .visibilityType(post.getVisibilityType())
                .teamId(post.getTeam() != null ? post.getTeam().getId() : null)
                .teamName(post.getTeam() != null ? post.getTeam().getName() : null)
                .authorId(post.getAuthor().getId())
                .authorFirstName(post.getAuthor().getFirstName())
                .authorLastName(post.getAuthor().getLastName())
                .authorRoleName(authorRoleName)
                .authorJobTitle(post.getAuthor().getJobTitle())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByMe(myReaction != null)
                .myReaction(myReaction)
                .savedByMe(savedByMe)
                .ownedByMe(currentUserId != null && currentUserId.equals(post.getAuthor().getId()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static CommentResponse toCommentResponse(Comment comment, Long currentUserId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .authorId(comment.getUser().getId())
                .authorFirstName(comment.getUser().getFirstName())
                .authorLastName(comment.getUser().getLastName())
                .ownedByMe(currentUserId != null && currentUserId.equals(comment.getUser().getId()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
