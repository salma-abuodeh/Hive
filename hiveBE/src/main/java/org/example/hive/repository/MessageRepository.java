package org.example.hive.repository;

import org.example.hive.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByConversation_IdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Optional<Message> findByIdAndConversation_Id(Long id, Long conversationId);

    Optional<Message> findTopByConversation_IdOrderByCreatedAtDesc(Long conversationId);

    long countByConversation_Id(Long conversationId);

    long countByConversation_IdAndCreatedAtAfter(Long conversationId, LocalDateTime after);
}