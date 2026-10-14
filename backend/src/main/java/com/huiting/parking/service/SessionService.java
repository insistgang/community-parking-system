package com.huiting.parking.service;

import com.huiting.parking.entity.AuthSession;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class SessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Repos.SessionRepo sessions;
    private final Repos.UserRepo users;

    public SessionService(Repos.SessionRepo sessions, Repos.UserRepo users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Transactional
    public String create(UserAccount user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthSession session = new AuthSession();
        session.tokenHash = hash(token);
        session.userId = user.id;
        session.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12);
        sessions.save(session);
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> resolve(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) return Optional.empty();
        return sessions.findByTokenHash(hash(token))
                .filter(s -> s.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .flatMap(s -> users.findById(s.userId));
    }

    @Transactional
    public void revoke(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.deleteByTokenHash(hash(authorization.substring(7).trim()));
        }
    }

    @Transactional
    public void revokeUser(Integer userId) {
        sessions.deleteByUserId(userId);
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void removeExpired() {
        sessions.deleteByExpiresAtBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
