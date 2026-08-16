package org.example.hive.repository;

import org.example.hive.model.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    Optional<UserCompany> findFirstByUser_IdAndActiveTrueOrderByJoinedAtDesc(Long userId);

    Optional<UserCompany> findByUser_IdAndCompany_Id(Long userId, Long companyId);

    Optional<UserCompany> findByUser_IdAndCompany_IdAndActiveTrue(Long userId, Long companyId);

    boolean existsByUser_IdAndCompany_Id(Long userId, Long companyId);

    List<UserCompany> findAllByUser_IdAndActiveTrue(Long userId);

    List<UserCompany> findAllByCompany_IdAndActiveTrueOrderByJoinedAtAsc(Long companyId);

    List<UserCompany> findAllByUser_Id(Long userId);

    Page<UserCompany> findAllByCompany_IdAndActiveTrue(Long companyId, Pageable pageable);

    Page<UserCompany> findAllByCompany_IdInAndActiveTrue(Collection<Long> companyIds, Pageable pageable);

    Page<UserCompany> findAllByCompany_IdInAndActive(Collection<Long> companyIds, Boolean active, Pageable pageable);

    Optional<UserCompany> findByUser_IdAndCompany_IdInAndActiveTrue(Long userId, Collection<Long> companyIds);

    void deleteAllByUser_Id(Long userId);
}
