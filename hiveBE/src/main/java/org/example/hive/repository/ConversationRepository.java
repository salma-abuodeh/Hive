package org.example.hive.repository;

import org.example.hive.config.AppEnums.ConversationType;
import org.example.hive.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByCompany_IdAndDirectKey(Long companyId, String directKey);

    Optional<Conversation> findByTeam_Id(Long teamId);

    Optional<Conversation> findByIdAndCompany_Id(Long id, Long companyId);

    List<Conversation> findAllByCompany_IdAndConversationType(Long companyId, ConversationType conversationType);

    @Query("""
            SELECT c FROM Conversation c
            JOIN ConversationMember cm ON cm.conversation = c
            WHERE cm.user.id = :userId
              AND c.company.id = :companyId
            ORDER BY c.id DESC
            """)
    List<Conversation> findAllForUser(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);
}