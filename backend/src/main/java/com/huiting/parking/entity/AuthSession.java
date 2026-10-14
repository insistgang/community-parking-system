package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_sessions", indexes = @Index(name = "idx_auth_sessions_user", columnList = "user_id"))
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    public String tokenHash;

    @Column(name = "user_id", nullable = false)
    public Integer userId;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;
}
