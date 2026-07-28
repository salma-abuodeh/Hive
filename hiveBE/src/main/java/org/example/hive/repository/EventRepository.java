package org.example.hive.repository;

import org.example.hive.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByCompany_IdAndActiveTrue(Long companyId, Pageable pageable);

    Optional<Event> findByIdAndCompany_Id(Long id, Long companyId);
}