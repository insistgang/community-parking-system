package com.huiting.parking.service;

import com.huiting.parking.entity.Notification;
import com.huiting.parking.entity.ParkingSpace;
import com.huiting.parking.entity.QueueApplication;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class QueueEngine {
    private final Repos.AppRepo apps;
    private final Repos.SpaceRepo spaces;
    private final Repos.UserRepo users;
    private final Repos.NoticeRepo notices;
    private final EvidenceService evidence;

    public QueueEngine(Repos.AppRepo apps, Repos.SpaceRepo spaces, Repos.UserRepo users, Repos.NoticeRepo notices, @Lazy EvidenceService evidence) {
        this.apps = apps;
        this.spaces = spaces;
        this.users = users;
        this.notices = notices;
        this.evidence = evidence;
    }

    public static String[] estimate(int rank, int totalSpaces) {
        String basis = "基于本小区近一年年化退租周转率 8% (年均释放约4个车位) 及当前顺位测算";
        if (rank <= 0) return new String[]{"即将分配", basis};
        if (rank == 1) return new String[]{"约 1~2 周 (候补首位)", basis};
        if (rank == 2) return new String[]{"约 1~2 个月", basis};
        int est = (int) Math.round(rank * (12.0 / Math.max(1, totalSpaces * 0.08)));
        int min = Math.max(1, est - 1);
        return new String[]{"约 " + min + "~" + (est + 1) + " 个月", basis};
    }

    @Transactional
    public void notifyApplicants(Integer communityId, String title, String content, Collection<Integer> extra, Collection<Integer> exclude, String type, String action) {
        Set<Integer> skip = new HashSet<>();
        if (exclude != null) exclude.stream().filter(Objects::nonNull).forEach(skip::add);
        Set<Integer> ids = new LinkedHashSet<>();
        apps.findByCommunityIdAndStatusIn(communityId, List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING"))
                .forEach(a -> { if (a.userId != null) ids.add(a.userId); });
        if (extra != null) extra.stream().filter(Objects::nonNull).forEach(ids::add);
        for (Integer uid : ids) {
            if (skip.contains(uid)) continue;
            Notification n = new Notification();
            n.userId = uid;
            n.title = title;
            n.content = content;
            n.type = type == null ? "SYSTEM" : type;
            n.actionType = action == null ? "GO_QUEUE" : action;
            n.createdAt = LocalDateTime.now(ZoneOffset.UTC);
            notices.save(n);
        }
    }

    @Transactional
    public void recalculate(Integer communityId) {
        List<QueueApplication> bad = apps.findByCommunityIdAndStatusIn(communityId, List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING"))
                .stream().filter(a -> {
                    UserAccount u = users.findById(a.userId).orElse(null);
                    return u != null && "PROPERTY_ADMIN".equals(u.role);
                }).toList();
        for (QueueApplication app : bad) {
            app.status = "CANCELLED";
            app.currentRank = 0;
            if (app.allocatedSpaceId != null) {
                spaces.findById(app.allocatedSpaceId).ifPresent(sp -> {
                    if ("ALLOCATED".equals(sp.status) || "OFFLINE_PENDING".equals(sp.status)) {
                        sp.status = "VACANT";
                        sp.currentUserId = null;
                    }
                });
                app.allocatedSpaceId = null;
            }
        }

        List<ParkingSpace> vacant = new ArrayList<>(spaces.findByCommunityIdAndStatusOrderBySpaceNoAsc(communityId, "VACANT"));
        List<QueueApplication> waiting = apps.findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(communityId, "WAITING")
                .stream().filter(a -> {
                    UserAccount u = users.findById(a.userId).orElse(null);
                    return u == null || !"PROPERTY_ADMIN".equals(u.role);
                }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        while (!vacant.isEmpty() && !waiting.isEmpty()) {
            ParkingSpace space = vacant.remove(0);
            QueueApplication app = waiting.remove(0);
            space.status = "ALLOCATED";
            space.currentUserId = app.userId;
            app.status = "ALLOCATED";
            app.allocatedSpaceId = space.id;
            app.allocatedTime = LocalDateTime.now(ZoneOffset.UTC);
            app.currentRank = 0;
            evidence.create(communityId, app.userId, "ALLOCATE",
                    "系统自动分配车位【" + space.spaceNo + "】(启动48小时中签倒计时)",
                    Map.of("space_no", space.spaceNo, "allocated_time", String.valueOf(app.allocatedTime)), null);
            Notification n = new Notification();
            n.userId = app.userId;
            n.title = "车位中签锁定提醒";
            n.content = "恭喜！系统已按排队规则为您锁定车位【" + space.spaceNo + "】，请在 48 小时内确认前往物业办公室完成线下签约缴费！";
            n.type = "ALLOCATION";
            n.actionType = "GO_CONFIRM";
            n.createdAt = LocalDateTime.now(ZoneOffset.UTC);
            notices.save(n);
        }
        int rank = 1;
        for (QueueApplication app : waiting) {
            int old = app.currentRank == null ? 0 : app.currentRank;
            if (old != rank) {
                app.currentRank = rank;
                if (old > rank || old == 0) {
                    Notification n = new Notification();
                    n.userId = app.userId;
                    n.title = rank == 1 ? "候补首位通知" : "排队顺位提醒";
                    n.content = "您的实时排位是第【" + rank + "】位";
                    n.type = "RANK_CHANGE";
                    n.actionType = "GO_QUEUE";
                    n.createdAt = LocalDateTime.now(ZoneOffset.UTC);
                    notices.save(n);
                }
            }
            rank++;
        }
    }

    @Transactional
    public void checkExpired(Integer communityId) {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusHours(48);
        List<QueueApplication> expired = apps.findByCommunityIdAndStatusIn(communityId, List.of("ALLOCATED")).stream()
                .filter(a -> a.allocatedTime != null && a.allocatedTime.isBefore(cutoff)).toList();
        if (expired.isEmpty()) return;
        for (QueueApplication app : expired) {
            app.status = "TIMEOUT_CANCELLED";
            String spaceNo = "--";
            if (app.allocatedSpaceId != null) {
                ParkingSpace space = spaces.findById(app.allocatedSpaceId).orElse(null);
                if (space != null) {
                    spaceNo = space.spaceNo;
                    space.status = "VACANT";
                    space.currentUserId = null;
                }
            }
            evidence.create(communityId, app.userId, "CANCEL", "中签车位超过48小时未确认，系统自动释放顺延",
                    Map.of("app_id", app.id, "reason", "TIMEOUT_48H"), null);
            Notification n = new Notification();
            n.userId = app.userId;
            n.title = "车位中签超时失效通知";
            n.content = "您此前锁定的车位因超过 48 小时未确认办理，系统已自动收回并顺延分配给后续业主。若需申请请重新排队。";
            n.type = "OVERDUE";
            n.actionType = "GO_QUEUE";
            n.createdAt = LocalDateTime.now(ZoneOffset.UTC);
            notices.save(n);
            notifyApplicants(communityId, "中签车位超时释放公告",
                    "车位【" + spaceNo + "】因中签业主超时未确认，已释放回池。系统将按排队顺位自动顺延，请关注您的排位与中签通知。",
                    null, List.of(app.userId), "SYSTEM", "GO_QUEUE");
        }
        recalculate(communityId);
    }

    @Transactional
    public List<ParkingSpace> initSpaces(Integer communityId, int count) {
        char prefix = (char) ('A' + (communityId - 1) % 26);
        List<ParkingSpace> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ParkingSpace sp = new ParkingSpace();
            sp.communityId = communityId;
            sp.spaceNo = prefix + "-" + String.format("%03d", i);
            sp.areaType = i <= 30 ? "GROUND" : "UNDERGROUND";
            sp.status = "VACANT";
            list.add(sp);
        }
        return spaces.saveAll(list);
    }
}
