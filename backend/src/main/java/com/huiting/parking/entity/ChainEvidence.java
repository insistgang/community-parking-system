package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain_evidence")
public class ChainEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "evidence_id", unique = true)
    public String evidenceId;
    @Column(name = "community_id")
    public Integer communityId;
    @Column(name = "user_id")
    public Integer userId;
    @Column(name = "event_type")
    public String eventType;
    @Column(name = "block_height")
    public Integer blockHeight;
    @Column(name = "tx_hash")
    public String txHash;
    @Column(name = "prev_hash")
    public String prevHash;
    @Column(name = "event_summary")
    public String eventSummary;
    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    public String rawPayloadJson;
    public String timestamp;
    @Column(name = "created_at")
    public LocalDateTime createdAt;
}
