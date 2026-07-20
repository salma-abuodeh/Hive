package org.example.hive.repository;

import org.example.hive.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByActiveTrue();

    boolean existsByDomain(String domain);
}