package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_review_history", indexes = @Index(name = "idx_review_history_application", columnList = "application_id"))
public class ApplicationReviewHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "application_id", nullable = false)
    public Integer applicationId;
    @Column(name = "version_number", nullable = false)
    public Integer versionNumber;
    @Column(name = "event_type", nullable = false, length = 32)
    public String eventType;
    @Column(name = "actor_id")
    public Integer actorId;
    @Column(name = "actor_name", length = 100)
    public String actorName;
    @Column(name = "summary", columnDefinition = "TEXT")
    public String summary;
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    public String snapshotJson;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
