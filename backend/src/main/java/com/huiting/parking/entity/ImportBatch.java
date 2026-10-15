package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "source_sha256", nullable = false, unique = true, length = 64)
    public String sourceSha256;
    @Column(name = "source_file_name", nullable = false)
    public String sourceFileName;
    @Column(name = "community_id", nullable = false)
    public Integer communityId;
    @Column(name = "rental_count", nullable = false)
    public Integer rentalCount;
    @Column(name = "overdue_count", nullable = false)
    public Integer overdueCount;
    @Column(name = "missing_id_count", nullable = false)
    public Integer missingIdCount;
    @Column(name = "missing_start_count", nullable = false)
    public Integer missingStartCount;
    @Column(name = "missing_phone_count", nullable = false)
    public Integer missingPhoneCount;
    @Column(name = "skipped_queue_count", nullable = false)
    public Integer skippedQueueCount;
    @Column(name = "imported_at", nullable = false)
    public LocalDateTime importedAt;
}
