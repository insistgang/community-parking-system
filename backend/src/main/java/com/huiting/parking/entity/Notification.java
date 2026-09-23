package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(name = "user_id")
    public Integer userId;
    public String title;
    @Column(columnDefinition = "TEXT")
    public String content;
    public String type;
    @Column(name = "is_read")
    public Boolean readFlag = false;
    @Column(name = "action_type")
    public String actionType;
    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
}
