package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_applications")
public class QueueApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "user_id")
    public Integer userId;
    @Column(name = "community_id")
    public Integer communityId;
    @Column(name = "property_no")
    public String propertyNo;
    @Column(name = "plate_no")
    public String plateNo;
    @Column(name = "is_spouse_car")
    public Boolean spouseCar = false;
    @Column(name = "marriage_cert_no")
    public String marriageCertNo;
    @Column(name = "car_relationship")
    public String carRelationship = "SELF";
    @Column(name = "family_cert_no")
    public String familyCertNo;
    @Column(name = "priority_level")
    public Integer priorityLevel = 1;
    @Column(name = "current_rank")
    public Integer currentRank = 0;
    public String status = "WAITING";
    @Column(name = "allocated_space_id")
    public Integer allocatedSpaceId;
    @Column(name = "allocated_time")
    public LocalDateTime allocatedTime;
    @Column(name = "apply_time")
    public LocalDateTime applyTime = LocalDateTime.now();
    @Column(name = "audit_mode")
    public String auditMode = "THREE_CERT_AUTO";
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    public String rejectReason;
    @Column(name = "attachments_json", columnDefinition = "TEXT")
    public String attachmentsJson;
    @Column(name = "audit_time")
    public LocalDateTime auditTime;
    @Column(name = "auditor_id")
    public Integer auditorId;
    @Column(name = "audit_reference")
    public String auditReference;
}
