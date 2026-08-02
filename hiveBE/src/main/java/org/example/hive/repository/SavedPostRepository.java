package org.example.hive.repository;

import org.example.hive.model.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    Optional<SavedPost> findByUser_IdAndPost_Id(Long userId, Long postId);

    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);

    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

    @Query("""
            SELECT sp.post.id
            FROM SavedPost sp
            WHERE sp.user.id = :userId AND sp.post.id IN :postIds
            """)
    List<Long> findSavedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") Collection<Long> postIds);
}
