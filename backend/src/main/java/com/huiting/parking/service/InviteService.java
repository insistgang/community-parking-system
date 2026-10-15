package com.huiting.parking.service;

import com.huiting.parking.common.ApiException;
import com.huiting.parking.entity.RegistrationInvite;
import com.huiting.parking.entity.PropertyRegistrationInvite;
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
import java.util.Map;
import java.util.Objects;

@Service
public class InviteService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Repos.InviteRepo invites;
    private final Repos.PropertyInviteRepo propertyInvites;
    private final Repos.UserRepo users;
    private final AuditLogService auditLog;

    public InviteService(Repos.InviteRepo invites, Repos.PropertyInviteRepo propertyInvites,
                         Repos.UserRepo users, AuditLogService auditLog) {
        this.invites = invites;
        this.propertyInvites = propertyInvites;
        this.users = users;
        this.auditLog = auditLog;
    }

    @Transactional
    public Map<String, Object> issueProperty(UserAccount admin, Map<String, Object> req) {
        if (!"SUPER_ADMIN".equals(admin.role)) throw new ApiException(403, "仅平台管理员可以签发物业邀请码");
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        String idCard = String.valueOf(req.getOrDefault("id_card", "")).trim();
        String communityName = String.valueOf(req.getOrDefault("community_name", "")).trim();
        if (!phone.matches("1\\d{10}") || !idCard.matches("[0-9]{17}[0-9Xx]"))
            throw new ApiException(400, "请输入有效的物业负责人手机号和18位身份证号");
        if (communityName.isBlank() || communityName.length() > 100)
            throw new ApiException(400, "请输入有效的小区名称（不超过100字）");
        if (users.existsByPhoneOrIdCard(phone, idCard))
            throw new ApiException(400, "该手机号或身份证号已注册");
        byte[] random = new byte[24];
        RANDOM.nextBytes(random);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        PropertyRegistrationInvite invite = new PropertyRegistrationInvite();
        invite.codeHash = hash(code);
        invite.phone = phone;
        invite.idCard = idCard;
        invite.communityName = communityName;
        invite.issuedBy = admin.id;
        invite.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
        propertyInvites.save(invite);
        auditLog.recordOperation(admin.id, admin.realName, admin.role, "AUTH", "ISSUE_PROPERTY_INVITE",
                "平台管理员签发绑定小区及负责人身份的一次性物业入驻邀请码", "--",
                Map.of("community_name", communityName,
                        "phone_masked", phone.substring(0, 3) + "****" + phone.substring(7),
                        "id_card_last4", idCard.substring(idCard.length() - 4)));
        return ParkingService.map("invite_code", code, "community_name", communityName,
                "phone", phone, "expires_at", invite.expiresAt.toString(),
                "message", "邀请码仅显示一次，请线下交给已核验身份的物业负责人");
    }

    @Transactional
    public void consumeProperty(String code, String phone, String idCard, String communityName) {
        if (code == null || code.isBlank()) throw new ApiException(403, "请提供平台管理员签发的物业邀请码");
        PropertyRegistrationInvite invite = propertyInvites.lockByCodeHash(hash(code.trim()))
                .orElseThrow(() -> new ApiException(403, "物业邀请码无效或已过期"));
        if (invite.usedAt != null || !invite.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))
                || !invite.phone.equals(phone) || !invite.idCard.equalsIgnoreCase(idCard)
                || !invite.communityName.equals(communityName))
            throw new ApiException(403, "物业邀请码无效或已过期");
        invite.usedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Transactional
    public Map<String, Object> issue(UserAccount manager, Map<String, Object> req) {
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        String idCard = String.valueOf(req.getOrDefault("id_card", "")).trim();
        if (!phone.matches("1\\d{10}") || !idCard.matches("[0-9]{17}[0-9Xx]"))
            throw new ApiException(400, "请输入有效的业主手机号和18位身份证号");
        if (users.existsByPhoneOrIdCard(phone, idCard))
            throw new ApiException(400, "该手机号或身份证号已注册");
        byte[] random = new byte[24];
        RANDOM.nextBytes(random);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        RegistrationInvite invite = new RegistrationInvite();
        invite.codeHash = hash(code);
        invite.communityId = manager.communityId;
        invite.phone = phone;
        invite.idCard = idCard;
        invite.issuedBy = manager.id;
        invite.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
        invites.save(invite);
        auditLog.recordOperation(manager.id, manager.realName, manager.role, "AUTH", "ISSUE_OWNER_INVITE",
                "物业为已线下核验身份的业主签发一次性注册邀请码", "--",
                Map.of("community_id", manager.communityId,
                        "phone_masked", phone.substring(0, 3) + "****" + phone.substring(7),
                        "id_card_last4", idCard.substring(idCard.length() - 4)));
        return ParkingService.map("invite_code", code, "phone", phone,
                "expires_at", invite.expiresAt.toString(), "message", "邀请码仅显示一次，请线下交给已核验身份的业主");
    }

    @Transactional
    public void consume(String code, String phone, String idCard, Integer communityId) {
        if (code == null || code.isBlank()) throw new ApiException(403, "请提供物业签发的业主邀请码");
        RegistrationInvite invite = invites.lockByCodeHash(hash(code.trim()))
                .orElseThrow(() -> new ApiException(403, "业主邀请码无效或已过期"));
        if (invite.usedAt != null || !invite.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))
                || !Objects.equals(invite.communityId, communityId)
                || !invite.phone.equals(phone) || !invite.idCard.equalsIgnoreCase(idCard))
            throw new ApiException(403, "业主邀请码无效或已过期");
        invite.usedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Scheduled(fixedDelay = 86400000)
    @Transactional
    public void removeExpired() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC);
        invites.deleteByExpiresAtBefore(cutoff);
        propertyInvites.deleteByExpiresAtBefore(cutoff);
    }

    private static String hash(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
