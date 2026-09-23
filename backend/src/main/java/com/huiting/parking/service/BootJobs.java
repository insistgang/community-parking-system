package com.huiting.parking.service;

import com.huiting.parking.common.Times;
import com.huiting.parking.entity.Notification;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
public class BootJobs {
    private final SeedService seed;
    private final Repos.CommunityRepo communities;
    private final QueueEngine engine;
    private final Repos.NoticeRepo notices;
    private final Repos.UserRepo users;
    private final AuditLogService auditLog;

    public BootJobs(SeedService seed, Repos.CommunityRepo communities, QueueEngine engine,
                    Repos.NoticeRepo notices, Repos.UserRepo users, AuditLogService auditLog) {
        this.seed = seed;
        this.communities = communities;
        this.engine = engine;
        this.notices = notices;
        this.users = users;
        this.auditLog = auditLog;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seed.init(false);
    }

    @Scheduled(fixedDelay = 60000)
    public void sweep() {
        communities.findAll().forEach(c -> engine.checkExpired(c.id));
        processUnreadSmsUpgrades(false);
    }

    @Transactional
    public int processUnreadSmsUpgrades(boolean forceAllUnreadForTesting) {
        LocalDateTime cutoff = forceAllUnreadForTesting
                ? LocalDateTime.now(ZoneOffset.UTC)
                : LocalDateTime.now(ZoneOffset.UTC).minusHours(24);

        List<Notification> unreadList = notices.findByReadFlagFalseAndSmsUpgradedFalseAndCreatedAtBefore(cutoff);
        int count = 0;
        for (Notification n : unreadList) {
            n.smsUpgraded = true;
            n.smsUpgradedAt = Times.utcNow();
            notices.save(n);
            count++;

            UserAccount u = users.findById(n.userId).orElse(null);
            String phone = u != null ? u.phone : "13800000001";
            String name = u != null ? u.realName : "业主";

            auditLog.recordExternalApi(
                    "移动政务云短信下发网关",
                    "中国移动通信集团上海分公司政企业务部",
                    "POST",
                    "https://api.shanghai.gov.cn/sms/v2/send-template",
                    Map.of(
                            "phone", phone,
                            "template_id", "SMS_24H_UNREAD_ESCALATE",
                            "params", Map.of("name", name, "title", n.title, "content", n.content)
                    ),
                    Map.of(
                            "code", "0000",
                            "message", "运营商下发成功",
                            "msg_id", "SMS_24H_" + System.currentTimeMillis() + "_" + n.id
                    ),
                    92L,
                    200,
                    true
            );

            auditLog.recordOperation(
                    n.userId,
                    name,
                    "SYSTEM",
                    "NOTIFICATION",
                    "SMS_UPGRADE_SENT",
                    "站内提醒【" + n.title + "】超过 24 小时未查阅，系统已自动通过短信备用通道再次催办提醒（目标手机号: " + phone + "）",
                    "127.0.0.1",
                    Map.of("notice_id", n.id, "phone", phone, "title", n.title)
            );
        }
        return count;
    }
}
