package org.example.hive.repository;

import org.example.hive.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByDomain(String domain);
    boolean existsByDomain(String domain);
}
