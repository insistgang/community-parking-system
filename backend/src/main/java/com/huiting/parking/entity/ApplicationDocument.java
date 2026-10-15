package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_documents", indexes = {
        @Index(name = "idx_document_owner", columnList = "owner_id"),
        @Index(name = "idx_document_application", columnList = "application_id")
})
public class ApplicationDocument {
    @Id
    @Column(length = 36)
    public String id;
    @Column(name = "owner_id", nullable = false)
    public Integer ownerId;
    @Column(name = "community_id", nullable = false)
    public Integer communityId;
    @Column(name = "application_id")
    public Integer applicationId;
    @Column(name = "document_type", nullable = false, length = 32)
    public String documentType;
    @Column(name = "original_name", nullable = false)
    public String originalName;
    @Column(name = "content_type", nullable = false, length = 64)
    public String contentType;
    @Column(name = "size_bytes", nullable = false)
    public Long sizeBytes;
    @Column(nullable = false, length = 64)
    public String sha256;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
