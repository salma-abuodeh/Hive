package org.example.hive.repository;

import org.example.hive.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByIdAndCompany_Id(Long id, Long companyId);
}