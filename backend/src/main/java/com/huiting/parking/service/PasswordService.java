package com.huiting.parking.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    public String encode(String password) {
        return encoder.encode(password);
    }

    public boolean matches(String password, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2")) return encoder.matches(password, stored);
        return demoEnabled && SeedService.hashPwd(password).equals(stored);
    }
}
