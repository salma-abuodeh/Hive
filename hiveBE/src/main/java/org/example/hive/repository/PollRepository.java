package org.example.hive.repository;

import org.example.hive.model.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @Query("""
            SELECT p FROM Poll p
            WHERE p.company.id = :companyId
              AND p.active = true
              AND (
                    :hasOverride = true
                 OR p.createdBy.id = :requesterId
                 OR p.visibility = org.example.hive.config.AppEnums.EventVisibility.COMPANY
                 OR (p.visibility = org.example.hive.config.AppEnums.EventVisibility.TEAM
                     AND p.team.id IN :teamIds)
              )
            """)
    Page<Poll> findVisibleForUser(
            @Param("companyId") Long companyId,
            @Param("requesterId") Long requesterId,
            @Param("teamIds") Collection<Long> teamIds,
            @Param("hasOverride") boolean hasOverride,
            Pageable pageable);

    Optional<Poll> findByIdAndCompany_Id(Long id, Long companyId);
}