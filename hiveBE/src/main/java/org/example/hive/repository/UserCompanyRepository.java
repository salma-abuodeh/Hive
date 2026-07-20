package org.example.hive.repository;

import org.example.hive.model.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    Optional<UserCompany> findFirstByUser_IdAndActiveTrueOrderByJoinedAtDesc(Long userId);

    Optional<UserCompany> findByUser_IdAndCompany_Id(Long userId, Long companyId);

    boolean existsByUser_IdAndCompany_Id(Long userId, Long companyId);

    Page<UserCompany> findAllByCompany_IdAndActiveTrue(Long companyId, Pageable pageable);
}