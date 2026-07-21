package org.example.hive.repository;

import org.example.hive.model.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    @Query("""
            SELECT ut.team.id
            FROM UserTeam ut
            WHERE ut.user.id = :userId
              AND ut.team.company.id = :companyId
              AND ut.team.active = true
            """)
    List<Long> findTeamIdsByUserAndCompany(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    boolean existsByUser_IdAndTeam_Id(Long userId, Long teamId);
}
