package org.example.hive.repository;

import org.example.hive.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT e FROM Event e
            WHERE e.company.id = :companyId
              AND e.active = true
              AND (
                    :hasOverride = true
                 OR e.createdBy.id = :requesterId
                 OR e.visibility = org.example.hive.config.AppEnums.EventVisibility.COMPANY
                 OR (e.visibility = org.example.hive.config.AppEnums.EventVisibility.TEAM
                     AND e.team.id IN :teamIds)
                 OR (e.visibility = org.example.hive.config.AppEnums.EventVisibility.PRIVATE
                     AND EXISTS (SELECT 1 FROM EventRsvp r WHERE r.event = e AND r.user.id = :requesterId))
              )
            """)
    Page<Event> findVisibleForUser(
            @Param("companyId") Long companyId,
            @Param("requesterId") Long requesterId,
            @Param("teamIds") Collection<Long> teamIds,
            @Param("hasOverride") boolean hasOverride,
            Pageable pageable);

    Optional<Event> findByIdAndCompany_Id(Long id, Long companyId);
}