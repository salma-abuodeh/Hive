package org.example.hive.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.hive.config.AppEnums.CompanyType;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_applications")
@Getter @Setter @NoArgsConstructor
public class CompanyApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false) private User requester;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "company_type", nullable = false) private CompanyType type;
    private String domain;
    @Column(nullable = false) private String status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id") private User reviewer;
    @Column(name = "rejection_reason") private String rejectionReason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); if (status == null) status = "PENDING"; }
}
