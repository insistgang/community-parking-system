package com.huiting.parking.service;

import com.huiting.parking.common.Times;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdmin {
    private final Repos.UserRepo users;
    private final PasswordService passwords;
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;
    @Value("${app.bootstrap.admin-phone:}")
    private String adminPhone;
    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    public BootstrapAdmin(Repos.UserRepo users, PasswordService passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        if (demoEnabled || users.findAll().stream().anyMatch(u -> "SUPER_ADMIN".equals(u.role))) return;
        if (!adminPhone.matches("1\\d{10}") || adminPassword.length() < 12)
            throw new IllegalStateException("首次启动需要配置 BOOTSTRAP_ADMIN_PHONE 和至少12位的 BOOTSTRAP_ADMIN_PASSWORD");
        UserAccount admin = new UserAccount();
        admin.phone = adminPhone;
        admin.realName = "平台管理员";
        admin.role = "SUPER_ADMIN";
        admin.passwordHash = passwords.encode(adminPassword);
        admin.createdAt = Times.utcNow();
        users.save(admin);
    }
}
