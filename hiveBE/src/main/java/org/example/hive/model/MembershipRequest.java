package org.example.hive.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_requests")
@Getter @Setter @NoArgsConstructor
public class MembershipRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false) private User requester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
    @Column(nullable = false) private String status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id") private User reviewer;
    @Column(name = "rejection_reason") private String rejectionReason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); if (status == null) status = "PENDING"; }
}
