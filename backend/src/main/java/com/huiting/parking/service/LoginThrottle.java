package com.huiting.parking.service;

import com.huiting.parking.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginThrottle {
    private static final int MAX_FAILURES = 5;
    private static final long BLOCK_SECONDS = 15 * 60;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void check(String account, HttpServletRequest request) {
        Attempt attempt = attempts.get(key(account, request));
        if (attempt != null && attempt.blockedUntil > Instant.now().getEpochSecond()) {
            throw new ApiException(429, "登录尝试过多，请15分钟后重试");
        }
    }

    public void failure(String account, HttpServletRequest request) {
        Attempt attempt = attempts.computeIfAbsent(key(account, request), ignored -> new Attempt());
        synchronized (attempt) {
            long now = Instant.now().getEpochSecond();
            if (attempt.blockedUntil != 0 && attempt.blockedUntil <= now) {
                attempt.failures = 0;
                attempt.blockedUntil = 0;
            }
            if (now - attempt.lastFailure > BLOCK_SECONDS) attempt.failures = 0;
            attempt.failures++;
            attempt.lastFailure = now;
            if (attempt.failures >= MAX_FAILURES) attempt.blockedUntil = now + BLOCK_SECONDS;
        }
    }

    public void success(String account, HttpServletRequest request) {
        attempts.remove(key(account, request));
    }

    @Scheduled(fixedDelay = 900000)
    public void removeOldAttempts() {
        long now = Instant.now().getEpochSecond();
        attempts.entrySet().removeIf(entry -> now - entry.getValue().lastFailure > 1800
                && entry.getValue().blockedUntil <= now);
    }

    private static String key(String account, HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip + ":" + account.trim().toLowerCase();
    }

    private static final class Attempt {
        int failures;
        volatile long blockedUntil;
        volatile long lastFailure;
    }
}
