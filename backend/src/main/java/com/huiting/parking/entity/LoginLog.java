package com.huiting.parking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs")
public class LoginLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "user_id")
    public Integer userId;

    public String account;

    @Column(name = "real_name")
    public String realName;

    public String role;

    @Column(name = "login_type")
    public String loginType = "PASSWORD";

    public String ip;

    @Column(name = "user_agent")
    public String userAgent;

    public String status = "SUCCESS";

    @Column(name = "fail_reason")
    public String failReason;

    @Column(name = "login_at")
    public LocalDateTime loginAt = LocalDateTime.now();
}
