package com.huiting.parking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.common.ApiException;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.*;
import com.huiting.parking.repo.Repos;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PropertyService {
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;
    private static final List<String> LIVE = List.of("ACTIVE", "EXPIRING", "OVERDUE");
    private final Repos.UserRepo users;
    private final Repos.CommunityRepo communities;
    private final Repos.SpaceRepo spaces;
    private final Repos.AppRepo apps;
    private final Repos.ContractRepo contracts;
    private final Repos.EvidenceRepo evidences;
    private final Repos.NoticeRepo notices;
    private final QueueEngine engine;
    private final EvidenceService evidence;
    private final ObjectMapper json;

    public PropertyService(Repos.UserRepo users, Repos.CommunityRepo communities, Repos.SpaceRepo spaces, Repos.AppRepo apps,
                           Repos.ContractRepo contracts, Repos.EvidenceRepo evidences, Repos.NoticeRepo notices,
                           QueueEngine engine, EvidenceService evidence, ObjectMapper json) {
        this.users = users;
        this.communities = communities;
        this.spaces = spaces;
        this.apps = apps;
        this.contracts = contracts;
        this.evidences = evidences;
        this.notices = notices;
        this.engine = engine;
        this.evidence = evidence;
        this.json = json;
    }

    public UserAccount requireAdmin(Integer id) {
        if (id == null) throw new ApiException(403, "仅限物业管理员权限访问");
        UserAccount u = users.findById(id).orElseThrow(() -> new ApiException(403, "仅限物业管理员权限访问"));
        if (!"PROPERTY_ADMIN".equals(u.role)) throw new ApiException(403, "仅限物业管理员权限访问");
        return u;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(UserAccount admin) {
        Integer cid = admin.communityId;
        Community comm = communities.findById(cid).orElse(null);
        long total = spaces.countByCommunityId(cid);
        long rented = spaces.countByCommunityIdAndStatus(cid, "RENTED");
        long vacant = spaces.countByCommunityIdAndStatus(cid, "VACANT");
        long maint = spaces.countByCommunityIdAndStatus(cid, "MAINTENANCE");
        long allocated = spaces.countByCommunityIdAndStatusIn(cid, List.of("ALLOCATED", "OFFLINE_PENDING"));
        long waiting = apps.countByCommunityIdAndStatus(cid, "WAITING");
        long pendingAudit = apps.countByCommunityIdAndStatus(cid, "PENDING_REVIEW");
        Set<Integer> spaceIds = new HashSet<>();
        spaces.findByCommunityIdOrderBySpaceNoAsc(cid).forEach(s -> spaceIds.add(s.id));
        List<RentalContract> communityContracts = contracts.findAll().stream().filter(c -> spaceIds.contains(c.spaceId)).toList();
        List<RentalContract> terminated = communityContracts.stream()
                .filter(c -> "TERMINATED".equals(c.status) && c.startDate != null && c.endDate != null).toList();
        int avgTurn = terminated.isEmpty() ? 0 : (int) Math.round(terminated.stream().mapToLong(c -> ChronoUnit.DAYS.between(c.startDate, c.endDate)).average().orElse(0));
        List<QueueApplication> waitingApps = QueueEngine.orderedWaiting(apps.findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(cid, "WAITING"));
        int avgWait = (int) Math.round(waitingApps.stream().filter(a -> a.applyTime != null).mapToLong(a -> ChronoUnit.DAYS.between(a.applyTime.toLocalDate(), Times.utcNow().toLocalDate())).average().orElse(0));
        long legacyQueueUnlinked = waitingApps.stream().filter(a -> a.legacyImportBatchId != null && a.userId == null).count();
        long expiring = communityContracts.stream().filter(c -> "EXPIRING".equals(c.status)).count();
        long legacyCount = communityContracts.stream().filter(c -> c.legacyImportBatchId != null).count();
        boolean inventoryComplete = comm == null || !Boolean.FALSE.equals(comm.inventoryComplete);
        long pendingDelivery = apps.countByCommunityIdAndStatus(cid, "OFFLINE_PENDING");
        String mode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        return ParkingService.map(
                "total_spaces", total, "rented_spaces", rented, "vacant_spaces", vacant, "maintenance_spaces", maint,
                "allocated_spaces", allocated, "waiting_count", waiting, "pending_audit_count", pendingAudit,
                "occupancy_rate", inventoryComplete ? String.format("%.1f%%", rented * 100.0 / Math.max(1, total)) : "**",
                "inventory_complete", inventoryComplete,
                "legacy_import_count", legacyCount,
                "legacy_queue_unlinked_count", legacyQueueUnlinked,
                "allocation_awaits_identity", !waitingApps.isEmpty() && waitingApps.get(0).userId == null,
                "inventory_note", inventoryComplete ? "" : "已导入在租车位台账；空闲及维修车位清单尚待补录，占用率暂不可计算。",
                "pending_warnings", expiring + pendingDelivery + pendingAudit,
                "avg_turnover_days", avgTurn, "avg_queue_wait_days", avgWait,
                "completed_allocations", apps.countByCommunityIdAndStatus(cid, "COMPLETED"),
                "released_total", evidences.countByCommunityIdAndEventType(cid, "TERMINATE"),
                "audit_mode", mode,
                "source_audit_mode", comm == null ? null : comm.sourceAuditMode,
                "district", comm == null ? null : comm.district, "subdistrict", comm == null ? null : comm.subdistrict,
                "company_name", comm == null ? null : comm.companyName,
                "contact_name", comm == null ? null : comm.contactName, "contact_phone", comm == null ? null : comm.contactPhone,
                "audit_mode_label", demoEnabled && "THREE_CERT_AUTO".equals(mode) ? "三证合一（演示自动审核）" : "物业人工审核");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> spaceList(UserAccount admin) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ParkingSpace sp : spaces.findByCommunityIdOrderBySpaceNoAsc(admin.communityId)) {
            String name = "--", phone = "--", plate = "--", end = "--";
            RentalContract ct = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
            if (sp.currentUserId != null) {
                UserAccount u = users.findById(sp.currentUserId).orElse(null);
                if (u != null) { name = u.realName; phone = u.phone; }
                if (ct == null) {
                    QueueApplication app = apps.findFirstByAllocatedSpaceId(sp.id).orElse(null);
                    if (app != null) { plate = app.plateNo; end = "48h待签约"; }
                }
            }
            if (ct != null) {
                plate = ct.plateNo;
                end = ct.endDate == null ? "**" : ct.endDate.toString();
                if (ct.legacyImportBatchId != null && sp.currentUserId == null) {
                    name = legacyValue(ct.legacyTenantName);
                    phone = legacyValue(ct.legacyTenantPhone);
                }
            }
            result.add(ParkingService.map("id", sp.id, "space_no", sp.spaceNo, "area_type", sp.areaType, "status", sp.status,
                    "user_id", sp.currentUserId, "user_name", name, "user_phone", phone, "plate_no", plate, "end_date", end,
                    "legacy_unverified", ct != null && ct.legacyImportBatchId != null && ct.userId == null,
                    "property_no", ct != null && ct.legacyImportBatchId != null ? legacyValue(ct.legacyPropertyNo) : "--"));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> addSpace(UserAccount admin, Map<String, Object> req) {
        communities.lockById(admin.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        String spaceNo = str(req.get("space_no")).trim().toUpperCase(Locale.ROOT);
        String areaType = str(req.get("area_type")).trim().toUpperCase(Locale.ROOT);
        if (!spaceNo.matches("[A-Z0-9-]{1,32}")) throw new ApiException(400, "车位编号仅支持字母、数字和连字符，最长32位");
        if (!"GROUND".equals(areaType) && !"UNDERGROUND".equals(areaType)) throw new ApiException(400, "请选择地面或地下车位");
        if (spaces.existsByCommunityIdAndSpaceNo(admin.communityId, spaceNo)) throw new ApiException(400, "本小区已存在该车位编号");
        ParkingSpace sp = new ParkingSpace();
        sp.communityId = admin.communityId;
        sp.spaceNo = spaceNo;
        sp.areaType = areaType;
        sp.status = "VACANT";
        sp = spaces.save(sp);
        communities.findById(admin.communityId).ifPresent(c -> c.totalSpaces = (int) spaces.countByCommunityId(admin.communityId));
        evidence.create(admin.communityId, admin.id, "SPACE_CREATE", "物业录入真实车位【" + spaceNo + "】",
                Map.of("space_no", spaceNo, "area_type", areaType), null);
        engine.recalculate(admin.communityId);
        return ParkingService.map("id", sp.id, "space_no", sp.spaceNo, "area_type", sp.areaType, "status", sp.status);
    }

    @Transactional
    public Map<String, Object> maintenance(UserAccount admin, Map<String, Object> req) {
        communities.lockById(admin.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        int spaceId = num(req.get("space_id"));
        ParkingSpace sp = spaces.findById(spaceId)
                .filter(s -> Objects.equals(s.communityId, admin.communityId))
                .orElseThrow(() -> new ApiException(404, "未找到本小区的指定车位"));
        String action = str(req.get("action"));
        String reason = or(str(req.get("reason")), "地砖检修与设施保养");
        String attachment = emptyToNull(str(req.get("attachment_url")));
        Integer affected = sp.currentUserId;
        String msg;
        if ("SET_MAINTENANCE".equals(action)) {
            String orig = sp.status;
            if ("ALLOCATED".equals(sp.status) || "OFFLINE_PENDING".equals(sp.status)) {
                QueueApplication active = apps.findFirstByAllocatedSpaceId(sp.id).orElse(null);
                if (active != null) {
                    affected = active.userId;
                    active.status = "WAITING";
                    active.allocatedSpaceId = null;
                    active.allocatedTime = null;
                    saveNotice(active.userId, "中签车位转入维修通知", "您中签的车位【" + sp.spaceNo + "】已由物业置为维修停用（原因: " + reason + "），本次分配已退回排队，系统将为您保留排队资格。", "SYSTEM", "GO_QUEUE");
                }
            } else if ("RENTED".equals(sp.status)) {
                RentalContract contract = contracts.findFirstBySpaceIdAndStatusIn(sp.id, LIVE).orElse(null);
                if (contract != null) {
                    affected = contract.userId;
                    contract.status = "TERMINATED";
                    if (contract.userId != null)
                        saveNotice(contract.userId, "承租车位维修停用通知", "您承租的车位【" + sp.spaceNo + "】已由物业置为维修停用（原因: " + reason + "），原租约已终止并记入系统审计记录。", "SYSTEM", null);
                }
            }
            sp.status = "MAINTENANCE";
            sp.currentUserId = null;
            msg = "车位【" + sp.spaceNo + "】已直接置为维修停用状态（原状态: " + orig + "，原因: " + reason + "），已移出自动排位池";
            engine.notifyApplicants(admin.communityId, "车位维修停用公告", "车位【" + sp.spaceNo + "】已置为维修停用（原因: " + reason + "），暂不参与自动分配。您的排队资格保留，请继续关注顺位变化。",
                    null, affected == null ? List.of() : List.of(affected), "SYSTEM", "GO_QUEUE");
        } else if ("RESTORE".equals(action)) {
            sp.status = "VACANT";
            sp.currentUserId = null;
            msg = "车位【" + sp.spaceNo + "】检修验收完毕，已恢复可用并重新纳入自动排位调度池！";
            engine.notifyApplicants(admin.communityId, "车位恢复可用公告", "车位【" + sp.spaceNo + "】已验收恢复可用，系统将按排队顺位自动分配。请关注您的排位与中签通知。", null, null, "SYSTEM", "GO_QUEUE");
        } else throw new ApiException(400, "无效的操作类型");
        engine.recalculate(admin.communityId);
        String summary = "SET_MAINTENANCE".equals(action)
                ? "物业管理员操作车位【" + sp.spaceNo + "】直接进入维修状态 (原因: " + reason + ")"
                : "物业验收车位【" + sp.spaceNo + "】维修完毕，恢复正常投入使用";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("space_no", sp.spaceNo);
        payload.put("action", action);
        payload.put("reason", reason);
        payload.put("operator", admin.realName);
        payload.put("attachment", attachment);
        evidence.create(admin.communityId, admin.id, "SET_MAINTENANCE".equals(action) ? "MAINTENANCE" : "RESTORE", summary, payload, null);
        return ParkingService.map("space_id", sp.id, "space_no", sp.spaceNo, "status", sp.status, "_message", msg);
    }

    @Transactional
    public String confirmDelivery(UserAccount admin, Map<String, Object> req) {
        communities.lockById(admin.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        String receiptNo = str(req.get("receipt_no")).trim();
        if (!demoEnabled && (!Boolean.TRUE.equals(req.get("payment_confirmed")) || receiptNo.isBlank()))
            throw new ApiException(400, "请先核对线下合同与缴费凭据，并填写缴费凭据编号");
        ParkingSpace sp = spaces.findById(num(req.get("space_id")))
                .filter(s -> Objects.equals(s.communityId, admin.communityId)).orElse(null);
        UserAccount u = users.findById(num(req.get("user_id")))
                .filter(candidate -> Objects.equals(candidate.communityId, admin.communityId)).orElse(null);
        if (sp == null || u == null) throw new ApiException(404, "车位或业主不存在");
        if (!("ALLOCATED".equals(sp.status) || "OFFLINE_PENDING".equals(sp.status)) || !Objects.equals(sp.currentUserId, u.id))
            throw new ApiException(400, "该车位当前并非分配给此业主的中签车位，禁止人工指定交付！车位只能由系统按排队顺位自动分发。");
        QueueApplication app = apps.findFirstByAllocatedSpaceIdAndStatusIn(sp.id, List.of("ALLOCATED", "OFFLINE_PENDING"))
                .filter(a -> a.userId.equals(u.id)).orElseThrow(() -> new ApiException(400, "未找到该业主对应该车位的中签申请单，无法交付"));
        if (contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").isPresent())
            throw new ApiException(400, "该车位已存在生效租约，不可重复交付");
        sp.status = "RENTED";
        sp.currentUserId = u.id;
        app.status = "COMPLETED";
        ParkingService.Term term = ParkingService.Term.of(req.get("months") == null ? null : num(req.get("months")));
        LocalDate today = LocalDate.now();
        String no = "AGY-CT-" + today.toString().replace("-", "") + "-" + sp.spaceNo.replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        RentalContract contract = new RentalContract();
        contract.contractNo = no;
        contract.spaceId = sp.id;
        contract.userId = u.id;
        contract.plateNo = app.plateNo;
        contract.paymentReceiptNo = receiptNo.isBlank() ? null : receiptNo;
        contract.startDate = today;
        contract.endDate = today.plusDays(term.days);
        contract.status = "ACTIVE";
        contract.createdAt = Times.utcNow();
        contracts.save(contract);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contract_no", no);
        payload.put("space_no", sp.spaceNo);
        payload.put("term", term.label);
        payload.put("months", term.months);
        payload.put("start_date", today.toString());
        payload.put("end_date", contract.endDate.toString());
        payload.put("receipt_no", receiptNo);
        payload.put("attachment", emptyToNull(str(req.get("attachment_url"))));
        evidence.create(admin.communityId, u.id, "SIGN", "物业与业主【" + u.realName + "】完成车位【" + sp.spaceNo + "】线下签约履约交付 (" + term.label + ")", payload, null);
        saveNotice(u.id, "车位签约成功生效通知", "恭喜！您与物业已完成车位【" + sp.spaceNo + "】的线下签约缴费，租期 " + term.label + " 已正式生效（有效期至 " + contract.endDate + "）！", "SYSTEM", "GO_CONFIRM");
        engine.notifyApplicants(admin.communityId, "车位签约生效公告", "车位【" + sp.spaceNo + "】已完成线下签约并正式出租，本轮不再进入分配池。请继续关注后续释放与您的排队顺位。", null, List.of(u.id), "SYSTEM", "GO_QUEUE");
        return "车位【" + sp.spaceNo + "】已成功交付业主【" + u.realName + "】，正式租约已生效！";
    }

    @Transactional
    public String forceTerminate(UserAccount admin, Map<String, Object> req) {
        communities.lockById(admin.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        ParkingSpace sp = spaces.findById(num(req.get("space_id")))
                .filter(s -> Objects.equals(s.communityId, admin.communityId))
                .orElseThrow(() -> new ApiException(404, "未找到本小区的指定车位"));
        if ("VACANT".equals(sp.status) || "MAINTENANCE".equals(sp.status)) throw new ApiException(400, "该车位当前未被租用占用，无需清退");
        String reason = str(req.get("reason"));
        String reference = str(req.get("evidence_reference")).trim();
        if (reason.isBlank() || (!demoEnabled && reference.isBlank()))
            throw new ApiException(400, "请填写清退事由及线下办理凭据编号");
        Integer oldUserId = sp.currentUserId;
        String oldName = "老业主";
        if (oldUserId != null) oldName = users.findById(oldUserId).map(u -> u.realName).orElse(oldName);
        else {
            RentalContract legacy = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
            if (legacy != null && legacy.legacyImportBatchId != null) oldName = legacyValue(legacy.legacyTenantName);
        }
        QueueApplication pending = apps.findFirstByAllocatedSpaceIdAndStatusIn(sp.id, List.of("ALLOCATED", "OFFLINE_PENDING")).orElse(null);
        if (pending != null) {
            pending.status = "CANCELLED";
            pending.allocatedSpaceId = null;
            pending.allocatedTime = null;
            saveNotice(pending.userId, "中签分配撤销通知", "很抱歉，您中签的车位【" + sp.spaceNo + "】因物业原因(" + reason + ")被收回，本次分配已作废。如需车位请重新提交排队申请。", "SYSTEM", "GO_QUEUE");
        }
        contracts.findBySpaceIdAndStatusNot(sp.id, "TERMINATED").forEach(c -> c.status = "TERMINATED");
        if (oldUserId != null && (pending == null || !oldUserId.equals(pending.userId))) {
            saveNotice(oldUserId, "车位强制清退通知", "您名下的车位【" + sp.spaceNo + "】已由物业清退（事由: " + reason + "），租约终止并已记入系统审计记录。", "SYSTEM", null);
        }
        sp.status = "VACANT";
        sp.currentUserId = null;
        List<Integer> exclude = new ArrayList<>();
        if (oldUserId != null) exclude.add(oldUserId);
        if (pending != null) exclude.add(pending.userId);
        engine.notifyApplicants(admin.communityId, "车位清退释放公告", "车位【" + sp.spaceNo + "】已由物业强制清退释放（事由: " + reason + "）。系统将按排队规则自动顺延分配，请关注您的排位与中签通知。", null, exclude, "SYSTEM", "GO_QUEUE");
        evidence.create(admin.communityId, oldUserId, "TERMINATE", "物业代清退车位【" + sp.spaceNo + "】(原业主:" + oldName + ", 事由:" + reason + ")",
                Map.of("space_no", sp.spaceNo, "old_user", oldName, "reason", reason, "evidence_reference", reference), null);
        engine.recalculate(admin.communityId);
        return "车位【" + sp.spaceNo + "】已成功清退释放，系统已自动触发后序顺位流转分配！";
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(UserAccount admin, Integer spaceId) {
        ParkingSpace sp = spaces.findById(spaceId).filter(s -> s.communityId.equals(admin.communityId)).orElseThrow(() -> new ApiException(404, "未找到指定车位"));
        Map<String, String> labels = Map.of("ACTIVE", "生效中", "EXPIRING", "临期", "OVERDUE", "逾期", "TERMINATED", "已退租");
        Map<String, Object> current = null;
        RentalContract ct = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
        if (ct != null) {
            current = ParkingService.map("user_name", contractName(ct), "phone", contractPhone(ct),
                    "plate_no", ct.plateNo, "start_date", contractStart(ct), "end_date", contractEnd(ct),
                    "status", ct.status, "property_no", ct.legacyImportBatchId == null ? "--" : legacyValue(ct.legacyPropertyNo),
                    "id_card", ct.legacyImportBatchId == null ? "--" : legacyValue(ct.legacyIdCard),
                    "legacy_unverified", ct.legacyImportBatchId != null && ct.userId == null);
        }
        List<Map<String, Object>> history = new ArrayList<>();
        for (RentalContract c : contracts.findBySpaceIdOrderByIdDesc(sp.id)) {
            history.add(ParkingService.map("contract_no", contractDisplayNo(c), "user_name", contractName(c), "phone", contractPhone(c),
                    "plate_no", c.plateNo, "start_date", contractStart(c), "end_date", contractEnd(c), "status", c.status,
                    "status_label", labels.getOrDefault(c.status, c.status), "legacy_unverified", c.legacyImportBatchId != null && c.userId == null));
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (ChainEvidence e : evidences.findTop10ByCommunityIdAndRawPayloadJsonContainingOrderByIdDesc(admin.communityId, sp.spaceNo)) {
            String hash = e.txHash == null ? "" : e.txHash;
            events.add(ParkingService.map("time", Times.fmtBj(e.createdAt), "summary", e.eventSummary, "tx_hash", hash.substring(0, Math.min(18, hash.length())) + "..."));
        }
        return ParkingService.map("space_no", sp.spaceNo, "status", sp.status, "current", current, "history", history, "events", events);
    }

    @Transactional(readOnly = true)
    public byte[] export(UserAccount admin) {
        Community community = communities.findById(admin.communityId).orElse(null);
        Map<String, String> statusMap = Map.of("RENTED", "已租(生效中)", "VACANT", "空闲可用", "ALLOCATED", "已分配待确认(48h)", "OFFLINE_PENDING", "线下签约办理中", "MAINTENANCE", "维修停用");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle head = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            head.setFont(font);
            head.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Sheet ws = wb.createSheet("车位资产台账");
            row(ws, head, "车位编号", "区域类型", "车位状态", "关联业主", "联系电话", "登记车牌", "租约编号", "租期截止日");
            for (ParkingSpace sp : spaces.findByCommunityIdOrderBySpaceNoAsc(admin.communityId)) {
                String name = "--", phone = "--", plate = "--", no = "--", end = "--";
                RentalContract ct = contracts.findFirstBySpaceIdAndStatusNot(sp.id, "TERMINATED").orElse(null);
                if (ct != null) {
                    name = contractName(ct);
                    phone = contractPhone(ct);
                    plate = ct.plateNo;
                    no = contractDisplayNo(ct);
                    end = contractEnd(ct);
                } else if (sp.currentUserId != null) {
                    UserAccount u = users.findById(sp.currentUserId).orElse(null);
                    if (u != null) { name = u.realName; phone = u.phone; }
                    QueueApplication app = apps.findFirstByAllocatedSpaceIdAndStatusIn(sp.id, List.of("ALLOCATED", "OFFLINE_PENDING")).orElse(null);
                    if (app != null) { plate = app.plateNo; end = "48h待签约"; }
                }
                row(ws, null, sp.spaceNo, areaLabel(sp.areaType), statusMap.getOrDefault(sp.status, sp.status), name, phone, plate, no, end);
            }
            Sheet ws2 = wb.createSheet("排队轮候名单");
            row(ws2, head, "实时排位", "业主姓名", "联系电话", "登记车牌", "优先级", "申报房屋", "申请时间");
            int idx = 1;
            for (QueueApplication app : QueueEngine.orderedWaiting(apps.findByCommunityIdAndStatusOrderByPriorityLevelAscApplyTimeAsc(admin.communityId, "WAITING"))) {
                UserAccount u = app.userId == null ? null : users.findById(app.userId).orElse(null);
                row(ws2, null, idx++, u != null ? u.realName : legacyValue(app.legacyApplicantName),
                        u != null ? u.phone : legacyValue(app.legacyApplicantPhone), app.plateNo,
                        app.priorityLevel != null && app.priorityLevel == 1 ? "家庭一车" : "家庭二车", app.propertyNo, ParkingService.formatApplicationTime(app));
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "导出台账失败");
        }
    }

    public String exportName(UserAccount admin) {
        Community community = communities.findById(admin.communityId).orElse(null);
        return (community != null ? community.name : "小区") + "_车位资产台账_" + LocalDate.now().toString().replace("-", "") + ".xlsx";
    }

    @Transactional
    public String initSpaces(UserAccount admin) {
        if (!demoEnabled) throw new ApiException(403, "正式版请逐一录入真实车位，禁止注入演示车位");
        if (spaces.countByCommunityId(admin.communityId) > 0) throw new ApiException(400, "当前小区已存在车位台账，无需重复初始化");
        List<ParkingSpace> list = engine.initSpaces(admin.communityId, 50);
        communities.findById(admin.communityId).ifPresent(c -> c.totalSpaces = 50);
        evidence.create(admin.communityId, admin.id, "INIT", "物业管理员【" + admin.realName + "】完成本小区 50 个车位台账初始化导入锁定", Map.of("total_spaces", 50, "manager", admin.realName), null);
        return "已成功注入 50 个车位资产 (" + list.get(0).spaceNo + " ~ " + list.get(list.size() - 1).spaceNo + ")，台账已写入本地审计链。";
    }

    @Transactional(readOnly = true)
    public Map<String, Object> auditConfig(UserAccount admin) {
        Community comm = communities.findById(admin.communityId).orElse(null);
        String mode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        return ParkingService.map("community_id", comm != null ? comm.id : admin.communityId, "community_name", comm != null ? comm.name : "龙华新村",
                "audit_mode", mode, "mode_label", label(mode));
    }

    @Transactional
    public Map<String, Object> updateAudit(UserAccount admin, String mode) {
        if (!demoEnabled) throw new ApiException(403, "正式版统一采用物业人工审核");
        if (!"THREE_CERT_AUTO".equals(mode) && !"FLEXIBLE_HYBRID".equals(mode)) throw new ApiException(400, "无效的审核方式，仅支持 THREE_CERT_AUTO 或 FLEXIBLE_HYBRID");
        Community comm = communities.findById(admin.communityId).orElseThrow(() -> new ApiException(404, "未找到所属小区"));
        String old = comm.auditMode == null ? "THREE_CERT_AUTO" : comm.auditMode;
        comm.auditMode = mode;
        String modeLabel = label(mode);
        evidence.create(admin.communityId, admin.id, "AUDIT_CONFIG", "物业管理员【" + admin.realName + "】变更小区审核方式为【" + modeLabel + "】(仅对新申请生效，存量申请保持原样)",
                Map.of("old_mode", old, "new_mode", mode, "mode_label", modeLabel, "operator", admin.realName), null);
        return ParkingService.map("audit_mode", comm.auditMode, "mode_label", modeLabel, "_message", "小区审核方式已更新为【" + modeLabel + "】（仅对新申请生效，存量申请保持原有模式不变）");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> communityUsers(UserAccount admin) {
        Community comm = communities.findById(admin.communityId).orElse(null);
        String commMode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAccount u : users.findByCommunityIdAndRoleInOrderByIdAsc(admin.communityId, List.of("OWNER", "FAMILY"))) {
            boolean custom = "THREE_CERT_AUTO".equals(u.auditMode) || "FLEXIBLE_HYBRID".equals(u.auditMode);
            String effective = custom ? u.auditMode : commMode;
            boolean activeApp = apps.findFirstByUserIdAndStatusIn(u.id, List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING", "PENDING_REVIEW")).isPresent();
            boolean activeCt = contracts.findFirstByUserIdAndStatusIn(u.id, LIVE).isPresent();
            String tag = activeCt ? "已签约租用" : (activeApp ? "排队/审核中" : "未发起申请");
            String propertyNo = "--";
            QueueApplication cur = apps.findFirstByUserIdAndStatusIn(u.id, List.of("WAITING", "ALLOCATED", "OFFLINE_PENDING", "PENDING_REVIEW")).orElse(null);
            if (cur != null && cur.propertyNo != null) propertyNo = cur.propertyNo;
            else {
                List<QueueApplication> past = apps.findByUserIdOrderByIdDesc(u.id);
                if (!past.isEmpty() && past.get(0).propertyNo != null) propertyNo = past.get(0).propertyNo;
                else if (demoEnabled && "张欣欣".equals(u.realName)) propertyNo = "12号楼302室";
                else if (demoEnabled && "李雷".equals(u.realName)) propertyNo = "10号楼201室";
                else if (demoEnabled && "韩梅梅".equals(u.realName)) propertyNo = "8号楼201室";
                else if (demoEnabled && "王建国".equals(u.realName)) propertyNo = "9号楼102室";
            }
            String masked = u.idCard != null && u.idCard.length() >= 10 ? u.idCard.substring(0, 6) + "********" + u.idCard.substring(u.idCard.length() - 4) : "--";
            result.add(ParkingService.map("id", u.id, "real_name", u.realName, "phone", u.phone, "property_no", propertyNo, "id_card_masked", masked,
                    "role", u.role, "user_audit_mode", u.auditMode, "effective_audit_mode", effective, "is_customized", custom, "status_tag", tag));
        }
        return ParkingService.map("community_id", admin.communityId, "community_audit_mode", commMode, "users", result);
    }

    @Transactional
    public Map<String, Object> setUserMode(UserAccount admin, Map<String, Object> req) {
        if (!demoEnabled) throw new ApiException(403, "正式版统一采用物业人工审核");
        UserAccount user = users.findById(num(req.get("user_id"))).filter(u -> u.communityId.equals(admin.communityId)).orElseThrow(() -> new ApiException(404, "未找到该业主账号"));
        Community comm = communities.findById(admin.communityId).orElse(null);
        String commMode = comm != null && comm.auditMode != null ? comm.auditMode : "THREE_CERT_AUTO";
        String asked = req.get("audit_mode") == null ? "" : str(req.get("audit_mode"));
        String old = user.auditMode;
        String desc;
        if ("THREE_CERT_AUTO".equals(asked) || "FLEXIBLE_HYBRID".equals(asked)) {
            user.auditMode = asked;
            desc = "FLEXIBLE_HYBRID".equals(asked) ? "特批开通【灵活申报流程（自主材料申报）】" : "指定为【标准申报流程（极简准入）】";
        } else {
            user.auditMode = null;
            desc = "恢复跟随小区默认策略【" + ("FLEXIBLE_HYBRID".equals(commMode) ? "灵活申报" : "标准申报") + "】";
        }
        evidence.create(admin.communityId, user.id, "USER_AUDIT_MODE", "物业管理员【" + admin.realName + "】为业主【" + user.realName + "】设置特例申报模式: " + desc,
                Map.of("user_id", user.id, "user_name", user.realName, "old_mode", String.valueOf(old), "new_mode", String.valueOf(user.auditMode), "operator", admin.realName), null);
        String notice = "FLEXIBLE_HYBRID".equals(user.auditMode)
                ? "物业服务中心已为您单独开通【车位申报灵活通道】。当您再次进入车位申请页面时，将呈现极简自主材料申报流程，支持提交非标准权属凭证。"
                : "物业服务中心已更新您的车位申请规则: " + desc + "。";
        saveNotice(user.id, "车位申请通道策略更新通知", notice, "SYSTEM", null);
        String effective = ("THREE_CERT_AUTO".equals(user.auditMode) || "FLEXIBLE_HYBRID".equals(user.auditMode)) ? user.auditMode : commMode;
        return ParkingService.map("user_id", user.id, "user_audit_mode", user.auditMode, "effective_audit_mode", effective, "is_customized", user.auditMode != null, "_message", "已成功为业主【" + user.realName + "】" + desc + "！");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> applications(UserAccount admin, String status) {
        List<QueueApplication> list = (status == null || status.isBlank() || "ALL".equals(status))
                ? apps.findByCommunityIdOrderByIdDesc(admin.communityId)
                : apps.findByCommunityIdAndStatusOrderByIdDesc(admin.communityId, status);
        Map<String, String> labels = Map.of(
                "PENDING_REVIEW", "待物业人工审核", "REJECTED", "审核驳回", "WAITING", "排队轮候中", "ALLOCATED", "已中签待确认",
                "OFFLINE_PENDING", "线下签约中", "COMPLETED", "租约已生效", "CANCELLED", "已撤销/已归档", "TIMEOUT_CANCELLED", "超时放弃");
        List<Map<String, Object>> result = new ArrayList<>();
        for (QueueApplication app : list) {
            UserAccount u = app.userId == null ? null : users.findById(app.userId).orElse(null);
            UserAccount auditor = app.auditorId == null ? null : users.findById(app.auditorId).orElse(null);
            boolean legacyUnverified = app.legacyImportBatchId != null && app.userId == null;
            result.add(ParkingService.map(
                    "id", app.id, "user_id", app.userId, "applicant_name", u != null ? u.realName : legacyValue(app.legacyApplicantName),
                    "applicant_phone", u != null ? u.phone : legacyValue(app.legacyApplicantPhone),
                    "property_no", app.propertyNo, "plate_no", app.plateNo, "car_relationship", or(app.carRelationship, "SELF"), "is_spouse_car", Boolean.TRUE.equals(app.spouseCar),
                    "source_car_relationship", app.legacyCarRelationship,
                    "legacy_unverified", legacyUnverified, "legacy_source_rank", app.legacySourceRank,
                    "legacy_source_row", app.legacySourceRow, "legacy_notes", app.legacyNotes,
                    "priority_level", app.priorityLevel, "priority_label", app.priorityLevel != null && app.priorityLevel == 1 ? "家庭一车" : "家庭二车",
                    "current_rank", app.currentRank, "status", app.status, "status_label", legacyUnverified ? "历史排队（身份待补）" : labels.getOrDefault(app.status, app.status),
                    "audit_mode", or(app.auditMode, "THREE_CERT_AUTO"), "audit_mode_label", app.legacyImportBatchId != null ? "历史台账导入" : "FLEXIBLE_HYBRID".equals(app.auditMode) ? "物业人工审核" : "演示自动审核",
                    "reject_reason", app.rejectReason, "attachments", parse(app.attachmentsJson), "apply_time", ParkingService.formatApplicationTime(app),
                    "audit_time", app.auditTime == null ? "--" : Times.fmtBj(app.auditTime), "auditor_name", auditor != null ? auditor.realName : "--",
                    "audit_reference", app.auditReference));
        }
        return result;
    }

    @Transactional
    public String audit(UserAccount admin, Map<String, Object> req) {
        communities.lockById(admin.communityId).orElseThrow(() -> new ApiException(404, "小区不存在"));
        QueueApplication app = apps.findById(num(req.get("app_id"))).filter(a -> a.communityId.equals(admin.communityId)).orElseThrow(() -> new ApiException(404, "未找到指定车位申请单"));
        if (!"PENDING_REVIEW".equals(app.status)) throw new ApiException(400, "该申请单当前状态为【" + app.status + "】，仅待人工审核单据允许操作！");
        UserAccount applicant = users.findById(app.userId).orElse(null);
        String name = applicant != null ? applicant.realName : "申请业主";
        String action = str(req.get("action"));
        if ("APPROVE".equals(action)) {
            String reference = str(req.get("audit_reference")).trim();
            if (!demoEnabled && (!Boolean.TRUE.equals(req.get("documents_verified")) || reference.isBlank()))
                throw new ApiException(400, "请先线下核对证明材料并填写核验登记编号");
            app.status = "WAITING";
            app.auditTime = Times.utcNow();
            app.auditorId = admin.id;
            app.auditReference = reference.isBlank() ? null : reference;
            app.rejectReason = null;
            evidence.create(admin.communityId, applicant != null ? applicant.id : admin.id, "AUDIT_PASS",
                    "物业管理员【" + admin.realName + "】审核通过业主【" + name + "】车位申请，单据正式进入排位序列",
                    Map.of("app_id", app.id, "property_no", app.propertyNo, "plate_no", app.plateNo, "auditor", admin.realName,
                            "action", "APPROVE", "audit_reference", reference), null);
            saveNotice(app.userId, "车位申请审核通过通知", "恭喜！您提交的车位申请（房屋: " + app.propertyNo + "，车辆: " + app.plateNo + "）已通过物业人工审核，现已正式进入小区公平排位序列！", "SYSTEM", "GO_QUEUE");
            engine.recalculate(admin.communityId);
            return "审核通过！业主【" + name + "】的申请已正式进入车位排位序列。";
        }
        if ("REJECT".equals(action)) {
            String reason = str(req.get("reject_reason")).trim();
            if (reason.isEmpty()) throw new ApiException(400, "审核驳回时必须详细填写驳回理由！");
            app.status = "REJECTED";
            app.rejectReason = reason;
            app.auditTime = Times.utcNow();
            app.auditorId = admin.id;
            evidence.create(admin.communityId, applicant != null ? applicant.id : admin.id, "AUDIT_REJECT",
                    "物业管理员【" + admin.realName + "】审核驳回业主【" + name + "】车位申请 (理由: " + reason + ")",
                    Map.of("app_id", app.id, "property_no", app.propertyNo, "plate_no", app.plateNo, "auditor", admin.realName, "action", "REJECT", "reason", reason), null);
            saveNotice(app.userId, "车位申请审核驳回通知", "很抱歉，您提交的车位申请（房屋: " + app.propertyNo + "，车辆: " + app.plateNo + "）未能通过物业人工审核。驳回理由：【" + reason + "】。您可以重新调整申请材料后再次提交。", "SYSTEM", "GO_QUEUE");
            return "已成功驳回该申请，驳回原因已通过站内消息即时送达业主。";
        }
        throw new ApiException(400, "无效的审核操作动作 (仅支持 APPROVE 或 REJECT)");
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

    private List<Map<String, Object>> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try { return json.readValue(raw, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    private static void row(Sheet sheet, CellStyle style, Object... vals) {
        Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
        for (int i = 0; i < vals.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(vals[i] == null ? "" : String.valueOf(vals[i]));
            if (style != null && sheet.getPhysicalNumberOfRows() == 1) cell.setCellStyle(style);
        }
    }

    private static String label(String mode) { return "THREE_CERT_AUTO".equals(mode) ? "三证合一（AI 自动化审核）" : "灵活配置（AI 自动化 + 人工审核）"; }
    private String contractName(RentalContract contract) {
        if (contract.userId != null) return users.findById(contract.userId).map(u -> u.realName).orElse("--");
        return contract.legacyImportBatchId == null ? "--" : legacyValue(contract.legacyTenantName);
    }
    private String contractPhone(RentalContract contract) {
        if (contract.userId != null) return users.findById(contract.userId).map(u -> u.phone).orElse("--");
        return contract.legacyImportBatchId == null ? "--" : legacyValue(contract.legacyTenantPhone);
    }
    private static String contractStart(RentalContract contract) {
        return contract.startDate != null ? contract.startDate.toString()
                : contract.legacyImportBatchId == null ? "--" : legacyValue(contract.legacyStartDateText);
    }
    private static String contractEnd(RentalContract contract) {
        return contract.endDate != null ? contract.endDate.toString()
                : contract.legacyImportBatchId == null ? "--" : "**";
    }
    private static String contractDisplayNo(RentalContract contract) {
        return contract.legacyImportBatchId == null ? contract.contractNo : legacyValue(contract.legacySourceContractNo);
    }
    private static String legacyValue(String value) { return value == null || value.isBlank() ? "**" : value; }
    private static String areaLabel(String areaType) {
        return "GROUND".equals(areaType) ? "地面" : "UNDERGROUND".equals(areaType) ? "地下" : "**";
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String or(String a, String b) { return a == null || a.isBlank() || "null".equals(a) ? b : a; }
    private static String emptyToNull(String s) { return s == null || s.isBlank() || "null".equals(s) ? null : s; }
    private static int num(Object o) { return o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o)); }
}
