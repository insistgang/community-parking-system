package com.huiting.parking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.ExternalApiLog;
import com.huiting.parking.entity.LoginLog;
import com.huiting.parking.entity.OperationLog;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private final Repos.LoginLogRepo loginLogs;
    private final Repos.OperationLogRepo operationLogs;
    private final Repos.ExternalApiLogRepo externalApiLogs;
    private final Repos.UserRepo users;
    private final ObjectMapper json;

    public AuditLogService(Repos.LoginLogRepo loginLogs, Repos.OperationLogRepo operationLogs,
                           Repos.ExternalApiLogRepo externalApiLogs, Repos.UserRepo users, ObjectMapper json) {
        this.loginLogs = loginLogs;
        this.operationLogs = operationLogs;
        this.externalApiLogs = externalApiLogs;
        this.users = users;
        this.json = json;
    }

    @Transactional
    public void recordLogin(Integer userId, String account, String realName, String role, String loginType, String ip, String userAgent, boolean success, String failReason) {
        LoginLog log = new LoginLog();
        log.userId = userId;
        log.account = account;
        log.realName = realName;
        log.role = role;
        log.loginType = loginType == null ? "PASSWORD" : loginType;
        log.ip = ip == null || ip.isBlank() ? "127.0.0.1" : ip;
        log.userAgent = userAgent == null || userAgent.isBlank() ? "Chrome / Windows 11" : userAgent;
        log.status = success ? "SUCCESS" : "FAIL";
        log.failReason = failReason;
        log.loginAt = Times.utcNow();
        loginLogs.save(log);

        if (success && userId != null) {
            users.findById(userId).ifPresent(u -> {
                u.lastLoginAt = Times.utcNow();
                u.lastLoginIp = log.ip;
                u.loginCount = (u.loginCount == null ? 0 : u.loginCount) + 1;
                users.save(u);
            });
        }
    }

    @Transactional
    public void recordOperation(Integer operatorId, String operatorName, String operatorRole, String module, String action, String summary, String ip, Object details) {
        OperationLog log = new OperationLog();
        log.operatorId = operatorId;
        log.operatorName = operatorName == null ? "系统" : operatorName;
        log.operatorRole = operatorRole == null ? "SYSTEM" : operatorRole;
        log.module = module;
        log.action = action;
        log.summary = summary;
        log.ip = ip == null || ip.isBlank() ? "127.0.0.1" : ip;
        try {
            log.detailsJson = details == null ? "{}" : (details instanceof String s ? s : json.writeValueAsString(details));
        } catch (Exception e) {
            log.detailsJson = "{}";
        }
        log.createdAt = Times.utcNow();
        operationLogs.save(log);
    }

    @Transactional
    public void recordExternalApi(String apiName, String provider, String method, String url, Object request, Object response, long durationMs, int statusCode, boolean success) {
        ExternalApiLog log = new ExternalApiLog();
        log.apiName = apiName;
        log.provider = provider;
        log.method = method == null ? "POST" : method;
        log.url = url;
        try {
            log.requestJson = request == null ? "{}" : (request instanceof String s ? s : json.writeValueAsString(request));
        } catch (Exception e) {
            log.requestJson = "{}";
        }
        try {
            log.responseJson = response == null ? "{}" : (response instanceof String s ? s : json.writeValueAsString(response));
        } catch (Exception e) {
            log.responseJson = "{}";
        }
        log.durationMs = durationMs;
        log.statusCode = statusCode;
        log.status = success ? "SUCCESS" : "FAIL";
        log.createdAt = Times.utcNow();
        externalApiLogs.save(log);
    }
}
