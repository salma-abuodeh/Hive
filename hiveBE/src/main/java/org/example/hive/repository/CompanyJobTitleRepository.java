package org.example.hive.repository;

import org.example.hive.model.CompanyJobTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyJobTitleRepository extends JpaRepository<CompanyJobTitle, Long> {

    List<CompanyJobTitle> findAllByCompany_IdAndActiveTrueOrderByTitleAsc(Long companyId);

    List<CompanyJobTitle> findAllByCompany_IdOrderByTitleAsc(Long companyId);

    Optional<CompanyJobTitle> findByIdAndCompany_IdAndActiveTrue(Long id, Long companyId);

    Optional<CompanyJobTitle> findByIdAndCompany_Id(Long id, Long companyId);

    boolean existsByCompany_IdAndTitleIgnoreCase(Long companyId, String title);
}
