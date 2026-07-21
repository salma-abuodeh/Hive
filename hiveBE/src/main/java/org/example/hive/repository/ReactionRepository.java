package org.example.hive.repository;

import org.example.hive.config.AppEnums.ReactionType;
import org.example.hive.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByUser_IdAndPost_Id(Long userId, Long postId);

    long countByPost_IdAndReactionType(Long postId, ReactionType reactionType);

    boolean existsByUser_IdAndPost_IdAndReactionType(Long userId, Long postId, ReactionType reactionType);

    @Query("""
            SELECT r.post.id, COUNT(r)
            FROM Reaction r
            WHERE r.post.id IN :postIds AND r.reactionType = :type
            GROUP BY r.post.id
            """)
    List<Object[]> countByPostIds(
            @Param("postIds") Collection<Long> postIds,
            @Param("type") ReactionType type);

    @Query("""
            SELECT r.post.id
            FROM Reaction r
            WHERE r.user.id = :userId
              AND r.post.id IN :postIds
              AND r.reactionType = :type
            """)
    List<Long> findLikedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") Collection<Long> postIds,
            @Param("type") ReactionType type);
}
