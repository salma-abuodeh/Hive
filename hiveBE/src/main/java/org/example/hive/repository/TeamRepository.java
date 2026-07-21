package org.example.hive.repository;

import org.example.hive.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findAllByCompany_IdAndActiveTrueOrderByNameAsc(Long companyId);

    List<Team> findAllByCompany_IdOrderByNameAsc(Long companyId);

    Optional<Team> findByIdAndCompany_IdAndActiveTrue(Long id, Long companyId);

    Optional<Team> findByIdAndCompany_Id(Long id, Long companyId);

    boolean existsByCompany_IdAndNameIgnoreCase(Long companyId, String name);
}
