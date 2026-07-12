package org.example.hive.repository;

import org.example.hive.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByIdAndCompanyId(Long id, Long companyId);

    Page<User> findAllByCompanyIdAndActiveTrue(Long companyId, Pageable pageable);
}
