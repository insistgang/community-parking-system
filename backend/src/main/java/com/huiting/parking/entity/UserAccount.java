package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(unique = true)
    public String phone;
    @Column(name = "id_card", unique = true)
    public String idCard;
    @Column(name = "password_hash")
    public String passwordHash;
    @Column(name = "real_name")
    public String realName;
    public String role = "OWNER";
    @Column(name = "community_id")
    public Integer communityId;
    @Column(name = "audit_mode")
    public String auditMode;
    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "last_login_at")
    public LocalDateTime lastLoginAt;
    @Column(name = "last_login_ip")
    public String lastLoginIp;
    @Column(name = "login_count")
    public Integer loginCount = 0;
}
