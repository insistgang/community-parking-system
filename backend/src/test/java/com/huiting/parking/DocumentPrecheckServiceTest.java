package com.huiting.parking;

import com.huiting.parking.service.DocumentPrecheckService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPrecheckServiceTest {
    private final DocumentPrecheckService precheck = new DocumentPrecheckService();

    @Test
    void identityNeedsNumberAndNeverEquatesMatchingNamesAloneWithVerification() {
        assertThat(precheck.identity("张三", "", "张三", "").status()).isEqualTo("UNDETERMINED");
        assertThat(precheck.identity("张三", "310104199201011111", "张三", "310104199201011112").status())
                .isEqualTo("FIELD_CONFLICT_REVIEW");
        var consistent = precheck.identity("张三", "310104199201011111", "张 三", "310104199201011111");
        assertThat(consistent.status()).isEqualTo("FIELD_MATCH");
        assertThat(consistent.explanation()).contains("仍需独立核验");
    }

    @Test
    void plateFormattingCanMatchButPropertyStillNeedsTrustedLedger() {
        assertThat(precheck.plate("沪A·12345", "沪a12345").status()).isEqualTo("FIELD_MATCH");
        assertThat(precheck.plate("沪A12345", "沪B12345").status()).isEqualTo("FIELD_CONFLICT_REVIEW");
        assertThat(precheck.propertyAddress("1号楼101室", "1号楼101室").status()).isEqualTo("UNDETERMINED");
    }
}
