package org.example.hive.repository;

import org.example.hive.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT p FROM Post p
            WHERE p.company.id = :companyId
              AND p.active = true
              AND (
                    p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.COMPANY
                 OR (p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.TEAM
                     AND p.team.id IN :teamIds)
              )
            """)
    Page<Post> findVisibleFeed(
            @Param("companyId") Long companyId,
            @Param("teamIds") Collection<Long> teamIds,
            Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE p.id = :postId
              AND p.company.id = :companyId
              AND p.active = true
              AND (
                    p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.COMPANY
                 OR (p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.TEAM
                     AND p.team.id IN :teamIds)
              )
            """)
    Optional<Post> findVisibleById(
            @Param("postId") Long postId,
            @Param("companyId") Long companyId,
            @Param("teamIds") Collection<Long> teamIds);

    @Query("""
            SELECT p FROM Post p
            JOIN SavedPost sp ON sp.post = p
            WHERE sp.user.id = :userId
              AND p.company.id = :companyId
              AND p.active = true
              AND (
                    p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.COMPANY
                 OR (p.visibilityType = org.example.hive.config.AppEnums.VisibilityType.TEAM
                     AND p.team.id IN :teamIds)
              )
            """)
    Page<Post> findSavedVisibleFeed(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId,
            @Param("teamIds") Collection<Long> teamIds,
            Pageable pageable);
}
