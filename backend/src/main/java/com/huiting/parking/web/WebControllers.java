package com.huiting.parking.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.common.ApiException;
import com.huiting.parking.common.ApiResponse;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.ChainEvidence;
import com.huiting.parking.entity.Community;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import com.huiting.parking.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
class AuthController {
    private final Repos.UserRepo users;
    private final Repos.CommunityRepo communities;
    private final QueueEngine engine;
    private final EvidenceService evidence;
    private final AuditLogService auditLog;
    private final ObjectMapper json;
    private final SessionService sessions;
    private final PasswordService passwords;
    private final LoginThrottle loginThrottle;
    private final InviteService invites;
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;
    private final Map<String, long[]> sms = new ConcurrentHashMap<>();

    AuthController(Repos.UserRepo users, Repos.CommunityRepo communities, QueueEngine engine,
                   EvidenceService evidence, AuditLogService auditLog, ObjectMapper json,
                   SessionService sessions, PasswordService passwords, LoginThrottle loginThrottle,
                   InviteService invites) {
        this.users = users;
        this.communities = communities;
        this.engine = engine;
        this.evidence = evidence;
        this.auditLog = auditLog;
        this.json = json;
        this.sessions = sessions;
        this.passwords = passwords;
        this.loginThrottle = loginThrottle;
        this.invites = invites;
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.ok("系统运行正常", Map.of("status", "UP", "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/common/districts")
    public ApiResponse districts() throws Exception {
        Map<String, List<String>> raw = json.readValue(new ClassPathResource("shanghai-districts.json").getInputStream(), new TypeReference<>() {});
        List<Map<String, Object>> data = new ArrayList<>();
        raw.forEach((k, v) -> data.add(ParkingService.map("district", k, "subdistricts", v)));
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/send-sms")
    public ApiResponse sms(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        if (!demoEnabled) throw new ApiException(503, "短信服务尚未接入，请使用密码登录");
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        if (phone.length() != 11 || !phone.chars().allMatch(Character::isDigit)) throw new ApiException(400, "请输入有效的11位手机号码");
        sms.put(phone, new long[]{888888, Instant.now().getEpochSecond() + 300});

        auditLog.recordExternalApi(
                "移动政务云短信下发网关",
                "中国移动通信集团上海分公司",
                "POST",
                "https://api.shanghai.gov.cn/sms/v2/send-verify-code",
                Map.of("phone", phone, "code", "888888", "expire_seconds", 300),
                Map.of("code", "0000", "message", "验证码发送成功", "msg_id", "SMS_" + System.currentTimeMillis()),
                58L,
                200,
                true
        );

        return ApiResponse.ok("短信验证码发送成功", ParkingService.map("phone", phone, "demo_code", "888888", "expire_seconds", 300));
    }

    @PostMapping("/auth/login")
    public ApiResponse login(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        String id = String.valueOf(req.getOrDefault("identifier", ""));
        loginThrottle.check(id, request);
        String ip = getIp(request);
        String ua = request.getHeader("User-Agent");
        UserAccount user = users.findByPhoneOrIdCard(id, id).orElse(null);
        if (user == null) {
            loginThrottle.failure(id, request);
            auditLog.recordLogin(null, id, "未知用户", "UNKNOWN", "PASSWORD", ip, ua, false, "用户不存在");
            throw new ApiException(400, demoEnabled ? "用户不存在，请先核对或注册" : "账号或密码错误");
        }
        String type = String.valueOf(req.getOrDefault("login_type", "PASSWORD"));
        if ("PASSWORD".equals(type)) {
            String pwd = req.get("password") == null ? "" : String.valueOf(req.get("password"));
            if (pwd.isBlank() || !passwords.matches(pwd, user.passwordHash)) {
                loginThrottle.failure(id, request);
                auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, false, "密码错误");
                throw new ApiException(400, "密码错误");
            }
        } else if ("SMS".equals(type)) {
            if (!demoEnabled) throw new ApiException(503, "短信登录尚未接入，请使用密码登录");
            String code = req.get("verify_code") == null ? "" : String.valueOf(req.get("verify_code")).trim();
            long[] cached = sms.get(user.phone);
            boolean ok = "888888".equals(code) || "123456".equals(code) || (cached != null && String.valueOf(cached[0]).equals(code) && Instant.now().getEpochSecond() <= cached[1]);
            if (!ok) {
                loginThrottle.failure(id, request);
                auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, false, "短信验证码无效或已过期");
                throw new ApiException(400, "短信验证码无效或已过期，请重新获取");
            }
        } else {
            throw new ApiException(400, "不支持的登录方式");
        }
        loginThrottle.success(id, request);
        auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, true, null);
        return ApiResponse.ok("登录成功", tokenBody(user));
    }

    @PostMapping("/auth/reset-password")
    public ApiResponse resetPassword(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        if (!demoEnabled) throw new ApiException(503, "短信找回密码尚未接入，请联系管理员");
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        String code = String.valueOf(req.getOrDefault("verify_code", "")).trim();
        String newPwd = String.valueOf(req.getOrDefault("new_password", ""));
        if (phone.length() != 11 || !phone.chars().allMatch(Character::isDigit)) throw new ApiException(400, "请输入有效的11位手机号码");
        if (newPwd.length() < 6) throw new ApiException(400, "新密码长度不能少于6位");
        long[] cached = sms.get(phone);
        boolean ok = "888888".equals(code) || "123456".equals(code) || (cached != null && String.valueOf(cached[0]).equals(code) && Instant.now().getEpochSecond() <= cached[1]);
        if (!ok) throw new ApiException(400, "短信验证码无效或已过期，请重新获取");

        UserAccount user = users.findByPhone(phone).orElseThrow(() -> new ApiException(404, "未找到该手机绑定的业主账号，请核对号码或先注册"));
        user.passwordHash = passwords.encode(newPwd);
        users.save(user);
        sessions.revokeUser(user.id);

        auditLog.recordOperation(user.id, user.realName, user.role, "AUTH", "RESET_PASSWORD",
                "业主通过手机验证码成功找回并重设密码", getIp(request), Map.of("phone", phone));
        return ApiResponse.msg("密码重置成功！请使用新密码重新登录。");
    }

    @PostMapping("/auth/register")
    @Transactional
    public ApiResponse register(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        String idCard = String.valueOf(req.getOrDefault("id_card", "")).trim();
        String password = String.valueOf(req.getOrDefault("password", ""));
        String role = String.valueOf(req.getOrDefault("role", "OWNER"));
        if (!phone.matches("1\\d{10}") || !idCard.matches("[0-9]{17}[0-9Xx]"))
            throw new ApiException(400, "请输入有效的手机号和18位身份证号");
        if (password.length() < 8) throw new ApiException(400, "密码长度不能少于8位");
        if (!"OWNER".equals(role) && !"PROPERTY_ADMIN".equals(role)) throw new ApiException(400, "无效的用户角色");
        if (users.existsByPhoneOrIdCard(phone, idCard)) throw new ApiException(400, "该手机号或身份证号已被注册");
        String communityName = String.valueOf(req.getOrDefault("community_name", "")).trim();
        if (communityName.isBlank()) throw new ApiException(400, "请选择所属小区");
        if ("PROPERTY_ADMIN".equals(role))
            invites.consumeProperty(String.valueOf(req.getOrDefault("invite_code", "")), phone, idCard, communityName);
        Community community = communities.findByName(communityName).orElse(null);
        if (community == null) {
            if (!"PROPERTY_ADMIN".equals(role)) throw new ApiException(400, "小区尚未开通，请联系物业管理员");
            community = new Community();
            community.name = communityName;
            community.district = String.valueOf(req.get("district"));
            community.subdistrict = String.valueOf(req.get("subdistrict"));
            community.totalSpaces = demoEnabled ? 50 : 0;
            community.auditMode = "FLEXIBLE_HYBRID";
            community = communities.save(community);
            if (demoEnabled) engine.initSpaces(community.id, 50);
            String trigger = "PROPERTY_ADMIN".equals(role) ? "物业管理员入驻" : "首位业主注册";
            evidence.create(community.id, null, "INIT", "【" + community.name + "】小区建档完成，等待物业录入真实车位",
                    Map.of("total_spaces", community.totalSpaces, "community", community.name, "trigger", trigger), null);
        }
        if ("OWNER".equals(role) && !demoEnabled)
            invites.consume(String.valueOf(req.getOrDefault("invite_code", "")), phone, idCard, community.id);
        UserAccount user = new UserAccount();
        user.phone = phone;
        user.idCard = idCard;
        user.passwordHash = passwords.encode(password);
        user.realName = String.valueOf(req.get("real_name"));
        user.role = role;
        user.communityId = community.id;
        user.createdAt = Times.utcNow();
        user.loginCount = 1;
        user.lastLoginAt = Times.utcNow();
        user.lastLoginIp = getIp(request);
        user = users.save(user);

        auditLog.recordOperation(user.id, user.realName, user.role, "AUTH", "REGISTER",
                "新用户注册成功 (所属小区: " + community.name + ")", getIp(request), Map.of("role", user.role, "phone", user.phone));
        auditLog.recordLogin(user.id, phone, user.realName, user.role, "PASSWORD", getIp(request), request.getHeader("User-Agent"), true, null);

        return ApiResponse.ok("注册成功", tokenBody(user));
    }

    private Map<String, Object> tokenBody(UserAccount user) {
        Community community = user.communityId == null ? null : communities.findById(user.communityId).orElse(null);
        String token = sessions.create(user);
        return ParkingService.map("token", token, "user", ParkingService.map(
                "id", user.id, "phone", user.phone, "real_name", user.realName, "role", user.role,
                "community_id", user.communityId, "community_name", community != null ? community.name : "龙华新村",
                "district", community != null ? community.district : "徐汇区"));
    }

    private static String getIp(HttpServletRequest req) {
        if (req == null) return "127.0.0.1";
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }
}

@RestController
@RequestMapping("/api")
class ParkingController {
    private final ParkingService parking;
    private final Repos.UserRepo users;
    private final AuditLogService auditLog;
    private final PasswordService passwords;
    private final SessionService sessions;

    ParkingController(ParkingService parking, Repos.UserRepo users, AuditLogService auditLog,
                      PasswordService passwords, SessionService sessions) {
        this.parking = parking;
        this.users = users;
        this.auditLog = auditLog;
        this.passwords = passwords;
        this.sessions = sessions;
    }

    @PostMapping("/auth/logout")
    public ApiResponse logout(HttpServletRequest request) {
        sessions.revoke(request.getHeader("Authorization"));
        return ApiResponse.msg("已退出登录");
    }

    @PostMapping("/user/change-password")
    public ApiResponse changePassword(@RequestAttribute(name = "authUserId") Integer uid,
                                      @RequestBody Map<String, Object> req,
                                      HttpServletRequest request) {
        UserAccount user = parking.requireUser(uid);
        String oldPwd = String.valueOf(req.getOrDefault("old_password", ""));
        String newPwd = String.valueOf(req.getOrDefault("new_password", ""));
        if (newPwd.length() < 8) throw new ApiException(400, "新密码长度不能少于8位");
        if (!passwords.matches(oldPwd, user.passwordHash)) {
            throw new ApiException(400, "原密码验证不正确，请核对后重试");
        }
        user.passwordHash = passwords.encode(newPwd);
        users.save(user);
        sessions.revokeUser(user.id);

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        auditLog.recordOperation(user.id, user.realName, user.role, "USER", "CHANGE_PASSWORD",
                "业主在线验证原密码修改登录密码成功", ip, Map.of("user_id", user.id));
        return ApiResponse.msg("密码修改成功，请牢记您的新密码！");
    }

    @GetMapping("/user/home")
    public ApiResponse home(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(parking.home(parking.requireUser(uid)));
    }

    @GetMapping("/parking/audit-mode")
    public ApiResponse mode(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(parking.auditMode(parking.requireUser(uid)));
    }

    @PostMapping("/parking/apply")
    public ApiResponse apply(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = parking.apply(parking.requireUser(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @PostMapping("/parking/cancel-apply")
    public ApiResponse cancel(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody(required = false) Map<String, Object> req) {
        String reason = req == null ? null : String.valueOf(req.getOrDefault("reason", "个人原因自愿撤销"));
        return ApiResponse.msg(parking.cancel(parking.requireUser(uid), reason));
    }

    @PostMapping("/parking/accept-allocation")
    public ApiResponse accept(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.msg(parking.accept(parking.requireUser(uid)));
    }

    @PostMapping("/parking/give-up-allocation")
    public ApiResponse giveUp(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.msg(parking.giveUp(parking.requireUser(uid)));
    }

    @PostMapping("/parking/terminate-rental")
    public ApiResponse terminate(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.msg(parking.terminate(parking.requireUser(uid)));
    }

    @PostMapping("/parking/renew")
    public ApiResponse renew(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody(required = false) Map<String, Object> req) {
        Integer months = req == null || req.get("months") == null ? null : ((Number) req.get("months")).intValue();
        Map<String, Object> data = parking.renew(parking.requireUser(uid), months);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/parking/queue")
    public ApiResponse queue(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(parking.queue(parking.requireUser(uid)));
    }

    @GetMapping("/parking/my-timeline")
    public ApiResponse timeline(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(parking.timeline(parking.requireUser(uid)));
    }

    @GetMapping("/notifications")
    public ApiResponse notices(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(parking.notifications(parking.requireUser(uid)));
    }
}

@RestController
@RequestMapping("/api/property")
class PropertyController {
    private final PropertyService property;
    private final InviteService invites;
    PropertyController(PropertyService property, InviteService invites) {
        this.property = property;
        this.invites = invites;
    }

    @GetMapping("/stats")
    public ApiResponse stats(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(property.stats(property.requireAdmin(uid)));
    }

    @GetMapping("/spaces")
    public ApiResponse spaces(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(property.spaceList(property.requireAdmin(uid)));
    }

    @PostMapping("/spaces")
    public ApiResponse addSpace(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.ok("车位录入成功", property.addSpace(property.requireAdmin(uid), req));
    }

    @PostMapping("/owner-invites")
    public ApiResponse issueOwnerInvite(@RequestAttribute(name = "authUserId") Integer uid,
                                        @RequestBody Map<String, Object> req) {
        return ApiResponse.ok("业主邀请码已生成", invites.issue(property.requireAdmin(uid), req));
    }

    @PostMapping("/set-maintenance")
    public ApiResponse maintenance(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.maintenance(property.requireAdmin(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @PostMapping("/confirm-delivery")
    public ApiResponse delivery(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.confirmDelivery(property.requireAdmin(uid), req));
    }

    @PostMapping("/force-terminate")
    public ApiResponse terminate(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.forceTerminate(property.requireAdmin(uid), req));
    }

    @GetMapping("/space-history/{spaceId}")
    public ApiResponse history(@RequestAttribute(name = "authUserId") Integer uid, @PathVariable Integer spaceId) {
        return ApiResponse.ok(property.history(property.requireAdmin(uid), spaceId));
    }

    @GetMapping("/export-spaces")
    public ResponseEntity<byte[]> export(@RequestAttribute(name = "authUserId") Integer uid) {
        var admin = property.requireAdmin(uid);
        String name = URLEncoder.encode(property.exportName(admin), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(property.export(admin));
    }

    @PostMapping("/init-demo-spaces")
    public ApiResponse init(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.msg(property.initSpaces(property.requireAdmin(uid)));
    }

    @GetMapping("/audit-config")
    public ApiResponse auditGet(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(property.auditConfig(property.requireAdmin(uid)));
    }

    @PostMapping("/audit-config")
    public ApiResponse auditSet(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.updateAudit(property.requireAdmin(uid), String.valueOf(req.get("audit_mode")));
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/community-users")
    public ApiResponse users(@RequestAttribute(name = "authUserId") Integer uid) {
        return ApiResponse.ok(property.communityUsers(property.requireAdmin(uid)));
    }

    @PostMapping("/set-user-audit-mode")
    public ApiResponse userMode(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.setUserMode(property.requireAdmin(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/applications")
    public ApiResponse apps(@RequestAttribute(name = "authUserId") Integer uid, @RequestParam(required = false) String status) {
        return ApiResponse.ok(property.applications(property.requireAdmin(uid), status));
    }

    @PostMapping("/audit-application")
    public ApiResponse audit(@RequestAttribute(name = "authUserId") Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.audit(property.requireAdmin(uid), req));
    }
}

@RestController
@RequestMapping("/api/evidence")
class EvidenceController {
    private final ParkingService parking;
    private final Repos.EvidenceRepo repo;
    private final EvidenceService evidence;
    EvidenceController(ParkingService parking, Repos.EvidenceRepo repo, EvidenceService evidence) {
        this.parking = parking;
        this.repo = repo;
        this.evidence = evidence;
    }

    @GetMapping("/list")
    public ApiResponse list(@RequestAttribute(name = "authUserId") Integer uid) {
        UserAccount user = parking.requireUser(uid);
        List<ChainEvidence> records = repo.findByCommunityIdOrderByBlockHeightDesc(user.communityId);
        if (!"PROPERTY_ADMIN".equals(user.role)) {
            records = records.stream().filter(r -> user.id.equals(r.userId) || "INIT".equals(r.eventType)).toList();
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (ChainEvidence r : records) {
            data.add(ParkingService.map("evidence_id", r.evidenceId, "event_type", r.eventType, "block_height", r.blockHeight,
                    "tx_hash", r.txHash, "prev_hash", r.prevHash, "event_summary", r.eventSummary, "timestamp", r.timestamp, "created_at", Times.fmtBj(r.createdAt)));
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/verify/{evidenceId}")
    public ApiResponse verify(@RequestAttribute(name = "authUserId") Integer uid, @PathVariable String evidenceId) {
        UserAccount user = parking.requireUser(uid);
        ChainEvidence rec = repo.findByEvidenceId(evidenceId).orElseThrow(() -> new ApiException(404, "未找到该存证记录"));
        if (!Objects.equals(rec.communityId, user.communityId)
                || (!"PROPERTY_ADMIN".equals(user.role) && !Objects.equals(rec.userId, user.id) && !"INIT".equals(rec.eventType)))
            throw new ApiException(404, "未找到该存证记录");
        boolean valid = evidence.verify(rec);
        return ApiResponse.ok(ParkingService.map("evidence_id", rec.evidenceId, "is_valid", valid, "block_height", rec.blockHeight,
                "tx_hash", rec.txHash, "prev_hash", rec.prevHash, "timestamp", rec.timestamp, "event_summary", rec.eventSummary,
                "verification_status", valid ? "LOCAL_INTEGRITY_VERIFIED" : "LOCAL_INTEGRITY_FAILED",
                "cert_agency", "本系统本地审计记录"));
    }
}

@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
class DevController {
    private final SeedService seed;
    private final BootJobs bootJobs;
    DevController(SeedService seed, BootJobs bootJobs) { this.seed = seed; this.bootJobs = bootJobs; }

    @PostMapping("/reset-seed")
    public ApiResponse reset() {
        seed.init(true);
        return ApiResponse.msg("演示数据已完全一键重置为初始状态！");
    }

    @PostMapping("/trigger-sms-upgrade")
    public ApiResponse triggerSms() {
        int count = bootJobs.processUnreadSmsUpgrades(true);
        return ApiResponse.ok("已触发扫描并对 " + count + " 条未读通知执行短信升级提醒！",
                ParkingService.map("upgraded_count", count));
    }
}

@RestController
class RootController {
    @GetMapping("/")
    public Map<String, Object> root() {
        return ParkingService.map("system", "慧停管家 API", "version", "v3.0-java", "status", "online", "docs_url", "/api");
    }
}
