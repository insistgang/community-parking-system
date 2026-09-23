package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "operator_id")
    public Integer operatorId;

    @Column(name = "operator_name")
    public String operatorName;

    @Column(name = "operator_role")
    public String operatorRole;

    public String module;

    public String action;

    @Column(columnDefinition = "TEXT")
    public String summary;

    public String ip;

    @Column(name = "details_json", columnDefinition = "TEXT")
    public String detailsJson;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
