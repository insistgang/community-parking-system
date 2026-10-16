package com.huiting.parking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.common.Times;
import com.huiting.parking.entity.ApplicationReviewHistory;
import com.huiting.parking.entity.QueueApplication;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ApplicationReviewHistoryService {
    private final Repos.ReviewHistoryRepo reviews;
    private final ObjectMapper json;

    public ApplicationReviewHistoryService(Repos.ReviewHistoryRepo reviews, ObjectMapper json) {
        this.reviews = reviews;
        this.json = json;
    }

    @Transactional
    public void ensureInitial(QueueApplication app, UserAccount applicant) {
        if (!reviews.existsByApplicationId(app.id))
            record(app, applicant, "SUBMITTED_LEGACY", "历史申请材料快照（在处理记录功能启用前提交）");
    }

    @Transactional
    public void record(QueueApplication app, UserAccount actor, String eventType, String summary) {
        ApplicationReviewHistory entry = new ApplicationReviewHistory();
        entry.applicationId = app.id;
        entry.versionNumber = app.reviewVersion == null ? 1 : app.reviewVersion;
        entry.eventType = eventType;
        entry.actorId = actor == null ? null : actor.id;
        entry.actorName = actor == null ? "系统" : actor.realName;
        entry.summary = summary;
        entry.createdAt = Times.utcNow();
        try {
            Object attachmentSnapshot = app.attachmentsJson == null ? List.of() : json.readTree(app.attachmentsJson);
            entry.snapshotJson = json.writeValueAsString(ParkingService.map(
                    "status", app.status, "property_no", app.propertyNo, "plate_no", app.plateNo,
                    "car_relationship", app.carRelationship, "attachments", attachmentSnapshot,
                    "submitted_at", app.lastSubmittedAt == null ? null : Times.fmtBj(app.lastSubmittedAt),
                    "audit_reference", app.auditReference, "reject_reason", app.rejectReason,
                    "appeal_reason", app.appealReason));
        } catch (Exception e) {
            throw new IllegalStateException("申请审核历史序列化失败", e);
        }
        reviews.save(entry);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Integer applicationId) {
        return reviews.findByApplicationIdOrderByIdAsc(applicationId).stream().map(r -> {
            try {
                return ParkingService.map("id", r.id, "version", r.versionNumber, "event_type", r.eventType,
                        "actor_name", r.actorName, "summary", r.summary, "created_at", Times.fmtBj(r.createdAt),
                        "snapshot", json.readTree(r.snapshotJson));
            } catch (Exception e) {
                throw new IllegalStateException("申请审核历史读取失败", e);
            }
        }).toList();
    }
}
