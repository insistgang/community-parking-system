package com.huiting.parking.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/** Deterministic field comparison only; it never decides parking eligibility. */
@Service
public class DocumentPrecheckService {
    public static final String RULE_VERSION = "PRECHECK_V1";

    public record Check(String status, String ruleVersion, String explanation) {}

    public Check identity(String registeredName, String registeredId, String documentName, String documentId) {
        String leftId = normalizeId(registeredId);
        String rightId = normalizeId(documentId);
        if (!leftId.matches("[0-9]{17}[0-9X]") || !rightId.matches("[0-9]{17}[0-9X]"))
            return unknown("证件号缺失或无法辨认，需人工核对原件");
        if (!leftId.equals(rightId)) return conflict("证件号与登记信息不一致，需人工复核");
        String leftName = normalizeText(registeredName);
        String rightName = normalizeText(documentName);
        if (leftName.isBlank() || rightName.isBlank()) return unknown("姓名缺失或无法辨认，需人工核对原件");
        if (!leftName.equals(rightName)) return conflict("姓名与登记信息不一致，需人工复核");
        return match("姓名和证件号字段一致；身份真实性仍需独立核验");
    }

    public Check plate(String declaredPlate, String documentPlate) {
        String left = normalizePlate(declaredPlate);
        String right = normalizePlate(documentPlate);
        if (left.isBlank() || right.isBlank()) return unknown("车牌字段缺失或无法辨认，需人工核对行驶证");
        if (!left.equals(right)) return conflict("申报车牌与行驶证识别字段不一致，需人工复核");
        return match("车牌字段一致；车辆权属与关系仍需人工核验");
    }

    public Check propertyAddress(String declaredAddress, String recognizedAddress) {
        if (normalizeText(declaredAddress).isBlank() || normalizeText(recognizedAddress).isBlank())
            return unknown("房屋地址缺失或无法辨认，需人工核对房屋证明");
        // No authoritative community/house ledger exists yet; string equality is not proof of eligibility.
        return unknown("房屋地址需与小区、楼栋及房屋台账人工核对");
    }

    private static String normalizeId(String raw) {
        return normalizeText(raw).toUpperCase(Locale.ROOT);
    }

    private static String normalizePlate(String raw) {
        return normalizeText(raw).toUpperCase(Locale.ROOT).replaceAll("[\\s·.\\-]", "");
    }

    private static String normalizeText(String raw) {
        return raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFKC).replaceAll("\\s+", "").trim();
    }

    private static Check match(String explanation) { return new Check("FIELD_MATCH", RULE_VERSION, explanation); }
    private static Check conflict(String explanation) { return new Check("FIELD_CONFLICT_REVIEW", RULE_VERSION, explanation); }
    private static Check unknown(String explanation) { return new Check("UNDETERMINED", RULE_VERSION, explanation); }
}
