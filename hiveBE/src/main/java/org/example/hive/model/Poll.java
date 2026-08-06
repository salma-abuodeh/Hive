package org.example.hive.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.hive.config.AppEnums.EventVisibility;

import java.time.LocalDateTime;

@Entity
@Table(name = "polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"company", "team", "createdBy"})
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String question;

    private String description;

    @Column(name = "allow_multiple", nullable = false)
    private Boolean allowMultiple;

    @Column(name = "closes_at")
    private LocalDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_type", nullable = false)
    private EventVisibility visibility;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) this.active = true;
        if (this.allowMultiple == null) this.allowMultiple = false;
        if (this.visibility == null) this.visibility = EventVisibility.COMPANY;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}