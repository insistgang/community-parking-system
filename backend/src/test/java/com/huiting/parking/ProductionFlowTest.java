package com.huiting.parking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.entity.ChainEvidence;
import com.huiting.parking.entity.Community;
import com.huiting.parking.entity.ParkingSpace;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import com.huiting.parking.service.PasswordService;
import com.huiting.parking.service.EvidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductionFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Repos.UserRepo users;
    @Autowired Repos.CommunityRepo communities;
    @Autowired Repos.SpaceRepo spaces;
    @Autowired Repos.ContractRepo contracts;
    @Autowired Repos.EvidenceRepo evidences;
    @Autowired PasswordService passwords;
    @Autowired EvidenceService evidenceService;

    @Test
    void protectedApisRequireServerSessionAndDemoResetIsAbsent() throws Exception {
        mvc.perform(get("/api/admin/users").header("X-User-Id", "1"))
                .andExpect(status().isUnauthorized());

        String adminToken = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)).header("X-User-Id", "999"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/dev/reset-seed").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"community_name\":\"未授权小区\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"invite_code\":\"test-property-invite\",\"community_name\":\"新建真实小区\",\"district\":\"徐汇区\",\"subdistrict\":\"龙华街道\",\"real_name\":\"物业经理\"}"))
                .andExpect(status().isOk());
        Community created = communities.findByName("新建真实小区").orElseThrow();
        assertThat(spaces.countByCommunityId(created.id)).isZero();
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"identifier\":\"13900000000\",\"login_type\":\"OTHER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualReviewQueueIsolationAndEvidenceIntegrity() throws Exception {
        Community home = new Community();
        home.name = "生产验收小区";
        home.totalSpaces = 0;
        home.auditMode = "FLEXIBLE_HYBRID";
        home = communities.save(home);
        Community other = new Community();
        other.name = "隔离小区";
        other.totalSpaces = 0;
        other = communities.save(other);
        UserAccount owner = account("13800000011", "OWNER", home.id);
        UserAccount manager = account("13900000011", "PROPERTY_ADMIN", home.id);
        ParkingSpace foreign = new ParkingSpace();
        foreign.communityId = other.id;
        foreign.spaceNo = "B-001";
        foreign = spaces.save(foreign);

        String ownerToken = login("/api/auth/login", "{\"identifier\":\"13800000011\",\"password\":\"password123\",\"login_type\":\"PASSWORD\"}");
        String managerToken = login("/api/auth/login", "{\"identifier\":\"13900000011\",\"password\":\"password123\",\"login_type\":\"PASSWORD\"}");

        mvc.perform(get("/api/admin/users").header("Authorization", bearer(ownerToken)).header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/property/set-maintenance").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_id\":" + foreign.id + ",\"action\":\"SET_MAINTENANCE\"}"))
                .andExpect(status().isNotFound());
        assertThat(spaces.findById(foreign.id).orElseThrow().status).isEqualTo("VACANT");
        ChainEvidence foreignEvidence = evidenceService.create(other.id, null, "INIT", "其他小区建档", java.util.Map.of(), null);
        mvc.perform(get("/api/evidence/verify/" + foreignEvidence.evidenceId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());

        String applyBody = mvc.perform(post("/api/parking/apply").header("Authorization", bearer(ownerToken))
                .contentType("application/json")
                .content("{\"property_no\":\"12号楼302室\",\"plate_no\":\"沪A12345\",\"audit_mode\":\"THREE_CERT_AUTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        int appId = json.readTree(applyBody).path("data").path("id").asInt();

        mvc.perform(post("/api/property/spaces").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_no\":\"A-001\",\"area_type\":\"GROUND\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_state").value("ALLOCATED"));

        int spaceId = spaces.findByCommunityIdOrderBySpaceNoAsc(home.id).get(0).id;
        mvc.perform(post("/api/property/confirm-delivery").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_id\":" + spaceId + ",\"user_id\":" + owner.id + ",\"months\":6}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/property/confirm-delivery").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_id\":" + spaceId + ",\"user_id\":" + owner.id + ",\"months\":6,\"payment_confirmed\":true,\"receipt_no\":\"RECEIPT-001\"}"))
                .andExpect(status().isOk());
        assertThat(contracts.findFirstBySpaceIdAndStatusIn(spaceId, java.util.List.of("ACTIVE")).orElseThrow().paymentReceiptNo)
                .isEqualTo("RECEIPT-001");
        mvc.perform(post("/api/parking/renew").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());

        ChainEvidence record = evidences.findFirstByCommunityIdOrderByIdDesc(home.id).orElseThrow();
        mvc.perform(get("/api/evidence/verify/" + record.evidenceId).header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.data.is_valid").value(true));
        String summary = record.eventSummary;
        record.eventSummary = "被修改的业务摘要";
        evidences.save(record);
        mvc.perform(get("/api/evidence/verify/" + record.evidenceId).header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.data.is_valid").value(false));
        record.eventSummary = summary;
        record.rawPayloadJson = "{\"tampered\":true}";
        evidences.save(record);
        mvc.perform(get("/api/evidence/verify/" + record.evidenceId).header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.data.is_valid").value(false));
    }

    private UserAccount account(String phone, String role, Integer communityId) {
        UserAccount user = new UserAccount();
        user.phone = phone;
        user.realName = "测试用户";
        user.role = role;
        user.communityId = communityId;
        user.passwordHash = passwords.encode("password123");
        return users.save(user);
    }

    private String login(String path, String body) throws Exception {
        String result = mvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(result).path("data");
        return data.path("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
