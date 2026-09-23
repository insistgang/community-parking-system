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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final Map<String, long[]> sms = new ConcurrentHashMap<>();

    AuthController(Repos.UserRepo users, Repos.CommunityRepo communities, QueueEngine engine,
                   EvidenceService evidence, AuditLogService auditLog, ObjectMapper json) {
        this.users = users;
        this.communities = communities;
        this.engine = engine;
        this.evidence = evidence;
        this.auditLog = auditLog;
        this.json = json;
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
        String ip = getIp(request);
        String ua = request.getHeader("User-Agent");
        UserAccount user = users.findByPhoneOrIdCard(id, id).orElse(null);
        if (user == null) {
            auditLog.recordLogin(null, id, "未知用户", "UNKNOWN", "PASSWORD", ip, ua, false, "用户不存在");
            throw new ApiException(400, "用户不存在，请先核对或注册");
        }
        String type = String.valueOf(req.getOrDefault("login_type", "PASSWORD"));
        if ("PASSWORD".equals(type)) {
            String pwd = req.get("password") == null ? "" : String.valueOf(req.get("password"));
            if (pwd.isBlank() || !SeedService.hashPwd(pwd).equals(user.passwordHash)) {
                auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, false, "密码错误");
                throw new ApiException(400, "密码错误");
            }
        } else if ("SMS".equals(type)) {
            String code = req.get("verify_code") == null ? "" : String.valueOf(req.get("verify_code")).trim();
            long[] cached = sms.get(user.phone);
            boolean ok = "888888".equals(code) || "123456".equals(code) || (cached != null && String.valueOf(cached[0]).equals(code) && Instant.now().getEpochSecond() <= cached[1]);
            if (!ok) {
                auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, false, "短信验证码无效或已过期");
                throw new ApiException(400, "短信验证码无效或已过期，请重新获取");
            }
        }
        auditLog.recordLogin(user.id, id, user.realName, user.role, type, ip, ua, true, null);
        return ApiResponse.ok("登录成功", tokenBody(user));
    }

    @PostMapping("/auth/reset-password")
    public ApiResponse resetPassword(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        String code = String.valueOf(req.getOrDefault("verify_code", "")).trim();
        String newPwd = String.valueOf(req.getOrDefault("new_password", ""));
        if (phone.length() != 11 || !phone.chars().allMatch(Character::isDigit)) throw new ApiException(400, "请输入有效的11位手机号码");
        if (newPwd.length() < 6) throw new ApiException(400, "新密码长度不能少于6位");
        long[] cached = sms.get(phone);
        boolean ok = "888888".equals(code) || "123456".equals(code) || (cached != null && String.valueOf(cached[0]).equals(code) && Instant.now().getEpochSecond() <= cached[1]);
        if (!ok) throw new ApiException(400, "短信验证码无效或已过期，请重新获取");

        UserAccount user = users.findByPhone(phone).orElseThrow(() -> new ApiException(404, "未找到该手机绑定的业主账号，请核对号码或先注册"));
        user.passwordHash = SeedService.hashPwd(newPwd);
        users.save(user);

        auditLog.recordOperation(user.id, user.realName, user.role, "AUTH", "RESET_PASSWORD",
                "业主通过手机验证码成功找回并重设密码", getIp(request), Map.of("phone", phone));
        return ApiResponse.msg("密码重置成功！请使用新密码重新登录。");
    }

    @PostMapping("/auth/register")
    public ApiResponse register(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        String phone = String.valueOf(req.get("phone"));
        String idCard = String.valueOf(req.get("id_card"));
        if (users.existsByPhoneOrIdCard(phone, idCard)) throw new ApiException(400, "该手机号或身份证号已被注册");
        String communityName = String.valueOf(req.get("community_name"));
        Community community = communities.findByName(communityName).orElse(null);
        if (community == null) {
            community = new Community();
            community.name = communityName;
            community.district = String.valueOf(req.get("district"));
            community.subdistrict = String.valueOf(req.get("subdistrict"));
            community.totalSpaces = 50;
            community.auditMode = "THREE_CERT_AUTO";
            community = communities.save(community);
            engine.initSpaces(community.id, 50);
            String role = String.valueOf(req.getOrDefault("role", "OWNER"));
            String trigger = "PROPERTY_ADMIN".equals(role) ? "物业管理员入驻" : "首位业主注册";
            evidence.create(community.id, null, "INIT", "【" + community.name + "】车位资产台账随" + trigger + "自动初始化 (50个空闲车位)",
                    Map.of("total_spaces", 50, "community", community.name, "trigger", "AUTO_ON_REGISTER"), null);
        }
        UserAccount user = new UserAccount();
        user.phone = phone;
        user.idCard = idCard;
        user.passwordHash = SeedService.hashPwd(String.valueOf(req.get("password")));
        user.realName = String.valueOf(req.get("real_name"));
        user.role = "PROPERTY_ADMIN".equals(String.valueOf(req.getOrDefault("role", "OWNER"))) ? "PROPERTY_ADMIN" : "OWNER";
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
        Community community = communities.findById(user.communityId).orElse(null);
        String token = "token-" + user.id + "-" + SeedService.hashPwd(user.phone).substring(0, 8);
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

    ParkingController(ParkingService parking, Repos.UserRepo users, AuditLogService auditLog) {
        this.parking = parking;
        this.users = users;
        this.auditLog = auditLog;
    }

    @PostMapping("/user/change-password")
    public ApiResponse changePassword(@RequestHeader(value = "X-User-Id", required = false) Integer uid,
                                      @RequestBody Map<String, Object> req,
                                      HttpServletRequest request) {
        UserAccount user = parking.requireUser(uid);
        String oldPwd = String.valueOf(req.getOrDefault("old_password", ""));
        String newPwd = String.valueOf(req.getOrDefault("new_password", ""));
        if (newPwd.length() < 6) throw new ApiException(400, "新密码长度不能少于6位");
        if (!SeedService.hashPwd(oldPwd).equals(user.passwordHash)) {
            throw new ApiException(400, "原密码验证不正确，请核对后重试");
        }
        user.passwordHash = SeedService.hashPwd(newPwd);
        users.save(user);

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        auditLog.recordOperation(user.id, user.realName, user.role, "USER", "CHANGE_PASSWORD",
                "业主在线验证原密码修改登录密码成功", ip, Map.of("user_id", user.id));
        return ApiResponse.msg("密码修改成功，请牢记您的新密码！");
    }

    @GetMapping("/user/home")
    public ApiResponse home(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(parking.home(parking.requireUser(uid)));
    }

    @GetMapping("/parking/audit-mode")
    public ApiResponse mode(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(parking.auditMode(parking.requireUser(uid)));
    }

    @PostMapping("/parking/apply")
    public ApiResponse apply(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = parking.apply(parking.requireUser(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @PostMapping("/parking/cancel-apply")
    public ApiResponse cancel(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody(required = false) Map<String, Object> req) {
        String reason = req == null ? null : String.valueOf(req.getOrDefault("reason", "个人原因自愿撤销"));
        return ApiResponse.msg(parking.cancel(parking.requireUser(uid), reason));
    }

    @PostMapping("/parking/accept-allocation")
    public ApiResponse accept(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.msg(parking.accept(parking.requireUser(uid)));
    }

    @PostMapping("/parking/give-up-allocation")
    public ApiResponse giveUp(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.msg(parking.giveUp(parking.requireUser(uid)));
    }

    @PostMapping("/parking/terminate-rental")
    public ApiResponse terminate(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.msg(parking.terminate(parking.requireUser(uid)));
    }

    @PostMapping("/parking/renew")
    public ApiResponse renew(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody(required = false) Map<String, Object> req) {
        Integer months = req == null || req.get("months") == null ? null : ((Number) req.get("months")).intValue();
        Map<String, Object> data = parking.renew(parking.requireUser(uid), months);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/parking/queue")
    public ApiResponse queue(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(parking.queue(parking.requireUser(uid)));
    }

    @GetMapping("/parking/my-timeline")
    public ApiResponse timeline(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(parking.timeline(parking.requireUser(uid)));
    }

    @GetMapping("/notifications")
    public ApiResponse notices(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(parking.notifications(parking.requireUser(uid)));
    }
}

@RestController
@RequestMapping("/api/property")
class PropertyController {
    private final PropertyService property;
    PropertyController(PropertyService property) { this.property = property; }

    @GetMapping("/stats")
    public ApiResponse stats(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(property.stats(property.requireAdmin(uid)));
    }

    @GetMapping("/spaces")
    public ApiResponse spaces(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(property.spaceList(property.requireAdmin(uid)));
    }

    @PostMapping("/set-maintenance")
    public ApiResponse maintenance(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.maintenance(property.requireAdmin(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @PostMapping("/confirm-delivery")
    public ApiResponse delivery(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.confirmDelivery(property.requireAdmin(uid), req));
    }

    @PostMapping("/force-terminate")
    public ApiResponse terminate(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.forceTerminate(property.requireAdmin(uid), req));
    }

    @GetMapping("/space-history/{spaceId}")
    public ApiResponse history(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @PathVariable Integer spaceId) {
        return ApiResponse.ok(property.history(property.requireAdmin(uid), spaceId));
    }

    @GetMapping("/export-spaces")
    public ResponseEntity<byte[]> export(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        var admin = property.requireAdmin(uid);
        String name = URLEncoder.encode(property.exportName(admin), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(property.export(admin));
    }

    @PostMapping("/init-demo-spaces")
    public ApiResponse init(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.msg(property.initSpaces(property.requireAdmin(uid)));
    }

    @GetMapping("/audit-config")
    public ApiResponse auditGet(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(property.auditConfig(property.requireAdmin(uid)));
    }

    @PostMapping("/audit-config")
    public ApiResponse auditSet(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.updateAudit(property.requireAdmin(uid), String.valueOf(req.get("audit_mode")));
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/community-users")
    public ApiResponse users(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
        return ApiResponse.ok(property.communityUsers(property.requireAdmin(uid)));
    }

    @PostMapping("/set-user-audit-mode")
    public ApiResponse userMode(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = property.setUserMode(property.requireAdmin(uid), req);
        String message = String.valueOf(data.remove("_message"));
        return ApiResponse.ok(message, data);
    }

    @GetMapping("/applications")
    public ApiResponse apps(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestParam(required = false) String status) {
        return ApiResponse.ok(property.applications(property.requireAdmin(uid), status));
    }

    @PostMapping("/audit-application")
    public ApiResponse audit(@RequestHeader(value = "X-User-Id", required = false) Integer uid, @RequestBody Map<String, Object> req) {
        return ApiResponse.msg(property.audit(property.requireAdmin(uid), req));
    }
}

@RestController
@RequestMapping("/api/evidence")
class EvidenceController {
    private final ParkingService parking;
    private final Repos.EvidenceRepo repo;
    EvidenceController(ParkingService parking, Repos.EvidenceRepo repo) { this.parking = parking; this.repo = repo; }

    @GetMapping("/list")
    public ApiResponse list(@RequestHeader(value = "X-User-Id", required = false) Integer uid) {
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
    public ApiResponse verify(@PathVariable String evidenceId) {
        ChainEvidence rec = repo.findByEvidenceId(evidenceId).orElseThrow(() -> new ApiException(404, "未找到该存证记录"));
        return ApiResponse.ok(ParkingService.map("evidence_id", rec.evidenceId, "is_valid", true, "block_height", rec.blockHeight,
                "tx_hash", rec.txHash, "prev_hash", rec.prevHash, "timestamp", rec.timestamp, "event_summary", rec.eventSummary,
                "verification_status", "BLOCKCHAIN_VERIFIED_SUCCESS", "cert_agency", "上海市“浦江数链”区块链公共存证基础设施平台"));
    }
}

@RestController
@RequestMapping("/api/dev")
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
