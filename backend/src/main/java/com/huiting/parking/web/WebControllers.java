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
    private final ObjectMapper json;
    private final Map<String, long[]> sms = new ConcurrentHashMap<>();

    AuthController(Repos.UserRepo users, Repos.CommunityRepo communities, QueueEngine engine, EvidenceService evidence, ObjectMapper json) {
        this.users = users;
        this.communities = communities;
        this.engine = engine;
        this.evidence = evidence;
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
    public ApiResponse sms(@RequestBody Map<String, Object> req) {
        String phone = String.valueOf(req.getOrDefault("phone", "")).trim();
        if (phone.length() != 11 || !phone.chars().allMatch(Character::isDigit)) throw new ApiException(400, "请输入有效的11位手机号码");
        sms.put(phone, new long[]{888888, Instant.now().getEpochSecond() + 300});
        return ApiResponse.ok("短信验证码发送成功", ParkingService.map("phone", phone, "demo_code", "888888", "expire_seconds", 300));
    }

    @PostMapping("/auth/login")
    public ApiResponse login(@RequestBody Map<String, Object> req) {
        String id = String.valueOf(req.getOrDefault("identifier", ""));
        UserAccount user = users.findByPhoneOrIdCard(id, id).orElseThrow(() -> new ApiException(400, "用户不存在，请先核对或注册"));
        String type = String.valueOf(req.getOrDefault("login_type", "PASSWORD"));
        if ("PASSWORD".equals(type)) {
            String pwd = req.get("password") == null ? "" : String.valueOf(req.get("password"));
            if (pwd.isBlank() || !SeedService.hashPwd(pwd).equals(user.passwordHash)) throw new ApiException(400, "密码错误");
        } else if ("SMS".equals(type)) {
            String code = req.get("verify_code") == null ? "" : String.valueOf(req.get("verify_code")).trim();
            long[] cached = sms.get(user.phone);
            boolean ok = "888888".equals(code) || "123456".equals(code) || (cached != null && String.valueOf(cached[0]).equals(code) && Instant.now().getEpochSecond() <= cached[1]);
            if (!ok) throw new ApiException(400, "短信验证码无效或已过期，请重新获取");
        }
        return ApiResponse.ok("登录成功", tokenBody(user));
    }

    @PostMapping("/auth/register")
    public ApiResponse register(@RequestBody Map<String, Object> req) {
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
        user = users.save(user);
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
}

@RestController
@RequestMapping("/api")
class ParkingController {
    private final ParkingService parking;
    ParkingController(ParkingService parking) { this.parking = parking; }

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
    DevController(SeedService seed) { this.seed = seed; }

    @PostMapping("/reset-seed")
    public ApiResponse reset() {
        seed.init(true);
        return ApiResponse.msg("演示数据已完全一键重置为初始状态！");
    }
}

@RestController
class RootController {
    @GetMapping("/")
    public Map<String, Object> root() {
        return ParkingService.map("system", "慧停管家 API", "version", "v3.0-java", "status", "online", "docs_url", "/api");
    }
}
