package org.example.hive.repository;

import org.example.hive.model.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    Optional<UserTeam> findByUser_IdAndTeam_Id(Long userId, Long teamId);

    List<UserTeam> findAllByTeam_IdOrderByJoinedAtAsc(Long teamId);

    List<UserTeam> findAllByUser_IdAndTeam_Company_IdAndTeam_ActiveTrue(Long userId, Long companyId);

    List<UserTeam> findAllByUser_IdAndTeam_Company_Id(Long userId, Long companyId);

    long countByTeam_Id(Long teamId);

    @Query("""
            SELECT ut.team.id, COUNT(ut)
            FROM UserTeam ut
            WHERE ut.team.id IN :teamIds
            GROUP BY ut.team.id
            """)
    List<Object[]> countMembersByTeamIds(@Param("teamIds") Collection<Long> teamIds);

    void deleteByUser_IdAndTeam_Id(Long userId, Long teamId);
}
