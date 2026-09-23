package com.huiting.parking.service;

import com.huiting.parking.common.Times;
import com.huiting.parking.entity.*;
import com.huiting.parking.repo.Repos;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Service
public class SeedService {
    private final Repos.UserRepo users;
    private final Repos.CommunityRepo communities;
    private final Repos.SpaceRepo spaces;
    private final Repos.AppRepo apps;
    private final Repos.ContractRepo contracts;
    private final Repos.NoticeRepo notices;
    private final Repos.EvidenceRepo evidenceRepo;
    private final EvidenceService evidence;
    private final AuditLogService auditLog;
    private final JdbcTemplate jdbc;

    public SeedService(Repos.UserRepo users, Repos.CommunityRepo communities, Repos.SpaceRepo spaces, Repos.AppRepo apps,
                       Repos.ContractRepo contracts, Repos.NoticeRepo notices, Repos.EvidenceRepo evidenceRepo,
                       EvidenceService evidence, @Lazy AuditLogService auditLog, JdbcTemplate jdbc) {
        this.users = users;
        this.communities = communities;
        this.spaces = spaces;
        this.apps = apps;
        this.contracts = contracts;
        this.notices = notices;
        this.evidenceRepo = evidenceRepo;
        this.evidence = evidence;
        this.auditLog = auditLog;
        this.jdbc = jdbc;
    }

    public static String hashPwd(String pwd) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pwd.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void init(boolean force) {
        if (!force && users.count() > 0) return;
        jdbc.execute("PRAGMA foreign_keys=OFF");
        for (String table : new String[]{"login_logs", "operation_logs", "external_api_logs", "notifications", "chain_evidence", "rental_contracts", "queue_applications", "parking_spaces", "users", "communities"}) {
            jdbc.execute("DELETE FROM " + table);
        }
        jdbc.execute("PRAGMA foreign_keys=ON");

        Community community = new Community();
        community.name = "龙华新村";
        community.district = "徐汇区";
        community.subdistrict = "龙华街道";
        community.totalSpaces = 50;
        community.auditMode = "THREE_CERT_AUTO";
        community = communities.save(community);

        UserAccount zhang = user("13800000001", "310104199201011111", "张欣欣", "OWNER", community.id);
        UserAccount li = user("13800000002", "310104199002022222", "李雷", "OWNER", community.id);
        UserAccount han = user("13800000003", "310104199103033333", "韩梅梅", "OWNER", community.id);
        UserAccount wang = user("13900000001", "310104198005055555", "王建国", "PROPERTY_ADMIN", community.id);
        UserAccount zhao = user("13800000004", "310104198804044444", "赵强", "OWNER", community.id);
        UserAccount qian = user("13800000005", "310104198905055555", "钱小敏", "OWNER", community.id);
        UserAccount sun = user("13800000006", "310104197506066666", "孙立新", "OWNER", community.id);
        UserAccount admin = user("13900000000", "310104198001010001", "系统运维总管", "SUPER_ADMIN", community.id);
        admin.passwordHash = hashPwd("admin123");
        admin.lastLoginAt = Times.utcNow().minusHours(1);
        admin.lastLoginIp = "127.0.0.1";
        admin.loginCount = 16;

        users.save(zhang); users.save(li); users.save(han); users.save(wang); users.save(zhao); users.save(qian); users.save(sun); users.save(admin);

        String surnames = "陈林黄张李王吴刘蔡杨许郑谢郭洪曾邱赖廖周";
        String[] givens = {"伟", "芳", "娜", "敏", "静", "秀", "强", "磊", "军", "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "兰", "霞", "平"};
        UserAccount[] virtuals = new UserAccount[46];
        for (int i = 1; i <= 46; i++) {
            long idnum = 197001010101L + (long) i * 137;
            virtuals[i - 1] = user(String.format("1391%07d", i), "310104" + String.format("%012d", idnum),
                    surnames.charAt(i % surnames.length()) + givens[(i * 7) % givens.length], "OWNER", community.id);
            users.save(virtuals[i - 1]);
        }

        ParkingSpace[] sps = new ParkingSpace[50];
        int vIdx = 0;
        for (int i = 1; i <= 50; i++) {
            ParkingSpace sp = new ParkingSpace();
            sp.communityId = community.id;
            sp.spaceNo = String.format("A-%03d", i);
            sp.areaType = i <= 30 ? "GROUND" : "UNDERGROUND";
            sp.status = "RENTED";
            if (i == 8) sp.currentUserId = han.id;
            else if (i == 12) { sp.status = "ALLOCATED"; sp.currentUserId = li.id; }
            else if (i == 15) sp.currentUserId = sun.id;
            else if (i == 50) { sp.status = "MAINTENANCE"; sp.currentUserId = null; }
            else if (vIdx < virtuals.length) sp.currentUserId = virtuals[vIdx++].id;
            sps[i - 1] = spaces.save(sp);
        }

        LocalDate now = LocalDate.now();
        LocalDate past1 = now.minusDays(335);
        LocalDate end1 = past1.plusDays(365);
        contracts.save(contract("AGY-CT-20251025-A008-88A1", sps[7].id, han.id, "沪A·67890", past1, end1, "ACTIVE"));

        LocalDate past2 = now.minusDays(350);
        LocalDate end2 = past2.plusDays(365);
        contracts.save(contract("AGY-CT-20251010-A015-99B2", sps[14].id, sun.id, "沪A·11223", past2, end2, "EXPIRING"));

        vIdx = 0;
        for (int i = 1; i <= 50; i++) {
            if (i == 8 || i == 12 || i == 15 || i == 50) continue;
            if (vIdx < virtuals.length) {
                UserAccount u = virtuals[vIdx++];
                LocalDate s = now.minusDays(60 + (i * 5));
                LocalDate e = s.plusDays(365);
                String st = e.isBefore(now) ? "OVERDUE" : (e.minusDays(30).isBefore(now) ? "EXPIRING" : "ACTIVE");
                contracts.save(contract(String.format("AGY-CT-MOCK-A%03d-%04d", i, u.id), sps[i - 1].id, u.id,
                        String.format("沪A·%05d", 10000 + i * 37), s, e, st));
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime base = LocalDateTime.parse("2026-09-17 09:29:09", fmt);
        apps.save(waiting(zhang.id, community.id, "12号楼 302室", "沪A·12345", 1, base));
        apps.save(waiting(zhao.id, community.id, "6号楼 401室", "沪A·44556", 2, base.plusHours(3)));
        apps.save(waiting(qian.id, community.id, "15号楼 202室", "沪A·77889", 3, base.plusHours(7)));

        QueueApplication appLi = new QueueApplication();
        appLi.userId = li.id;
        appLi.communityId = community.id;
        appLi.propertyNo = "10号楼 201室";
        appLi.plateNo = "沪A·99999";
        appLi.priorityLevel = 1;
        appLi.currentRank = 0;
        appLi.status = "ALLOCATED";
        appLi.allocatedSpaceId = sps[11].id;
        appLi.allocatedTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(18);
        appLi.applyTime = base.minusDays(2);
        apps.save(appLi);

        evidence.create(community.id, null, "INIT", "【" + community.name + "】全小区 50 个车位资产台账初始化导入锁定",
                Map.of("total_spaces", 50, "manager", wang.realName), null);

        // 初始通知
        notice(zhang.id, "排队顺位提醒", "恭喜！您在【龙华新村】车位申请当前排位：第【1】位（候补首位）。一旦有车位清退腾空，系统将第一时间为您锁定并推送意向确认通知。", "RANK_CHANGE", "GO_QUEUE");
        
        // 为李雷生成一条超过 24 小时未读的中签通知，供 24 小时未读催办演示使用
        Notification overdueNotice = new Notification();
        overdueNotice.userId = li.id;
        overdueNotice.title = "车位中签锁定提醒";
        overdueNotice.content = "恭喜！系统已按排队规则为您锁定车位【A-012】，请在 48 小时内确认前往物业办公室完成线下签约缴费！";
        overdueNotice.type = "ALLOCATION";
        overdueNotice.actionType = "GO_CONFIRM";
        overdueNotice.readFlag = false;
        overdueNotice.smsUpgraded = false;
        overdueNotice.createdAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(26);
        notices.save(overdueNotice);

        // 预置初始登录日志
        auditLog.recordLogin(admin.id, "admin", admin.realName, "SUPER_ADMIN", "PASSWORD", "127.0.0.1", "Chrome 128 / Windows 11", true, null);
        auditLog.recordLogin(wang.id, wang.phone, wang.realName, "PROPERTY_ADMIN", "PASSWORD", "192.168.1.108", "Chrome 128 / Windows 11", true, null);
        auditLog.recordLogin(zhang.id, zhang.phone, zhang.realName, "OWNER", "PASSWORD", "192.168.1.205", "Mobile Safari / iOS 17", true, null);
        auditLog.recordLogin(li.id, li.phone, li.realName, "OWNER", "SMS", "192.168.1.221", "WeChat / Android 14", true, null);

        // 预置初始操作日志
        auditLog.recordOperation(admin.id, admin.realName, "SUPER_ADMIN", "SYSTEM", "SYSTEM_INIT", "平台管理员初始化系统演示数据库及种子资产", "127.0.0.1", Map.of("spaces", 50, "community", community.name));
        auditLog.recordOperation(wang.id, wang.realName, "PROPERTY_ADMIN", "SPACE", "SET_MAINTENANCE", "物业登记车位【A-050】进入地坪检修停用状态", "192.168.1.108", Map.of("space_no", "A-050", "reason", "地坪破损修复"));
        auditLog.recordOperation(zhang.id, zhang.realName, "OWNER", "APPLICATION", "APPLY", "业主【张欣欣】提交车位排队申请（三证合一通过）", "192.168.1.205", Map.of("property_no", "12号楼 302室", "plate_no", "沪A·12345"));

        // 预置初始外部接口调用日志
        auditLog.recordExternalApi("公安实人身份核验接口", "上海市大数据中心·公安便民认证中台", "POST", "https://api.shanghai.gov.cn/security/v1/real-person-verify",
                Map.of("name", "张欣欣", "id_card", "310104199201011111"), Map.of("result", "PASS", "confidence", 0.998), 120L, 200, true);
        auditLog.recordExternalApi("不动产权属联网核验接口", "上海市自然资源与不动产登记服务平台", "POST", "https://bdc.shanghai.gov.cn/api/v2/property-check",
                Map.of("property_no", "龙华新村 12号楼 302室", "owner", "张欣欣"), Map.of("matched", true, "cert_no", "沪(2022)徐字不动产权第018892号"), 185L, 200, true);
        auditLog.recordExternalApi("公安交警车辆行驶证核验接口", "上海市公安局交警总队车辆管理所", "POST", "https://shjj.gov.cn/api/vehicle/license-verify",
                Map.of("plate_no", "沪A·12345", "owner", "张欣欣"), Map.of("vehicle_type", "小型轿车", "status", "NORMAL"), 95L, 200, true);
        auditLog.recordExternalApi("移动政务云短信下发网关", "中国移动通信集团上海分公司", "POST", "https://api.shanghai.gov.cn/sms/v2/send-verify-code",
                Map.of("phone", "13800000001", "template", "SMS_LOGIN_CODE"), Map.of("code", "0000", "message", "发送成功"), 45L, 200, true);
    }

    private UserAccount user(String phone, String idCard, String name, String role, Integer cid) {
        UserAccount u = new UserAccount();
        u.phone = phone;
        u.idCard = idCard;
        u.passwordHash = hashPwd("123456");
        u.realName = name;
        u.role = role;
        u.communityId = cid;
        u.createdAt = Times.utcNow();
        u.loginCount = "OWNER".equals(role) ? 3 : 8;
        u.lastLoginAt = Times.utcNow().minusHours(5);
        u.lastLoginIp = "127.0.0.1";
        return u;
    }

    private RentalContract contract(String no, Integer spaceId, Integer userId, String plate, LocalDate start, LocalDate end, String status) {
        RentalContract c = new RentalContract();
        c.contractNo = no;
        c.spaceId = spaceId;
        c.userId = userId;
        c.plateNo = plate;
        c.startDate = start;
        c.endDate = end;
        c.status = status;
        c.createdAt = Times.utcNow();
        return c;
    }

    private QueueApplication waiting(Integer uid, Integer cid, String room, String plate, int rank, LocalDateTime apply) {
        QueueApplication a = new QueueApplication();
        a.userId = uid;
        a.communityId = cid;
        a.propertyNo = room;
        a.plateNo = plate;
        a.priorityLevel = 1;
        a.currentRank = rank;
        a.status = "WAITING";
        a.applyTime = apply;
        return a;
    }

    private void notice(Integer uid, String title, String content, String type, String action) {
        Notification n = new Notification();
        n.userId = uid;
        n.title = title;
        n.content = content;
        n.type = type;
        n.actionType = action;
        n.readFlag = false;
        n.smsUpgraded = false;
        n.createdAt = Times.utcNow();
        notices.save(n);
    }
}
