package org.example.hive.repository;

import org.example.hive.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPost_IdAndActiveTrueOrderByCreatedAtAsc(Long postId);

    long countByPost_IdAndActiveTrue(Long postId);

    Optional<Comment> findByIdAndActiveTrue(Long id);

    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Comment c
            WHERE c.post.id IN :postIds AND c.active = true
            GROUP BY c.post.id
            """)
    List<Object[]> countActiveByPostIds(@Param("postIds") Collection<Long> postIds);
}
