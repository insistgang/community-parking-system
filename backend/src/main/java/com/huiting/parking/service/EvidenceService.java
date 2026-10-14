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
    private final Repos.CommunityRepo communities;
    private final ObjectMapper json;

    public EvidenceService(Repos.EvidenceRepo repo, Repos.CommunityRepo communities, ObjectMapper json) {
        this.repo = repo;
        this.communities = communities;
        this.json = json;
    }

    @Transactional
    public ChainEvidence create(Integer communityId, Integer userId, String eventType, String summary, Map<String, ?> payload, LocalDateTime occurredAt) {
        communities.lockById(communityId).orElseThrow(() -> new IllegalStateException("小区不存在"));
        ChainEvidence last = repo.findFirstByCommunityIdOrderByIdDesc(communityId).orElse(null);
        String prev = last != null ? last.txHash : "0x0000000000000000000000000000000000000000000000000000000000000000";
        int height = last != null ? last.blockHeight : 0;
        LocalDateTime occurred = occurredAt != null ? occurredAt : LocalDateTime.now(ZoneOffset.UTC);
        String ts = String.valueOf(occurred.toInstant(ZoneOffset.UTC).toEpochMilli());
        String payloadStr;
        try {
            payloadStr = json.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            payloadStr = "{}";
        }
        String raw = eventType + "_" + communityId + "_" + userId + "_" + ts + "_" + prev + "_" + summary + "_" + payloadStr;
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

    @Transactional(readOnly = true)
    public boolean verify(ChainEvidence rec) {
        String previous = "0x0000000000000000000000000000000000000000000000000000000000000000";
        int height = 1;
        for (ChainEvidence item : repo.findByCommunityIdOrderByIdAsc(rec.communityId)) {
            if (!previous.equals(item.prevHash) || item.blockHeight == null || item.blockHeight != height) return false;
            String payload = item.rawPayloadJson == null ? "{}" : item.rawPayloadJson;
            String raw = item.eventType + "_" + item.communityId + "_" + item.userId + "_" + item.timestamp + "_" + item.prevHash + "_" + item.eventSummary + "_" + payload;
            try {
                String actual = "0x" + HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
                if (!actual.equals(item.txHash)) return false;
            } catch (Exception e) {
                return false;
            }
            if (item.id.equals(rec.id)) return true;
            previous = item.txHash;
            height++;
        }
        return false;
    }
}
