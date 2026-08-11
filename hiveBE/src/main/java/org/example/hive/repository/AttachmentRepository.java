package org.example.hive.repository;

import org.example.hive.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByIdAndCompany_Id(Long id, Long companyId);

    List<Attachment> findAllByPost_IdOrderByIdAsc(Long postId);

    List<Attachment> findAllByComment_IdOrderByIdAsc(Long commentId);

    Optional<Attachment> findByIdAndPost_Id(Long id, Long postId);

    Optional<Attachment> findByIdAndComment_Id(Long id, Long commentId);

    long countByPost_Id(Long postId);

    long countByComment_Id(Long commentId);
}