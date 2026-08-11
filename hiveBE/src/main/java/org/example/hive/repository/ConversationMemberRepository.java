package org.example.hive.repository;

import org.example.hive.model.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    Optional<ConversationMember> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    List<ConversationMember> findAllByConversation_Id(Long conversationId);

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    long countByConversation_Id(Long conversationId);

    void deleteByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Query("""
            SELECT cm.conversation.id FROM ConversationMember cm
            WHERE cm.user.id = :userId
            """)
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);
}