package org.example.hive.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.hive.config.AppEnums.RsvpStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_rsvps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"event", "user"})
public class EventRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RsvpStatus status;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.respondedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RsvpStatus.INVITED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.respondedAt = LocalDateTime.now();
    }
}