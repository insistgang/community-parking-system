package com.huiting.parking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.entity.ChainEvidence;
import com.huiting.parking.repo.Repos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class EvidenceService {
    private final Repos.EvidenceRepo repo;
    private final ObjectMapper json;

    public EvidenceService(Repos.EvidenceRepo repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Transactional
    public ChainEvidence create(Integer communityId, Integer userId, String eventType, String summary, Map<String, ?> payload, LocalDateTime occurredAt) {
        ChainEvidence last = repo.findFirstByOrderByIdDesc().orElse(null);
        String prev = last != null ? last.txHash : "0x0000000000000000000000000000000000000000000000000000000000000000";
        int height = last != null ? last.blockHeight : 1829000;
        LocalDateTime occurred = occurredAt != null ? occurredAt : LocalDateTime.now(ZoneOffset.UTC);
        String ts = String.valueOf(occurred.toInstant(ZoneOffset.UTC).toEpochMilli());
        String payloadStr;
        try {
            payloadStr = json.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            payloadStr = "{}";
        }
        String raw = eventType + "_" + communityId + "_" + userId + "_" + ts + "_" + prev + "_" + payloadStr;
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        ChainEvidence rec = new ChainEvidence();
        rec.evidenceId = "AGY-" + eventType + "-" + ts.substring(Math.max(0, ts.length() - 6)) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        rec.communityId = communityId;
        rec.userId = userId;
        rec.eventType = eventType;
        rec.blockHeight = height + 1;
        rec.txHash = "0x" + hash;
        rec.prevHash = prev;
        rec.eventSummary = summary;
        rec.rawPayloadJson = payloadStr;
        rec.timestamp = ts;
        rec.createdAt = occurred;
        return repo.save(rec);
    }
}
