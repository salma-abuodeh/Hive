package org.example.hive.repository;

import org.example.hive.model.MembershipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, Long> {
    List<MembershipRequest> findByCompany_IdAndStatusOrderByCreatedAtAsc(Long companyId, String status);
    List<MembershipRequest> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);
    boolean existsByRequester_IdAndCompany_IdAndStatus(Long requesterId, Long companyId, String status);
}
