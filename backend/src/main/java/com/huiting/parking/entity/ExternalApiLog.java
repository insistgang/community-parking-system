package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_api_logs")
public class ExternalApiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "api_name")
    public String apiName;

    public String provider;

    public String method = "POST";

    public String url;

    @Column(name = "request_json", columnDefinition = "TEXT")
    public String requestJson;

    @Column(name = "response_json", columnDefinition = "TEXT")
    public String responseJson;

    @Column(name = "duration_ms")
    public Long durationMs = 0L;

    @Column(name = "status_code")
    public Integer statusCode = 200;

    public String status = "SUCCESS";

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
