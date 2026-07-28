package org.example.hive.repository;

import org.example.hive.model.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, Long> {

    List<EventRsvp> findAllByEvent_Id(Long eventId);

    Optional<EventRsvp> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    void deleteByEvent_IdAndUser_Id(Long eventId, Long userId);
}