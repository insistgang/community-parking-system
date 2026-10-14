package com.huiting.parking.web;

import com.huiting.parking.common.ApiException;
import com.huiting.parking.common.ApiResponse;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.Community;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import com.huiting.parking.service.AuditLogService;
import com.huiting.parking.service.BootJobs;
import com.huiting.parking.service.ParkingService;
import com.huiting.parking.service.SeedService;
import com.huiting.parking.service.PasswordService;
import com.huiting.parking.service.SessionService;
import com.huiting.parking.service.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final Repos.UserRepo users;
    private final Repos.CommunityRepo communities;
    private final Repos.SpaceRepo spaces;
    private final Repos.AppRepo apps;
    private final Repos.LoginLogRepo loginLogs;
    private final Repos.OperationLogRepo operationLogs;
    private final Repos.ExternalApiLogRepo externalApiLogs;
    private final Repos.NoticeRepo notices;
    private final AuditLogService auditLog;
    private final BootJobs bootJobs;
    private final PasswordService passwords;
    private final SessionService sessions;
    private final LoginThrottle loginThrottle;
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    public AdminController(Repos.UserRepo users, Repos.CommunityRepo communities, Repos.SpaceRepo spaces,
                           Repos.AppRepo apps, Repos.LoginLogRepo loginLogs, Repos.OperationLogRepo operationLogs,
                           Repos.ExternalApiLogRepo externalApiLogs, Repos.NoticeRepo notices,
                           AuditLogService auditLog, BootJobs bootJobs, PasswordService passwords,
                           SessionService sessions, LoginThrottle loginThrottle) {
        this.users = users;
        this.communities = communities;
        this.spaces = spaces;
        this.apps = apps;
        this.loginLogs = loginLogs;
        this.operationLogs = operationLogs;
        this.externalApiLogs = externalApiLogs;
        this.notices = notices;
        this.auditLog = auditLog;
        this.bootJobs = bootJobs;
        this.passwords = passwords;
        this.sessions = sessions;
        this.loginThrottle = loginThrottle;
    }

    private UserAccount requireAdmin(Integer uid) {
        if (uid == null) throw new ApiException(401, "请先登录后台管理员账号");
        UserAccount u = users.findById(uid).orElseThrow(() -> new ApiException(401, "管理员账号不存在"));
        if (!"SUPER_ADMIN".equals(u.role)) {
            throw new ApiException(403, "仅限系统运维管理员权限访问后台管理端！");
        }
        return u;
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        String username = String.valueOf(req.getOrDefault("username", "")).trim();
        loginThrottle.check(username, request);
        String password = String.valueOf(req.getOrDefault("password", ""));
        String ip = getIp(request);
        String ua = request.getHeader("User-Agent");

        UserAccount admin = users.findByPhoneOrIdCard(username, username)
                .filter(u -> "SUPER_ADMIN".equals(u.role))
                .orElse(null);

        // 如果用户使用 admin 登录，查找系统超级总管
        if (admin == null && demoEnabled && "admin".equalsIgnoreCase(username)) {
            admin = users.findAll().stream().filter(u -> "SUPER_ADMIN".equals(u.role)).findFirst().orElse(null);
        }

        if (admin == null || !passwords.matches(password, admin.passwordHash)) {
            loginThrottle.failure(username, request);
            auditLog.recordLogin(null, username, "未知管理员", "SUPER_ADMIN", "PASSWORD", ip, ua, false, "账号或密码错误");
            throw new ApiException(400, "管理员账号或密码错误");
        }

        loginThrottle.success(username, request);
        auditLog.recordLogin(admin.id, username, admin.realName, "SUPER_ADMIN", "PASSWORD", ip, ua, true, null);
        String token = sessions.create(admin);
        return ApiResponse.ok("管理员登录成功", ParkingService.map(
                "token", token,
                "user", ParkingService.map(
                        "id", admin.id,
                        "real_name", admin.realName,
                        "role", admin.role,
                        "phone", admin.phone
                )
        ));
    }

    @GetMapping("/stats")
    public ApiResponse stats(@RequestAttribute(name = "authUserId") Integer uid) {
        requireAdmin(uid);
        long totalUsers = users.count();
        long owners = users.findAll().stream().filter(u -> "OWNER".equals(u.role)).count();
        long propertyAdmins = users.findAll().stream().filter(u -> "PROPERTY_ADMIN".equals(u.role)).count();
        long totalSpaces = spaces.count();
        long totalApps = apps.count();
        long totalLogins = loginLogs.count();
        long totalApiLogs = externalApiLogs.count();
        long smsUpgraded = notices.findAll().stream().filter(n -> Boolean.TRUE.equals(n.smsUpgraded)).count();

        return ApiResponse.ok(ParkingService.map(
                "total_users", totalUsers,
                "owner_count", owners,
                "property_count", propertyAdmins,
                "total_spaces", totalSpaces,
                "total_applications", totalApps,
                "total_logins", totalLogins,
                "total_api_calls", totalApiLogs,
                "sms_upgraded_count", smsUpgraded
        ));
    }

    @GetMapping("/users")
    public ApiResponse userList(@RequestAttribute(name = "authUserId") Integer uid,
                                @RequestParam(required = false) String role) {
        requireAdmin(uid);
        List<UserAccount> list = users.findAll();
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            list = list.stream().filter(u -> role.equalsIgnoreCase(u.role)).toList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Integer, String> commNames = new HashMap<>();
        communities.findAll().forEach(c -> commNames.put(c.id, c.name));

        for (UserAccount u : list) {
            String maskedId = u.idCard != null && u.idCard.length() >= 10
                    ? u.idCard.substring(0, 6) + "********" + u.idCard.substring(u.idCard.length() - 4)
                    : "--";
            result.add(ParkingService.map(
                    "id", u.id,
                    "phone", u.phone,
                    "id_card_masked", maskedId,
                    "real_name", u.realName,
                    "role", u.role,
                    "role_label", "SUPER_ADMIN".equals(u.role) ? "系统超级管理员" : ("PROPERTY_ADMIN".equals(u.role) ? "物业管理员" : "社区业主"),
                    "community_id", u.communityId,
                    "community_name", commNames.getOrDefault(u.communityId, "未关联"),
                    "last_login_at", u.lastLoginAt != null ? Times.fmtBj(u.lastLoginAt) : "尚未登录",
                    "last_login_ip", u.lastLoginIp != null ? u.lastLoginIp : "--",
                    "login_count", u.loginCount != null ? u.loginCount : 0,
                    "created_at", Times.fmtBj(u.createdAt)
            ));
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/login-logs")
    public ApiResponse loginLogs(@RequestAttribute(name = "authUserId") Integer uid) {
        requireAdmin(uid);
        List<Map<String, Object>> result = new ArrayList<>();
        loginLogs.findTop50ByOrderByLoginAtDesc().forEach(l -> {
            result.add(ParkingService.map(
                    "id", l.id,
                    "user_id", l.userId,
                    "account", l.account,
                    "real_name", l.realName != null ? l.realName : "--",
                    "role", l.role != null ? l.role : "--",
                    "role_label", "SUPER_ADMIN".equals(l.role) ? "平台管理员" : ("PROPERTY_ADMIN".equals(l.role) ? "物业管理员" : "业主"),
                    "login_type", l.loginType,
                    "ip", l.ip,
                    "user_agent", l.userAgent,
                    "status", l.status,
                    "fail_reason", l.failReason,
                    "login_at", Times.fmtBj(l.loginAt)
            ));
        });
        return ApiResponse.ok(result);
    }

    @GetMapping("/operation-logs")
    public ApiResponse operationLogs(@RequestAttribute(name = "authUserId") Integer uid) {
        requireAdmin(uid);
        List<Map<String, Object>> result = new ArrayList<>();
        operationLogs.findTop50ByOrderByCreatedAtDesc().forEach(l -> {
            result.add(ParkingService.map(
                    "id", l.id,
                    "operator_id", l.operatorId,
                    "operator_name", l.operatorName,
                    "operator_role", l.operatorRole,
                    "module", l.module,
                    "action", l.action,
                    "summary", l.summary,
                    "ip", l.ip,
                    "details_json", l.detailsJson,
                    "created_at", Times.fmtBj(l.createdAt)
            ));
        });
        return ApiResponse.ok(result);
    }

    @GetMapping("/external-api-logs")
    public ApiResponse externalApiLogs(@RequestAttribute(name = "authUserId") Integer uid) {
        requireAdmin(uid);
        List<Map<String, Object>> result = new ArrayList<>();
        externalApiLogs.findTop50ByOrderByCreatedAtDesc().forEach(l -> {
            result.add(ParkingService.map(
                    "id", l.id,
                    "api_name", l.apiName,
                    "provider", l.provider,
                    "method", l.method,
                    "url", l.url,
                    "request_json", l.requestJson,
                    "response_json", l.responseJson,
                    "duration_ms", l.durationMs,
                    "status_code", l.statusCode,
                    "status", l.status,
                    "created_at", Times.fmtBj(l.createdAt)
            ));
        });
        return ApiResponse.ok(result);
    }

    @PostMapping("/trigger-sms-upgrade")
    public ApiResponse triggerSmsUpgrade(@RequestAttribute(name = "authUserId") Integer uid) {
        requireAdmin(uid);
        if (!demoEnabled) throw new ApiException(503, "短信服务尚未接入");
        int count = bootJobs.processUnreadSmsUpgrades(true);
        return ApiResponse.ok("24小时未读短信升级催办执行完毕！本次共升级催办并下发短信 " + count + " 条",
                ParkingService.map("upgraded_count", count));
    }

    private static String getIp(HttpServletRequest req) {
        if (req == null) return "127.0.0.1";
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }
}
