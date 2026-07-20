package org.example.hive.repository;

import org.example.hive.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameAndCompanyIsNull(String name);
}
