package org.example.hive.repository;

import org.example.hive.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<User> findAllByActive(Boolean active, Pageable pageable);

    @Query("""
            select distinct u from User u
            join u.memberships m
            where m.company.id in :companyIds
            """)
    Page<User> findMembersInCompanies(@Param("companyIds") Collection<Long> companyIds, Pageable pageable);

    @Query("""
            select distinct u from User u
            join u.memberships m
            where m.company.id in :companyIds and u.active = :active
            """)
    Page<User> findMembersInCompaniesAndActive(
            @Param("companyIds") Collection<Long> companyIds,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            select u from User u
            join u.memberships m
            where u.id = :id and m.company.id in :companyIds
            """)
    Optional<User> findByIdInCompanies(
            @Param("id") Long id,
            @Param("companyIds") Collection<Long> companyIds);
}