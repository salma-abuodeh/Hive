package org.example.hive.repository;

import org.example.hive.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByIdAndCompany_IdAndActiveTrue(Long id, Long companyId);
}
