package org.example.hive.repository;

import org.example.hive.model.CompanyApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyApplicationRepository extends JpaRepository<CompanyApplication, Long> {
    List<CompanyApplication> findByStatusOrderByCreatedAtAsc(String status);
    List<CompanyApplication> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);
    boolean existsByRequester_IdAndStatus(Long requesterId, String status);
}
