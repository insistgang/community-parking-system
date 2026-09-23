package com.huiting.parking.service;

import com.huiting.parking.common.Times;
import com.huiting.parking.entity.*;
import com.huiting.parking.repo.Repos;
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
    private final JdbcTemplate jdbc;

    public SeedService(Repos.UserRepo users, Repos.CommunityRepo communities, Repos.SpaceRepo spaces, Repos.AppRepo apps,
                       Repos.ContractRepo contracts, Repos.NoticeRepo notices, Repos.EvidenceRepo evidenceRepo,
                       EvidenceService evidence, JdbcTemplate jdbc) {
        this.users = users;
        this.communities = communities;
        this.spaces = spaces;
        this.apps = apps;
        this.contracts = contracts;
        this.notices = notices;
        this.evidenceRepo = evidenceRepo;
        this.evidence = evidence;
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
        for (String table : new String[]{"notifications", "chain_evidence", "rental_contracts", "queue_applications", "parking_spaces", "users", "communities"}) {
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
        users.save(zhang); users.save(li); users.save(han); users.save(wang); users.save(zhao); users.save(qian); users.save(sun);

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
            else sp.currentUserId = virtuals[vIdx++].id;
            sps[i - 1] = spaces.save(sp);
        }

        LocalDate today = LocalDate.now();
        contracts.save(contract("AGY-CT-20251001-0008", sps[7].id, han.id, "沪A·88888", today.minusDays(340), today.plusDays(25), "EXPIRING"));
        contracts.save(contract("AGY-CT-20250801-0015", sps[14].id, sun.id, "沪B·99999", today.minusDays(370), today.minusDays(5), "OVERDUE"));
        int v2 = 0;
        for (int i = 1; i <= 49; i++) {
            if (i == 8 || i == 12 || i == 15) continue;
            UserAccount vu = virtuals[v2++];
            LocalDate start = today.minusDays(10 + (i * 7) % 300);
            int plate = (10000 + i * 37) % 90000 + 10000;
            char letter = (char) ('A' + i % 6);
            contracts.save(contract("AGY-CT-" + start.format(DateTimeFormatter.BASIC_ISO_DATE) + "-A" + String.format("%03d", i),
                    sps[i - 1].id, vu.id, "沪" + letter + "·" + String.format("%05d", plate), start, start.plusDays(365), "ACTIVE"));
        }

        LocalDateTime now = Times.utcNow();
        QueueApplication appLi = new QueueApplication();
        appLi.userId = li.id;
        appLi.communityId = community.id;
        appLi.propertyNo = "8号楼201室";
        appLi.plateNo = "沪A·22222";
        appLi.priorityLevel = 1;
        appLi.currentRank = 0;
        appLi.status = "ALLOCATED";
        appLi.allocatedSpaceId = sps[11].id;
        appLi.allocatedTime = now.minusMinutes(10);
        appLi.applyTime = now.minusDays(10);
        apps.save(appLi);
        apps.save(waiting(zhao.id, community.id, "9号楼102室", "沪A·44444", 1, now.minusDays(5)));
        apps.save(waiting(qian.id, community.id, "10号楼403室", "沪A·55555", 2, now.minusDays(3)));

        evidence.create(community.id, wang.id, "INIT", "物业管理员王建国完成【龙华新村】50个车位台账初始化导入锁定",
                Map.of("total_spaces", 50, "manager", "王建国", "community", "龙华新村"),
                LocalDateTime.of(2024, 6, 1, 1, 0, 0));
        evidence.create(community.id, li.id, "ALLOCATE", "系统自动分配车位【A-012】给李雷 (启动48小时倒计时)",
                Map.of("space_no", "A-012", "user", "李雷"), now.minusMinutes(10));

        notice(li.id, "车位中签锁定提醒", "恭喜！系统已按排队顺位为您锁定车位【A-012】，请在 48 小时内确认前往物业办公室办理线下签约！", "ALLOCATION", "GO_CONFIRM");
        notice(han.id, "车位租约即将到期提醒", "您的车位【A-008】租约剩余 25 天到期，系统已开放续租入口，请及时前往物业办理续签以防逾期自动清退！", "EXPIRATION", "GO_CONFIRM");
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
        n.createdAt = Times.utcNow();
        notices.save(n);
    }
}
