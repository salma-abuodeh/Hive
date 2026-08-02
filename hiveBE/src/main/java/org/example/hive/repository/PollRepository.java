package org.example.hive.repository;

import org.example.hive.model.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    Page<Poll> findAllByCompany_IdAndActiveTrue(Long companyId, Pageable pageable);

    Optional<Poll> findByIdAndCompany_Id(Long id, Long companyId);
}