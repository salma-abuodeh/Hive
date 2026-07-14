package org.example.hive.repository;

import org.example.hive.domain.UserCompanyMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCompanyMembershipRepository extends JpaRepository<UserCompanyMembership, Long> {
    List<UserCompanyMembership> findByUserId(Long userId);
    List<UserCompanyMembership> findByCompanyId(Long companyId);
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);
    void deleteByUserIdAndCompanyId(Long userId, Long companyId);
}