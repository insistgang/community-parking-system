package com.huiting.parking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.common.ApiException;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.*;
import com.huiting.parking.repo.Repos;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ParkingService {
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;
    private static final List<String> LIVE = List.of("ACTIVE", "EXPIRING", "OVERDUE");
    private static final List<String> OPEN_APP = List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING", "PENDING_REVIEW", "REJECTED");

    private final Repos.UserRepo users;
    private final Repos.CommunityRepo communities;
    private final Repos.SpaceRepo spaces;
    private final Repos.AppRepo apps;
    private final Repos.ContractRepo contracts;
    private final Repos.NoticeRepo notices;
    private final QueueEngine engine;
    private final EvidenceService evidence;
    private final ObjectMapper json;
    private final ApplicationDocumentService documents;

    public ParkingService(Repos.UserRepo users, Repos.CommunityRepo communities, Repos.SpaceRepo spaces, Repos.AppRepo apps,
                          Repos.ContractRepo contracts, Repos.NoticeRepo notices, QueueEngine engine, EvidenceService evidence, ObjectMapper json,
                          ApplicationDocumentService documents) {
        this.users = users;
        this.communities = communities;
        this.spaces = spaces;
        this.apps = apps;
        this.contracts = contracts;
        this.notices = notices;
        this.engine = engine;
        this.evidence = evidence;
        this.json = json;
        this.documents = documents;
    }

    public UserAccount requireUser(Integer id) {
        if (id == null) throw new ApiException(401, "用户身份未认证");
        return users.findById(id).orElseThrow(() -> new ApiException(401, "用户身份未认证"));
    }

    @Transactional
    public Map<String, Object> home(UserAccount user) {
        engine.checkExpired(user.communityId);
        Community community = communities.findById(user.communityId).orElse(null);
        RentalContract contract = contracts.findFirstByUserIdAndStatusIn(user.id, LIVE).orElse(null);
        Map<String, Object> mySpace = null;
        if (contract != null) {
            ParkingSpace space = spaces.findById(contract.spaceId).orElse(null);
            int days = (int) ChronoUnit.DAYS.between(LocalDate.now(), contract.endDate);
            mySpace = map("space_no", space != null ? space.spaceNo : "--", "plate_no", contract.plateNo,
                    "start_date", String.valueOf(contract.startDate), "end_date", String.valueOf(contract.endDate),
                    "days_left", Math.max(0, days), "status", contract.status);
        }
        QueueApplication app = apps.findByUserIdAndStatusInOrderByIdDesc(user.id, OPEN_APP).stream().findFirst().orElse(null);
        if (app == null) app = pendingLegacyQueue(user);
        boolean identityPending = !demoEnabled && "OWNER".equals(user.role) && (user.idCard == null || user.idCard.isBlank());
        String view = contract != null ? "RENTED" : (app != null ? app.status : (identityPending ? "IDENTITY_PENDING" : "NO_APPLY"));
        Map<String, Object> myQueue = null;
        if (contract == null && app != null) myQueue = queueView(app, user, community);
        long unread = notices.countByUserIdAndReadFlag(user.id, false);
        int total = community != null && community.totalSpaces != null ? community.totalSpaces : 0;
        String mode = community != null && community.auditMode != null ? community.auditMode : "THREE_CERT_AUTO";
        List<Map<String, Object>> publicSpaces = new ArrayList<>();
        spaces.findByCommunityIdOrderBySpaceNoAsc(user.communityId).stream().limit(20).forEach(sp -> publicSpaces.add(maskSpace(sp)));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("view_state", view);
        data.put("user_info", map("id", user.id, "real_name", user.realName, "role", user.role,
                "identity_pending", identityPending,
                "community_name", community != null ? community.name : "未配置", "community_audit_mode", mode));
        data.put("unread_notifications", unread);
        data.put("my_space", mySpace);
        data.put("my_queue", myQueue);
        data.put("community_overview", map(
                "total_spaces", total,
                "rented_spaces", spaces.countByCommunityIdAndStatus(user.communityId, "RENTED"),
                "vacant_spaces", spaces.countByCommunityIdAndStatus(user.communityId, "VACANT"),
                "maintenance_spaces", spaces.countByCommunityIdAndStatus(user.communityId, "MAINTENANCE"),
                "allocated_spaces", spaces.countByCommunityIdAndStatusIn(user.communityId, List.of("ALLOCATED", "OFFLINE_PENDING")),
                "pending_audit_count", apps.countByCommunityIdAndStatus(user.communityId, "PENDING_REVIEW"),
                "inventory_complete", community == null || !Boolean.FALSE.equals(community.inventoryComplete),
                "inventory_note", community != null && Boolean.FALSE.equals(community.inventoryComplete)
                        ? "当前仅录入历史在租车位，空闲及维修车位尚待物业补录。" : "",
                "audit_mode", mode));
        data.put("public_spaces", publicSpaces);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> auditMode(UserAccount user) {
        Community comm = communities.findById(user.communityId).orElse(null);
        String commMode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        String userMode = user.auditMode;
        boolean custom = "THREE_CERT_AUTO".equals(userMode) || "FLEXIBLE_HYBRID".equals(userMode);
        String effective = custom ? userMode : commMode;
        return map("community_id", comm != null ? comm.id : user.communityId,
                "community_name", comm != null ? comm.name : "龙华新村",
                "audit_mode", effective, "community_audit_mode", commMode, "user_audit_mode", userMode,
                "is_user_customized", custom,
                "mode_label", "THREE_CERT_AUTO".equals(effective) ? "标准申报流程" : "灵活申报流程");
    }

    @Transactional
    public Map<String, Object> apply(UserAccount user, Map<String, Object> req) {
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        if (!"OWNER".equals(user.role)) throw new ApiException(403, "仅业主账号可以申请车位");
        if (!demoEnabled && (user.idCard == null || user.idCard.isBlank()))
            throw new ApiException(403, "身份资料待核实，请联系物业补齐后再申请");
        String propertyNo = str(req.get("property_no")).trim();
        String plate = str(req.get("plate_no")).trim();
        if (propertyNo.isBlank() || propertyNo.length() > 100 || plate.isBlank() || plate.length() > 32)
            throw new ApiException(400, "请填写有效的房屋和车牌信息");
        if (!demoEnabled) {
            plate = plate.toUpperCase(Locale.ROOT).replaceAll("[\\s·.\\-]", "");
            if (!plate.matches("[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]"))
                throw new ApiException(400, "请输入完整车牌号码，例如沪A12345或沪AD12345；特殊号牌请联系物业办理");
        }
        if (propertyNo.contains("浦东") || propertyNo.contains("世纪大道") || propertyNo.contains("外区"))
            throw new ApiException(400, "仅限本小区产权房屋可申请车位，外区房产无法通过核验！");
        if (apps.findFirstByUserIdAndStatusIn(user.id, List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING", "PENDING_REVIEW")).isPresent())
            throw new ApiException(400, "您已有正在生效或轮候中的申请，不可重复发起！");
        apps.findByUserIdAndStatus(user.id, "REJECTED").forEach(a -> a.status = "CANCELLED");
        boolean house = !apps.findByCommunityIdAndPropertyNoAndStatusIn(user.communityId, propertyNo,
                List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING", "PENDING_REVIEW", "COMPLETED")).isEmpty();
        boolean rented = contracts.findFirstByUserIdAndStatusIn(user.id, LIVE).isPresent();
        int priority = (house || rented) ? 2 : 1;
        boolean spouseFlag = Boolean.TRUE.equals(req.get("is_spouse_car"));
        String rel = str(req.get("car_relationship"));
        if (rel.isEmpty()) rel = spouseFlag ? "SPOUSE" : "SELF";
        if (!demoEnabled && !List.of("SELF", "SPOUSE", "FAMILY").contains(rel)) throw new ApiException(400, "请选择有效的车辆关系");
        boolean spouse = "SPOUSE".equals(rel) || spouseFlag;
        String relLabel = "SPOUSE".equals(rel) ? "配偶车(结婚证核验)" : ("FAMILY".equals(rel) ? "家人车(户口本核验)" : "本人名下");
        Community comm = communities.findById(user.communityId).orElse(null);
        String commMode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        String userMode = user.auditMode;
        String effective = ("THREE_CERT_AUTO".equals(userMode) || "FLEXIBLE_HYBRID".equals(userMode)) ? userMode : commMode;
        String mode = demoEnabled ? effective : "FLEXIBLE_HYBRID";
        List<ApplicationDocument> selectedDocuments = demoEnabled ? List.of()
                : documents.requireDocuments(user, req.get("document_ids"), rel);
        List<Map<String, Object>> attachments = demoEnabled ? attachments(req.get("attachments"))
                : selectedDocuments.stream().map(documents::describe).toList();
        if (attachments.isEmpty() && demoEnabled) {
            attachments.add(map("name", "居民身份证原件扫描件", "type", "ID_CARD", "desc", "权利人: " + user.realName + " (实人人脸比对核验通过)"));
            attachments.add(map("name", "不动产权证书原件扫描件", "type", "PROPERTY_CERT", "desc", "房屋坐落: " + propertyNo + " (市不动产中心联网比对通过)"));
            attachments.add(map("name", "机动车行驶证正副页扫描件", "type", "VEHICLE_LICENSE", "desc", "号牌号码: " + plate + " (公安交管比对通过)"));
            if (spouse) attachments.add(map("name", "夫妻关系结婚证明原件", "type", "MARRIAGE_CERT", "desc", "民政婚姻登记比对通过 (" + or(str(req.get("marriage_cert_no")), "J310104-2022-0089") + ")"));
            else if ("FAMILY".equals(rel)) attachments.add(map("name", "常住人口直系亲属居民户口簿", "type", "FAMILY_CERT", "desc", "常住人口户籍比对通过 (" + or(str(req.get("family_cert_no")), "310104-HK-982103") + ")"));
        }
        QueueApplication app = new QueueApplication();
        app.userId = user.id;
        app.communityId = user.communityId;
        app.propertyNo = propertyNo;
        app.plateNo = plate;
        app.spouseCar = spouse;
        app.marriageCertNo = spouse ? emptyToNull(str(req.get("marriage_cert_no"))) : null;
        app.carRelationship = rel;
        app.familyCertNo = "FAMILY".equals(rel) ? emptyToNull(str(req.get("family_cert_no"))) : null;
        app.priorityLevel = priority;
        app.status = "FLEXIBLE_HYBRID".equals(mode) ? "PENDING_REVIEW" : "WAITING";
        app.auditMode = mode;
        app.applyTime = Times.utcNow();
        try { app.attachmentsJson = json.writeValueAsString(attachments); } catch (Exception ignored) { app.attachmentsJson = "[]"; }
        app = apps.save(app);
        if (!demoEnabled) documents.link(selectedDocuments, app.id);
        String pri = priority == 1 ? "家庭一车" : "家庭二车";
        if ("FLEXIBLE_HYBRID".equals(mode)) {
            evidence.create(user.communityId, user.id, "APPLY_SUBMIT",
                    "业主【" + user.realName + "】提交车位申请，待平台管理员人工审核（" + relLabel + "·" + pri + "）",
                    Map.of("property_no", propertyNo, "plate_no", plate, "audit_mode", "FLEXIBLE_HYBRID", "attachments_count", attachments.size(), "priority", priority), null);
            saveNotice(user.id, "车位申请提交成功（待平台审核）", "您的申请已提交，平台管理员将人工核对房屋、车辆及身份证明后通知审核结果。", "SYSTEM", "GO_QUEUE");
            return map("id", app.id, "status", app.status, "audit_mode", app.auditMode, "_message", "申请已提交，请保留证明材料原件并等待平台人工审核。");
        }
        evidence.create(user.communityId, user.id, "APPLY",
                "业主【" + user.realName + "】提交车位申请(三证合一AI直审通过·" + relLabel + "·" + pri + ")",
                Map.of("property_no", propertyNo, "plate_no", plate, "audit_mode", "THREE_CERT_AUTO", "attachments_count", attachments.size(), "priority", priority), null);
        engine.recalculate(user.communityId);
        return map("id", app.id, "status", app.status, "audit_mode", app.auditMode, "_message", "三证合一核验通过，排队申请已成功提交并进入队列！");
    }

    @Transactional
    public String cancel(UserAccount user, String reason) {
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        QueueApplication app = apps.findByUserIdAndStatusInOrderByIdDesc(user.id, List.of("PENDING_REVIEW", "WAITING"))
                .stream().filter(a -> Objects.equals(a.communityId, user.communityId)).findFirst()
                .orElseThrow(() -> new ApiException(400, "未找到可撤销的待审核或排队申请，请刷新页面查看最新状态"));
        String previousStatus = app.status;
        boolean waiting = "WAITING".equals(previousStatus);
        int old = app.currentRank == null ? 0 : app.currentRank;
        app.status = "CANCELLED";
        app.currentRank = 0;
        apps.save(app);
        String summary = waiting
                ? "业主【" + user.realName + "】主动撤销第【" + old + "】位排队资格"
                : "业主【" + user.realName + "】主动撤销待平台审核的车位申请";
        evidence.create(user.communityId, user.id, "CANCEL", summary,
                Map.of("app_id", app.id, "previous_status", previousStatus,
                        "reason", reason == null || reason.isBlank() ? "个人原因自愿撤销" : reason,
                        "cancelled_rank", old), null);
        if (waiting) engine.recalculate(user.communityId);
        return waiting ? "排队申请已成功撤销，排位已释放" : "待审核申请已成功撤销，可修改资料后重新申请";
    }

    @Transactional
    public String accept(UserAccount user) {
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        QueueApplication app = apps.findFirstByUserIdAndStatus(user.id, "ALLOCATED").orElseThrow(() -> new ApiException(400, "未查询到待确认的中签申请"));
        app.status = "OFFLINE_PENDING";
        String spaceNo = "--";
        if (app.allocatedSpaceId != null) {
            ParkingSpace sp = spaces.findById(app.allocatedSpaceId).orElse(null);
            if (sp != null) { sp.status = "OFFLINE_PENDING"; spaceNo = sp.spaceNo; }
        }
        engine.notifyApplicants(user.communityId, "中签业主确认办理公告",
                "车位【" + spaceNo + "】的中签业主已确认前往线下办理，该车位进入签约流程。请继续关注您的排队顺位。",
                null, List.of(user.id), "SYSTEM", "GO_QUEUE");
        evidence.create(user.communityId, user.id, "OFFLINE_SIGN", "业主【" + user.realName + "】确认接收中签分配，进入线下签约办理流程",
                Map.of("app_id", app.id, "space_id", String.valueOf(app.allocatedSpaceId)), null);
        return "已确认接收！请于规定时间内前往物业办公室办理线下纸质合同签约与缴费";
    }

    @Transactional
    public String giveUp(UserAccount user) {
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        QueueApplication app = apps.findFirstByUserIdAndStatus(user.id, "ALLOCATED").orElseThrow(() -> new ApiException(400, "未查询到待确认的中签申请"));
        app.status = "CANCELLED";
        String spaceNo = "--";
        if (app.allocatedSpaceId != null) {
            ParkingSpace sp = spaces.findById(app.allocatedSpaceId).orElse(null);
            if (sp != null) { spaceNo = sp.spaceNo; sp.status = "VACANT"; sp.currentUserId = null; }
        }
        engine.notifyApplicants(user.communityId, "中签车位放弃释放公告",
                "车位【" + spaceNo + "】的中签业主已放弃本次机会，车位已释放回池。系统将按排队顺位自动顺延，请关注您的排位与中签通知。",
                null, List.of(user.id), "SYSTEM", "GO_QUEUE");
        evidence.create(user.communityId, user.id, "CANCEL", "业主【" + user.realName + "】主动放弃中签车位，车位释放自动顺延", Map.of("app_id", app.id), null);
        engine.recalculate(user.communityId);
        return "您已放弃本次车位分配，机会已顺延给下一位业主";
    }

    @Transactional
    public String terminate(UserAccount user) {
        if (!demoEnabled) throw new ApiException(403, "正式版退租请联系物业核对合同并办理");
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        RentalContract contract = contracts.findFirstByUserIdAndStatusIn(user.id, LIVE).orElseThrow(() -> new ApiException(400, "未查询到您名下正在生效的车位租约"));
        ParkingSpace sp = spaces.findById(contract.spaceId).orElse(null);
        contract.status = "TERMINATED";
        if (sp != null) { sp.status = "VACANT"; sp.currentUserId = null; }
        String spaceNo = sp != null ? sp.spaceNo : "--";
        evidence.create(user.communityId, user.id, "TERMINATE", "业主【" + user.realName + "】主动退租车位【" + spaceNo + "】，租约终止车位释放回炉",
                Map.of("contract_no", contract.contractNo, "space_no", spaceNo, "mode", "OWNER_INITIATED"), null);
        saveNotice(user.id, "退租办理完成通知", "您的车位【" + spaceNo + "】退租手续已办结，租约终止并已记入系统审计记录。车位将按排队顺位自动顺延分配。", "SYSTEM", null);
        engine.notifyApplicants(user.communityId, "车位退租释放公告", "车位【" + spaceNo + "】已退租释放。系统将按排队顺位自动分配，请关注您的排位与中签通知。", null, List.of(user.id), "SYSTEM", "GO_QUEUE");
        engine.recalculate(user.communityId);
        return "退租成功！车位【" + spaceNo + "】已释放，系统已自动顺延分配给排队顺位业主。";
    }

    @Transactional
    public Map<String, Object> renew(UserAccount user, Integer months) {
        if (!demoEnabled) throw new ApiException(403, "正式版续租请联系物业完成缴费与合同确认");
        communities.lockById(user.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        RentalContract contract = contracts.findFirstByUserIdAndStatusIn(user.id, LIVE).orElseThrow(() -> new ApiException(400, "未查询到您名下正在生效的车位租约，无法续租"));
        int daysLeft = (int) ChronoUnit.DAYS.between(LocalDate.now(), contract.endDate);
        if ("ACTIVE".equals(contract.status) && daysLeft > 30)
            throw new ApiException(400, "租约尚未进入续租期 (到期前 30 天内开放)，当前剩余 " + daysLeft + " 天");
        Term term = Term.of(months);
        LocalDate oldEnd = contract.endDate;
        contract.endDate = contract.endDate.plusDays(term.days);
        contract.status = "ACTIVE";
        ParkingSpace sp = spaces.findById(contract.spaceId).orElse(null);
        String spaceNo = sp != null ? sp.spaceNo : "--";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contract_no", contract.contractNo);
        payload.put("term", term.label);
        payload.put("months", term.months);
        payload.put("delta_days", term.days);
        payload.put("old_end_date", String.valueOf(oldEnd));
        payload.put("new_end_date", String.valueOf(contract.endDate));
        evidence.create(user.communityId, user.id, "RENEW", "业主【" + user.realName + "】完成车位【" + spaceNo + "】续租申请 (" + term.label + ")", payload, null);
        saveNotice(user.id, "车位续租成功通知", "您的车位【" + spaceNo + "】已成功办理续租，租约有效期已顺延 " + term.label + " 至 " + contract.endDate + "，本次续租已记入系统审计记录。", "SYSTEM", "GO_CONFIRM");
        return map("contract_no", contract.contractNo, "term", term.label, "new_end_date", String.valueOf(contract.endDate), "_message", "续租成功！租约已顺延 " + term.label + " 至 " + contract.endDate + "。");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> queue(UserAccount user) {
        List<QueueApplication> waiting = QueueEngine.orderedWaiting(apps.findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(user.communityId, "WAITING"));
        QueueApplication pendingLegacy = pendingLegacyQueue(user);
        List<Map<String, Object>> result = new ArrayList<>();
        int idx = 1;
        for (QueueApplication app : waiting) {
            UserAccount u = app.userId == null ? null : users.findById(app.userId).orElse(null);
            result.add(map("rank", idx, "masked_name", maskName(u == null ? app.legacyApplicantName : u.realName),
                    "masked_phone", maskPhone(u == null ? app.legacyApplicantPhone : u.phone),
                    "priority_level", app.priorityLevel, "priority_type", app.priorityLevel != null && app.priorityLevel == 1 ? "常规轮候" : "同户二车",
                    "apply_time", formatApplicationTime(app), "is_me", Objects.equals(app.userId, user.id) || Objects.equals(app.id, pendingLegacy == null ? null : pendingLegacy.id),
                    "legacy_imported", app.legacyImportBatchId != null));
            idx++;
        }
        List<Map<String, Object>> allocated = new ArrayList<>();
        for (QueueApplication app : apps.findByCommunityIdAndStatusInOrderByAllocatedTimeAsc(user.communityId, List.of("ALLOCATED", "OFFLINE_PENDING"))) {
            UserAccount u = app.userId == null ? null : users.findById(app.userId).orElse(null);
            ParkingSpace sp = app.allocatedSpaceId == null ? null : spaces.findById(app.allocatedSpaceId).orElse(null);
            allocated.add(map("masked_name", maskName(u), "masked_phone", maskPhone(u), "space_no", sp != null ? sp.spaceNo : "--",
                    "status", app.status, "status_label", "ALLOCATED".equals(app.status) ? "中签待确认(48h)" : "线下签约办理中",
                    "allocated_time", Times.fmtBj(app.allocatedTime), "is_me", Objects.equals(app.userId, user.id)));
        }
        return map("waiting", result, "allocated", allocated);
    }

    @Transactional
    public List<Map<String, Object>> timeline(UserAccount user) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Notification n : notices.findByUserIdOrderByCreatedAtDesc(user.id)) {
            list.add(map("id", n.id, "title", n.title, "content", n.content, "type", n.type, "time", Times.fmtBj(n.createdAt)));
        }
        return list;
    }

    @Transactional
    public List<Map<String, Object>> notifications(UserAccount user) {
        List<Notification> all = notices.findByUserIdOrderByCreatedAtDesc(user.id);
        all.forEach(n -> n.readFlag = true);
        List<Map<String, Object>> data = new ArrayList<>();
        for (Notification n : all) {
            data.add(map("id", n.id, "title", n.title, "content", n.content, "type", n.type, "action_type", n.actionType, "created_at", Times.fmtBj(n.createdAt)));
        }
        return data;
    }

    private Map<String, Object> queueView(QueueApplication app, UserAccount user, Community community) {
        List<Map<String, Object>> attachments = parse(app.attachmentsJson);
        if ("PENDING_REVIEW".equals(app.status)) {
            return map("id", app.id, "property_no", app.propertyNo, "plate_no", app.plateNo, "status", "PENDING_REVIEW",
                    "status_label", "待平台人工审核", "audit_mode", or(app.auditMode, "FLEXIBLE_HYBRID"), "apply_time", formatApplicationTime(app),
                    "attachments", attachments, "notice", "申请已提交，正在等待平台管理员人工核验材料。");
        }
        if ("REJECTED".equals(app.status)) {
            return map("id", app.id, "property_no", app.propertyNo, "plate_no", app.plateNo, "status", "REJECTED",
                    "status_label", "审核已驳回", "reject_reason", or(app.rejectReason, "申请材料不符合要求，请核对后重新提交"),
                    "audit_time", app.auditTime == null ? "--" : Times.fmtBj(app.auditTime), "audit_mode", or(app.auditMode, "FLEXIBLE_HYBRID"),
                    "apply_time", formatApplicationTime(app), "attachments", attachments, "can_reapply", true);
        }
        String allocatedNo = null;
        int countdown = 0;
        if (("ALLOCATED".equals(app.status) || "OFFLINE_PENDING".equals(app.status)) && app.allocatedSpaceId != null) {
            ParkingSpace sp = spaces.findById(app.allocatedSpaceId).orElse(null);
            allocatedNo = sp != null ? sp.spaceNo : "待定";
            if (app.allocatedTime != null) {
                long elapsed = ChronoUnit.SECONDS.between(app.allocatedTime, Times.utcNow());
                countdown = (int) Math.max(0, 48 * 3600 - elapsed);
            }
        }
        int totalWaiting = (int) apps.countByCommunityIdAndStatus(user.communityId, "WAITING");
        int totalSpaces = community != null && community.totalSpaces != null ? community.totalSpaces : 50;
        String[] est = QueueEngine.estimate(app.currentRank == null ? 0 : app.currentRank, totalSpaces);
        boolean identityPending = app.userId == null && app.legacyImportBatchId != null;
        if (identityPending) est[1] = "历史排队记录已保留，身份资料核实后才能参与车位分配。";
        return map("id", app.id, "property_no", app.propertyNo, "plate_no", app.plateNo, "priority_level", app.priorityLevel,
                "priority_label", app.priorityLevel != null && app.priorityLevel == 1 ? "常规优先轮候" : "同户二车(顺延后置)",
                "current_rank", app.currentRank, "total_queue", totalWaiting, "is_first_candidate", !identityPending && app.currentRank != null && app.currentRank == 1,
                "identity_pending", identityPending, "can_cancel", !identityPending,
                "apply_time", formatApplicationTime(app), "ai_waiting_time", est[0], "ai_estimation_basis", est[1],
                "allocated_space_no", allocatedNo, "countdown_seconds", countdown, "attachments", attachments,
                "audit_mode", or(app.auditMode, "THREE_CERT_AUTO"));
    }

    private Map<String, Object> maskSpace(ParkingSpace sp) {
        String owner = "空闲", room = "--", plate = "--", end = "--";
        if (sp.currentUserId != null) {
            UserAccount u = users.findById(sp.currentUserId).orElse(null);
            if (u != null && u.realName != null && !u.realName.isEmpty()) owner = u.realName.charAt(0) + "**";
            RentalContract ct = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
            if (ct != null) {
                plate = ct.plateNo != null && ct.plateNo.length() >= 5 ? ct.plateNo.substring(0, 2) + "·***" + ct.plateNo.substring(ct.plateNo.length() - 2) : ct.plateNo;
                end = String.valueOf(ct.endDate);
                room = "--";
            }
        } else if ("RENTED".equals(sp.status)) {
            RentalContract legacy = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
            owner = legacy != null && legacy.legacyTenantName != null && !legacy.legacyTenantName.isBlank()
                    ? legacy.legacyTenantName.charAt(0) + "**" : "已租";
            if (legacy != null) {
                plate = legacy.plateNo != null && legacy.plateNo.length() >= 5
                        ? legacy.plateNo.substring(0, 2) + "·***" + legacy.plateNo.substring(legacy.plateNo.length() - 2)
                        : "**";
                end = legacy.endDate == null ? "**" : legacy.endDate.toString();
            }
        } else if ("MAINTENANCE".equals(sp.status)) owner = "维修停用";
        return map("space_no", sp.spaceNo, "area_type", "GROUND".equals(sp.areaType) ? "地面" : "UNDERGROUND".equals(sp.areaType) ? "地下" : "**", "status", sp.status,
                "owner_name", owner, "room_no", room, "plate_no", plate, "end_date", end);
    }

    private void saveNotice(Integer uid, String title, String content, String type, String action) {
        Notification n = new Notification();
        n.userId = uid;
        n.title = title;
        n.content = content;
        n.type = type;
        n.actionType = action;
        n.createdAt = Times.utcNow();
        notices.save(n);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> attachments(Object raw) {
        if (!(raw instanceof List<?> list)) return new ArrayList<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) if (o instanceof Map<?, ?> m) out.add(new LinkedHashMap<>((Map<String, Object>) m));
        return out;
    }

    private List<Map<String, Object>> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try { return json.readValue(raw, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    public static String formatApplicationTime(QueueApplication app) {
        if (app.legacyImportBatchId != null && app.legacyApplyTimeText != null && !app.legacyApplyTimeText.isBlank())
            return app.legacyApplyTimeText;
        return Times.fmtBj(app.applyTime);
    }

    private QueueApplication pendingLegacyQueue(UserAccount user) {
        if (!"OWNER".equals(user.role) || user.idCard != null || user.phone == null || user.communityId == null) return null;
        return apps.findByCommunityIdAndLegacyApplicantPhoneAndUserIdIsNullAndStatus(user.communityId, user.phone, "WAITING")
                .stream().filter(app -> app.legacyImportBatchId != null && Objects.equals(app.legacyApplicantName, user.realName))
                .findFirst().orElse(null);
    }

    private static String maskName(UserAccount u) { return maskName(u == null ? null : u.realName); }
    private static String maskName(String name) { return name == null || name.isBlank() || "**".equals(name) ? "**" : name.charAt(0) + "**"; }
    private static String maskPhone(UserAccount u) { return maskPhone(u == null ? null : u.phone); }
    private static String maskPhone(String phone) { return phone == null || phone.length() < 7 ? "**" : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4); }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String or(String a, String b) { return a == null || a.isBlank() ? b : a; }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    static final class Term {
        final int months, days;
        final String label;
        Term(int months, int days, String label) { this.months = months; this.days = days; this.label = label; }
        static Term of(Integer months) {
            int m = months == null || months <= 0 ? 6 : months;
            return switch (m) {
                case 12 -> new Term(12, 365, "1 年 (12个月)");
                case 6 -> new Term(6, 180, "半年 (6个月)");
                case 3 -> new Term(3, 90, "一季度 (3个月)");
                case 1 -> new Term(1, 30, "1 个月");
                default -> new Term(m, m * 30, m + " 个月");
            };
        }
    }
}
