package com.huiting.parking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.entity.ChainEvidence;
import com.huiting.parking.entity.Community;
import com.huiting.parking.entity.ImportBatch;
import com.huiting.parking.entity.ParkingSpace;
import com.huiting.parking.entity.RentalContract;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.entity.QueueApplication;
import com.huiting.parking.repo.Repos;
import com.huiting.parking.service.PasswordService;
import com.huiting.parking.service.EvidenceService;
import com.huiting.parking.service.InviteService;
import com.huiting.parking.service.QueueEngine;
import com.huiting.parking.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    @Autowired Repos.AppRepo applications;
    @Autowired Repos.DocumentRepo documents;
    @Autowired Repos.ReviewHistoryRepo reviewHistory;
    @Autowired Repos.NoticeRepo notices;
    @Autowired QueueEngine queueEngine;
    @Autowired Repos.ContractRepo contracts;
    @Autowired Repos.EvidenceRepo evidences;
    @Autowired Repos.ImportBatchRepo batches;
    @Autowired Repos.InviteRepo ownerInvites;
    @Autowired Repos.PropertyInviteRepo propertyInvites;
    @Autowired PasswordService passwords;
    @Autowired EvidenceService evidenceService;
    @Autowired InviteService inviteService;

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
        mvc.perform(post("/api/admin/property-invites").contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"community_name\":\"新建真实小区\"}"))
                .andExpect(status().isUnauthorized());
        String propertyInviteBody = mvc.perform(post("/api/admin/property-invites")
                .header("Authorization", bearer(adminToken)).contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"community_name\":\"新建真实小区\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String propertyInvite = json.readTree(propertyInviteBody).path("data").path("invite_code").asText();
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"invite_code\":\"" + propertyInvite + "\",\"community_name\":\"其他小区\"}"))
                .andExpect(status().isForbidden());
        String managerRegistration = mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000001\",\"id_card\":\"310104198005055555\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"invite_code\":\"" + propertyInvite + "\",\"community_name\":\"新建真实小区\",\"district\":\"徐汇区\",\"subdistrict\":\"龙华街道\",\"real_name\":\"物业经理\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(propertyInvites.findAll().get(0).usedAt).isNotNull();
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000002\",\"id_card\":\"310104198005055556\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"invite_code\":\"" + propertyInvite + "\",\"community_name\":\"新建真实小区\"}"))
                .andExpect(status().isForbidden());
        Community created = communities.findByName("新建真实小区").orElseThrow();
        assertThat(spaces.countByCommunityId(created.id)).isZero();
        String managerToken = json.readTree(managerRegistration).path("data").path("token").asText();
        String secondInviteBody = mvc.perform(post("/api/admin/property-invites")
                .header("Authorization", bearer(adminToken)).contentType("application/json")
                .content("{\"phone\":\"13900000002\",\"id_card\":\"310104198005055556\",\"community_name\":\"新建真实小区\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secondInvite = json.readTree(secondInviteBody).path("data").path("invite_code").asText();
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13900000002\",\"id_card\":\"310104198005055556\",\"password\":\"password123\",\"role\":\"PROPERTY_ADMIN\",\"invite_code\":\"" + secondInvite + "\",\"community_name\":\"新建真实小区\",\"real_name\":\"第二位物业经理\"}"))
                .andExpect(status().isOk());
        assertThat(users.findByPhone("13900000002").orElseThrow().communityId).isEqualTo(created.id);
        mvc.perform(post("/api/admin/property-invites").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"phone\":\"13900000003\",\"id_card\":\"310104198005055557\",\"community_name\":\"新建真实小区\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13800000001\",\"id_card\":\"310104199201011111\",\"password\":\"password123\",\"role\":\"OWNER\",\"community_name\":\"新建真实小区\",\"real_name\":\"业主\"}"))
                .andExpect(status().isForbidden());
        String inviteBody = mvc.perform(post("/api/property/owner-invites").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"phone\":\"13800000001\",\"id_card\":\"310104199201011111\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String invite = json.readTree(inviteBody).path("data").path("invite_code").asText();
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"phone\":\"13800000001\",\"id_card\":\"310104199201011111\",\"password\":\"password123\",\"role\":\"OWNER\",\"community_name\":\"新建真实小区\",\"real_name\":\"业主\",\"invite_code\":\"" + invite + "\"}"))
                .andExpect(status().isOk());
        assertThatThrownBy(() -> inviteService.consume(invite, "13800000001", "310104199201011111", created.id))
                .isInstanceOf(ApiException.class);
        for (var oldPropertyInvite : propertyInvites.findAll()) {
            oldPropertyInvite.expiresAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(1);
            propertyInvites.save(oldPropertyInvite);
        }
        var oldOwnerInvite = ownerInvites.findAll().get(0);
        oldOwnerInvite.expiresAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(1);
        ownerInvites.save(oldOwnerInvite);
        inviteService.removeExpired();
        assertThat(propertyInvites.count()).isZero();
        assertThat(ownerInvites.count()).isZero();
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"identifier\":\"13900000000\",\"login_type\":\"OTHER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legacyRentalWithoutIdentityRemainsOccupiedAndDisplaysMissingFields() throws Exception {
        Community community = new Community();
        community.name = "历史在租验收小区";
        community.totalSpaces = 1;
        community.auditMode = "FLEXIBLE_HYBRID";
        community.inventoryComplete = false;
        community = communities.save(community);
        UserAccount manager = account("13900000021", "PROPERTY_ADMIN", community.id);
        account("13800000021", "OWNER", community.id);
        ParkingSpace space = new ParkingSpace();
        space.communityId = community.id;
        space.spaceNo = "1";
        space.areaType = "**";
        space.status = "RENTED";
        space = spaces.save(space);
        RentalContract contract = new RentalContract();
        contract.contractNo = "IMPORT-TEST-1";
        contract.spaceId = space.id;
        contract.plateNo = "沪A12345";
        contract.endDate = java.time.LocalDate.now().minusDays(1);
        contract.status = "OVERDUE";
        contract.legacyTenantName = "历史租户";
        contract.legacyTenantPhone = "**";
        contract.legacyIdCard = "**";
        contract.legacyPropertyNo = "225-5-301";
        contract.legacyStartDateText = "**";
        contract.legacySourceContractNo = "**";
        contract.legacyImportBatchId = "test-batch";
        contract.legacySourceRow = 3;
        contracts.save(contract);

        String managerToken = login("/api/auth/login", "{\"identifier\":\"13900000021\",\"password\":\"password123\",\"login_type\":\"PASSWORD\"}");
        mvc.perform(get("/api/property/stats").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rented_spaces").value(1))
                .andExpect(jsonPath("$.data.occupancy_rate").value("100%"))
                .andExpect(jsonPath("$.data.pending_warnings").value(1))
                .andExpect(jsonPath("$.data.legacy_import_count").value(1));
        mvc.perform(get("/api/property/spaces").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].user_name").value("历史租户"))
                .andExpect(jsonPath("$.data[0].user_phone").value("**"))
                .andExpect(jsonPath("$.data[0].area_type").value("**"))
                .andExpect(jsonPath("$.data[0].legacy_unverified").value(true));
        mvc.perform(get("/api/property/space-history/" + space.id).header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.current.start_date").value("**"))
                .andExpect(jsonPath("$.data.current.id_card").value("**"));
        String ownerToken = login("/api/auth/login", "{\"identifier\":\"13800000021\",\"password\":\"password123\",\"login_type\":\"PASSWORD\"}");
        mvc.perform(get("/api/user/home").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.public_spaces[0].owner_name").value("历**"))
                .andExpect(jsonPath("$.data.community_overview.inventory_complete").value(false));
        mvc.perform(post("/api/property/set-maintenance").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_id\":" + space.id + ",\"action\":\"SET_MAINTENANCE\",\"reason\":\"验收测试\"}"))
                .andExpect(status().isOk());
        assertThat(contracts.findById(contract.id).orElseThrow().status).isEqualTo("TERMINATED");
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
        String adminToken = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");

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

        // 未有权威房屋台账时，地址关键词不能替代资格核验；交由平台人工审核。
        int appId = submitApplication(ownerToken, "浦东新区世纪大道888弄201室", "沪A12345");
        assertThat(reviewHistory.findByApplicationIdOrderByIdAsc(appId)).hasSize(1)
                .first().extracting(r -> r.eventType).isEqualTo("SUBMITTED");
        mvc.perform(get("/api/admin/applications?status=PENDING_REVIEW").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(appId))
                .andExpect(jsonPath("$.data[0].wait_days").isNumber());
        mvc.perform(get("/api/property/applications?status=PENDING_REVIEW").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(appId));

        mvc.perform(post("/api/property/spaces").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"space_no\":\"A-001\",\"area_type\":\"GROUND\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(managerToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(ownerToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(adminToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(adminToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(adminToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"community_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(adminToken))
                .contentType("application/json")
                .content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\",\"documents_verified\":true,\"community_verified\":true,\"audit_reference\":\"CHECK-001\"}"))
                .andExpect(status().isBadRequest());
        assertThat(evidences.countByCommunityIdAndEventType(home.id, "AUDIT_PASS")).isEqualTo(1);
        assertThat(reviewHistory.findByApplicationIdOrderByIdAsc(appId)).hasSize(2);
        mvc.perform(get("/api/user/home").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_state").value("ALLOCATED"));
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());
        assertThat(applications.findById(appId).orElseThrow().status).isEqualTo("ALLOCATED");
        mvc.perform(post("/api/parking/accept-allocation").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());
        assertThat(applications.findById(appId).orElseThrow().status).isEqualTo("OFFLINE_PENDING");

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

    @Test
    void pendingReviewCanBeCancelledAndCannotBeApprovedAfterwards() throws Exception {
        Community community = new Community();
        community.name = "待审核撤销验收小区";
        community.totalSpaces = 0;
        community.auditMode = "FLEXIBLE_HYBRID";
        community = communities.save(community);
        UserAccount owner = account("13800000031", "OWNER", community.id);
        account("13800000032", "OWNER", community.id);
        account("13900000031", "PROPERTY_ADMIN", community.id);
        String ownerToken = login("/api/auth/login", "{\"identifier\":\"13800000031\",\"password\":\"password123\"}");
        String otherToken = login("/api/auth/login", "{\"identifier\":\"13800000032\",\"password\":\"password123\"}");
        String managerToken = login("/api/auth/login", "{\"identifier\":\"13900000031\",\"password\":\"password123\"}");
        String adminToken = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        int appId = submitApplication(ownerToken, "1号楼101室", "沪A12345");

        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(otherToken)))
                .andExpect(status().isBadRequest());
        assertThat(applications.findById(appId).orElseThrow().status).isEqualTo("PENDING_REVIEW");
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(ownerToken))
                .contentType("application/json").content("{\"reason\":\"补正材料后重新申请\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("待审核申请已成功撤销，可修改资料后重新申请"));
        assertThat(applications.findById(appId).orElseThrow().status).isEqualTo("CANCELLED");
        assertThat(applications.findById(appId).orElseThrow().currentRank).isZero();
        mvc.perform(get("/api/user/home").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.data.view_state").value("NO_APPLY"));
        mvc.perform(get("/api/property/stats").header("Authorization", bearer(managerToken)))
                .andExpect(jsonPath("$.data.pending_audit_count").value(0));
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(adminToken))
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of(
                        "app_id", appId, "action", "APPROVE", "documents_verified", true, "community_verified", true, "audit_reference", "CHECK-CANCELLED"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());
        assertThat(evidences.countByCommunityIdAndEventType(community.id, "CANCEL")).isEqualTo(1);
        ChainEvidence cancellation = evidences.findFirstByCommunityIdOrderByIdDesc(community.id).orElseThrow();
        assertThat(cancellation.userId).isEqualTo(owner.id);
        assertThat(cancellation.eventSummary).contains("撤销待平台审核的车位申请").doesNotContain("第【0】位");
        assertThat(json.readTree(cancellation.rawPayloadJson).path("previous_status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(submitApplication(ownerToken, "1号楼102室", "沪A12345")).isGreaterThan(appId);
    }

    @Test
    void cancellingWaitingApplicationAdvancesRemainingQueue() throws Exception {
        Community community = new Community();
        community.name = "排队撤销验收小区";
        community.totalSpaces = 0;
        community.auditMode = "FLEXIBLE_HYBRID";
        community = communities.save(community);
        account("13800000041", "OWNER", community.id);
        account("13800000042", "OWNER", community.id);
        account("13900000041", "PROPERTY_ADMIN", community.id);
        String first = login("/api/auth/login", "{\"identifier\":\"13800000041\",\"password\":\"password123\"}");
        String second = login("/api/auth/login", "{\"identifier\":\"13800000042\",\"password\":\"password123\"}");
        String manager = login("/api/auth/login", "{\"identifier\":\"13900000041\",\"password\":\"password123\"}");
        String admin = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        int firstApp = submitApplication(first, "1号楼101室", "沪A12345");
        int secondApp = submitApplication(second, "2号楼101室", "沪B12345");
        for (int appId : new int[]{firstApp, secondApp}) {
            mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(admin))
                    .contentType("application/json").content(json.writeValueAsString(java.util.Map.of(
                            "app_id", appId, "action", "APPROVE", "documents_verified", true, "community_verified", true, "audit_reference", "CHECK-" + appId))))
                    .andExpect(status().isOk());
        }
        assertThat(applications.findById(secondApp).orElseThrow().currentRank).isEqualTo(2);
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("排队申请已成功撤销，排位已释放"));
        assertThat(applications.findById(firstApp).orElseThrow().status).isEqualTo("CANCELLED");
        assertThat(applications.findById(firstApp).orElseThrow().currentRank).isZero();
        assertThat(applications.findById(secondApp).orElseThrow().currentRank).isEqualTo(1);
        assertThat(contracts.count()).isZero();
    }

    @Test
    void correctionKeepsApplicationAndEarlierDocumentsForPlatformReview() throws Exception {
        Community community = new Community();
        community.name = "材料补正验收小区";
        community.totalSpaces = 0;
        community = communities.save(community);
        account("13800000061", "OWNER", community.id);
        account("13800000062", "OWNER", community.id);
        account("13900000061", "PROPERTY_ADMIN", community.id);
        String owner = login("/api/auth/login", "{\"identifier\":\"13800000061\",\"password\":\"password123\"}");
        String stranger = login("/api/auth/login", "{\"identifier\":\"13800000062\",\"password\":\"password123\"}");
        String manager = login("/api/auth/login", "{\"identifier\":\"13900000061\",\"password\":\"password123\"}");
        String admin = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        int appId = submitApplication(owner, "1号楼101室", "沪A12345");
        QueueApplication original = applications.findById(appId).orElseThrow();
        var initialDocs = json.readTree(original.attachmentsJson);
        String idCard = initialDocs.get(0).path("id").asText();
        String oldProperty = initialDocs.get(1).path("id").asText();
        String vehicle = initialDocs.get(2).path("id").asText();

        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(admin))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "action", "REQUEST_CORRECTION",
                        "correction_reason", "房屋证明第二页不清晰，请重新上传"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.view_state").value("NEEDS_CORRECTION"))
                .andExpect(jsonPath("$.data.my_queue.id").value(appId));
        mvc.perform(get("/api/parking/queue").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.waiting.length()").value(0));
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(manager))
                .contentType("application/json").content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
        String newProperty = uploadDocument(owner, "PROPERTY_CERT");
        var correction = java.util.Map.of("app_id", appId, "document_ids", java.util.List.of(idCard, newProperty, vehicle));
        mvc.perform(post("/api/parking/resubmit-correction").header("Authorization", bearer(stranger))
                .contentType("application/json").content(json.writeValueAsString(correction)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/parking/resubmit-correction").header("Authorization", bearer(owner))
                .contentType("application/json").content(json.writeValueAsString(correction)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(appId))
                .andExpect(jsonPath("$.data.review_version").value(2));
        mvc.perform(post("/api/parking/resubmit-correction").header("Authorization", bearer(owner))
                .contentType("application/json").content(json.writeValueAsString(correction)))
                .andExpect(status().isConflict());
        assertThat(documents.findById(oldProperty).orElseThrow().applicationId).isEqualTo(appId);
        assertThat(documents.findById(newProperty).orElseThrow().applicationId).isEqualTo(appId);
        assertThat(reviewHistory.findByApplicationIdOrderByIdAsc(appId)).extracting(r -> r.eventType)
                .containsExactly("SUBMITTED", "CORRECTION_REQUESTED", "RESUBMITTED");
        var versions = reviewHistory.findByApplicationIdOrderByIdAsc(appId);
        assertThat(json.readTree(versions.get(0).snapshotJson).path("attachments").get(1).path("id").asText()).isEqualTo(oldProperty);
        assertThat(json.readTree(versions.get(2).snapshotJson).path("attachments").get(1).path("id").asText()).isEqualTo(newProperty);
        mvc.perform(get("/api/admin/applications?status=PENDING_REVIEW").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data[0].review_history[0].snapshot.attachments[1].id").value(oldProperty))
                .andExpect(jsonPath("$.data[0].review_history[2].snapshot.attachments[1].id").value(newProperty));

        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(admin))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "action", "APPROVE",
                        "documents_verified", true, "community_verified", true, "audit_reference", "CHECK-CORRECTION"))))
                .andExpect(status().isOk());
        assertThat(reviewHistory.findByApplicationIdOrderByIdAsc(appId)).extracting(r -> r.eventType)
                .containsExactly("SUBMITTED", "CORRECTION_REQUESTED", "RESUBMITTED", "APPROVED");
        assertThat(applications.findById(appId).orElseThrow().status).isEqualTo("WAITING");
    }

    @Test
    void rejectedOwnerCanAppealOnceWithoutPropertyAuditPermission() throws Exception {
        Community community = new Community();
        community.name = "申诉复审验收小区";
        community.totalSpaces = 0;
        community = communities.save(community);
        account("13800000071", "OWNER", community.id);
        account("13900000071", "PROPERTY_ADMIN", community.id);
        String owner = login("/api/auth/login", "{\"identifier\":\"13800000071\",\"password\":\"password123\"}");
        String manager = login("/api/auth/login", "{\"identifier\":\"13900000071\",\"password\":\"password123\"}");
        String admin = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        int appId = submitApplication(owner, "2号楼202室", "沪A12345");
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(admin))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "action", "REJECT", "reject_reason", "请进一步核实房屋权属"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.my_queue.can_appeal").value(true));
        mvc.perform(post("/api/parking/appeal").header("Authorization", bearer(owner))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "reason", "房屋产权为本人持有，请重新核对原件。"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(appId));
        mvc.perform(get("/api/admin/applications?status=APPEAL_PENDING").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data[0].id").value(appId));
        mvc.perform(post("/api/property/audit-application").header("Authorization", bearer(manager))
                .contentType("application/json").content("{\"app_id\":" + appId + ",\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/audit-application").header("Authorization", bearer(admin))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "action", "REJECT", "reject_reason", "复审后仍不能确认房屋权属"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.my_queue.can_appeal").value(false));
        mvc.perform(post("/api/parking/appeal").header("Authorization", bearer(owner))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("app_id", appId, "reason", "再次提交人工复审申请说明"))))
                .andExpect(status().isConflict());
        assertThat(reviewHistory.findByApplicationIdOrderByIdAsc(appId)).extracting(r -> r.eventType)
                .containsExactly("SUBMITTED", "REJECTED", "APPEALED", "APPEAL_UPHELD");
    }

    private int submitApplication(String token, String propertyNo, String plateNo) throws Exception {
        var documentIds = uploadRequiredDocuments(token);
        String response = mvc.perform(post("/api/parking/apply").header("Authorization", bearer(token))
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of(
                        "property_no", propertyNo, "plate_no", plateNo, "car_relationship", "SELF",
                        "audit_mode", "THREE_CERT_AUTO", "document_ids", documentIds))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("id").asInt();
    }

    @Test
    void documentsArePrivateAndRequiredForProductionApplications() throws Exception {
        Community community = new Community();
        community.name = "三证上传验收小区";
        community.totalSpaces = 0;
        community = communities.save(community);
        Community other = new Community();
        other.name = "其他材料隔离小区";
        other.totalSpaces = 0;
        other = communities.save(other);
        account("13800000051", "OWNER", community.id);
        account("13800000052", "OWNER", community.id);
        account("13900000051", "PROPERTY_ADMIN", community.id);
        account("13900000052", "PROPERTY_ADMIN", other.id);
        String owner = login("/api/auth/login", "{\"identifier\":\"13800000051\",\"password\":\"password123\"}");
        String otherOwner = login("/api/auth/login", "{\"identifier\":\"13800000052\",\"password\":\"password123\"}");
        String manager = login("/api/auth/login", "{\"identifier\":\"13900000051\",\"password\":\"password123\"}");
        String foreignManager = login("/api/auth/login", "{\"identifier\":\"13900000052\",\"password\":\"password123\"}");
        String platformAdmin = login("/api/admin/login", "{\"username\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "证件.png", "image/png", png())).param("document_type", "ID_CARD"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "证件.png", "image/png", png())).param("document_type", "ID_CARD")
                .header("Authorization", bearer(manager)))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "fake.png", "image/png", "<script>bad</script>".getBytes()))
                .param("document_type", "ID_CARD").header("Authorization", bearer(owner))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
                .param("document_type", "ID_CARD").header("Authorization", bearer(owner))).andExpect(status().isPayloadTooLarge());

        String draft = uploadDocument(owner, "ID_CARD");
        String path = "/api/parking/application-documents/" + draft;
        mvc.perform(get(path).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(content().bytes(png()))
                .andExpect(header().string("Cache-Control", "no-store"));
        for (String token : java.util.List.of(otherOwner, manager, foreignManager, platformAdmin)) {
            mvc.perform(get(path).header("Authorization", bearer(token))).andExpect(status().isNotFound());
        }
        mvc.perform(delete(path).header("Authorization", bearer(owner))).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", bearer(owner))).andExpect(status().isNotFound());

        var ids = uploadRequiredDocuments(owner);
        var body = new java.util.HashMap<String, Object>();
        body.put("property_no", "1号楼101室");
        body.put("plate_no", "沪A12345");
        body.put("car_relationship", "SELF");
        body.put("attachments", java.util.List.of(java.util.Map.of("type", "ID_CARD", "name", "伪造材料元数据")));
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.put("document_ids", ids.subList(0, 1));
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.put("document_ids", ids);
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(otherOwner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.put("plate_no", "132");
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        assertThat(documents.findById(ids.get(0)).orElseThrow().applicationId).isNull();

        body.put("plate_no", "沪a·12345");
        String result = mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        int appId = json.readTree(result).path("data").path("id").asInt();
        assertThat(documents.findById(ids.get(0)).orElseThrow().applicationId).isEqualTo(appId);
        assertThat(applications.findById(appId).orElseThrow().plateNo).isEqualTo("沪A12345");
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "重复材料.png", "image/png", png()))
                .param("document_type", "ID_CARD").header("Authorization", bearer(owner)))
                .andExpect(status().isConflict());
        path = "/api/parking/application-documents/" + ids.get(0);
        mvc.perform(get(path).header("Authorization", bearer(manager))).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", bearer(platformAdmin))).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", bearer(foreignManager))).andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization", bearer(owner))).andExpect(status().isConflict());
        mvc.perform(post("/api/parking/cancel-apply").header("Authorization", bearer(owner))).andExpect(status().isOk());
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());

        var spouseIds = new java.util.ArrayList<>(uploadRequiredDocuments(owner));
        body.put("document_ids", spouseIds);
        body.put("car_relationship", "SPOUSE");
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        spouseIds.add(uploadDocument(owner, "RELATIONSHIP_PROOF"));
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(owner)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    private java.util.List<String> uploadRequiredDocuments(String token) throws Exception {
        var ids = new java.util.ArrayList<String>();
        for (String type : java.util.List.of("ID_CARD", "PROPERTY_CERT", "VEHICLE_LICENSE")) ids.add(uploadDocument(token, type));
        return ids;
    }

    private String uploadDocument(String token, String type) throws Exception {
        String response = mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "验收材料.png", "image/png", png()))
                .header("Authorization", bearer(token)).param("document_type", type))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("id").asText();
    }

    private byte[] png() {
        return java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j5E0AAAAASUVORK5CYII=");
    }

    @Test
    void historicalQueuePreservesPositionWithoutCreatingAnAccountOrSkippingAllocation() throws Exception {
        Community community = new Community();
        community.name = "历史排队验收小区";
        community.totalSpaces = 1;
        community.auditMode = "FLEXIBLE_HYBRID";
        community.sourceAuditMode = "三证自动";
        community = communities.save(community);
        UserAccount legacyOwner = account("13800000101", "OWNER", community.id);
        legacyOwner.realName = "历史业主甲";
        legacyOwner.idCard = null;
        users.save(legacyOwner);
        UserAccount newOwner = account("13800000102", "OWNER", community.id);
        UserAccount pendingRental = account("13800000103", "OWNER", community.id);
        pendingRental.idCard = null;
        users.save(pendingRental);
        UserAccount manager = account("13900000101", "PROPERTY_ADMIN", community.id);
        manager.jobTitle = "物业负责人";
        manager.notes = "经理";
        users.save(manager);
        ParkingSpace space = new ParkingSpace();
        space.communityId = community.id;
        space.spaceNo = "H-001";
        space = spaces.save(space);
        QueueApplication legacy = new QueueApplication();
        legacy.communityId = community.id;
        legacy.propertyNo = "1号楼101室";
        legacy.plateNo = "沪A12345";
        legacy.applyTime = java.time.LocalDateTime.of(2025, 1, 12, 0, 0);
        legacy.legacyApplyTimeText = "2025-01-12 00:00:00";
        legacy.legacyApplicantName = "历史业主甲";
        legacy.legacyApplicantPhone = "13800000101";
        legacy.legacyIdCard = "0000000000000000001";
        legacy.legacyImportBatchId = "c".repeat(64);
        legacy.legacySourceRow = 3;
        legacy.legacySourceRank = 1;
        legacy.legacyCarRelationship = "业主";
        legacy.legacyPriorityText = "1车";
        legacy = applications.save(legacy);
        QueueApplication current = new QueueApplication();
        current.communityId = community.id;
        current.userId = newOwner.id;
        current.propertyNo = "2号楼101室";
        current.plateNo = "沪B12345";
        current.applyTime = java.time.LocalDateTime.of(2026, 10, 15, 1, 0);
        current = applications.save(current);
        long accountCount = users.count();
        queueEngine.recalculate(community.id);
        assertThat(users.count()).isEqualTo(accountCount);
        assertThat(applications.findById(legacy.id).orElseThrow().currentRank).isEqualTo(1);
        assertThat(applications.findById(current.id).orElseThrow().currentRank).isEqualTo(2);
        assertThat(spaces.findById(space.id).orElseThrow().status).isEqualTo("VACANT");
        assertThat(notices.findAll()).allMatch(n -> n.userId != null);
        String ownerToken = login("/api/auth/login", "{\"identifier\":\"13800000102\",\"password\":\"password123\"}");
        String pendingRentalToken = login("/api/auth/login", "{\"identifier\":\"13800000103\",\"password\":\"password123\"}");
        mvc.perform(get("/api/user/home").header("Authorization", bearer(pendingRentalToken)))
                .andExpect(jsonPath("$.data.view_state").value("IDENTITY_PENDING"))
                .andExpect(jsonPath("$.data.user_info.identity_pending").value(true));
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(pendingRentalToken))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        String managerToken = login("/api/auth/login", "{\"identifier\":\"13900000101\",\"password\":\"password123\"}");
        mvc.perform(get("/api/parking/queue").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waiting[0].rank").value(1))
                .andExpect(jsonPath("$.data.waiting[0].masked_name").value("历**"))
                .andExpect(jsonPath("$.data.waiting[0].masked_phone").value("138****0101"))
                .andExpect(jsonPath("$.data.waiting[0].apply_time").value("2025-01-12 00:00:00"))
                .andExpect(jsonPath("$.data.waiting[0].legacy_imported").value(true))
                .andExpect(jsonPath("$.data.waiting[0].is_me").value(false));
        String provisionalToken = login("/api/auth/login", "{\"identifier\":\"13800000101\",\"password\":\"password123\"}");
        mvc.perform(multipart("/api/parking/application-documents")
                .file(new MockMultipartFile("file", "待核实证件.png", "image/png", png()))
                .param("document_type", "ID_CARD").header("Authorization", bearer(provisionalToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(provisionalToken)))
                .andExpect(jsonPath("$.data.view_state").value("WAITING"))
                .andExpect(jsonPath("$.data.my_queue.current_rank").value(1))
                .andExpect(jsonPath("$.data.my_queue.apply_time").value("2025-01-12 00:00:00"))
                .andExpect(jsonPath("$.data.my_queue.identity_pending").value(true))
                .andExpect(jsonPath("$.data.my_queue.can_cancel").value(false));
        mvc.perform(get("/api/parking/queue").header("Authorization", bearer(provisionalToken)))
                .andExpect(jsonPath("$.data.waiting[0].is_me").value(true));
        mvc.perform(post("/api/parking/apply").header("Authorization", bearer(provisionalToken))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertThat(applications.findById(legacy.id).orElseThrow().userId).isNull();
        mvc.perform(get("/api/property/stats").header("Authorization", bearer(managerToken)))
                .andExpect(jsonPath("$.data.legacy_queue_unlinked_count").value(1))
                .andExpect(jsonPath("$.data.source_audit_mode").value("三证自动"));
        String applicationBody = mvc.perform(get("/api/property/applications").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode legacyView = json.readTree(applicationBody).path("data").get(1);
        assertThat(legacyView.path("applicant_name").asText()).isEqualTo("历史业主甲");
        assertThat(legacyView.path("source_car_relationship").asText()).isEqualTo("业主");
        assertThat(legacyView.path("legacy_unverified").asBoolean()).isTrue();
        assertThat(legacyView.path("apply_time").asText()).isEqualTo("2025-01-12 00:00:00");
        byte[] exported = mvc.perform(get("/api/property/export-spaces").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(exported))) {
            var row = workbook.getSheet("排队轮候名单").getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("历史业主甲");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("2025-01-12 00:00:00");
        }
        legacy = applications.findById(legacy.id).orElseThrow();
        legacy.userId = legacyOwner.id;
        applications.save(legacy);
        queueEngine.recalculate(community.id);
        assertThat(spaces.findById(space.id).orElseThrow().currentUserId).isEqualTo(legacyOwner.id);
        assertThat(applications.findById(legacy.id).orElseThrow().status).isEqualTo("ALLOCATED");
        assertThat(applications.findById(current.id).orElseThrow().currentRank).isEqualTo(1);
    }

    @Test
    void importEvidenceIsAdminOnlyIdempotentAndLocallyVerifiable() throws Exception {
        Community community = new Community();
        community.name = "初始化存证验收小区";
        community.totalSpaces = 0;
        community = communities.save(community);
        ImportBatch batch = new ImportBatch();
        batch.sourceSha256 = "a".repeat(64);
        batch.sourceFileName = "source.xlsx";
        batch.communityId = community.id;
        batch.rentalCount = 1;
        batch.overdueCount = 0;
        batch.missingIdCount = 0;
        batch.missingStartCount = 0;
        batch.missingPhoneCount = 0;
        batch.skippedQueueCount = 0;
        batch.queueCount = 0;
        batch.importedAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        batches.save(batch);
        String path = "/api/admin/import-evidence/" + batch.sourceSha256;
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        String ownerToken = login("/api/auth/login", "{\"identifier\":\"13900000000\",\"password\":\"test-bootstrap-admin-password\"}");
        mvc.perform(post(path).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict());
        batch.rentalCount = 0;
        batches.save(batch);
        String first = mvc.perform(post(path).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record_type").value("LOCAL_HASH_CHAIN"))
                .andExpect(jsonPath("$.data.already_recorded").value(false))
                .andReturn().getResponse().getContentAsString();
        String evidenceId = json.readTree(first).path("data").path("evidence_id").asText();
        assertThat(evidenceService.verify(evidences.findByEvidenceId(evidenceId).orElseThrow())).isTrue();
        account("13800000104", "OWNER", community.id);
        String communityOwnerToken = login("/api/auth/login", "{\"identifier\":\"13800000104\",\"password\":\"password123\"}");
        mvc.perform(post(path).header("Authorization", bearer(communityOwnerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/evidence/list").header("Authorization", bearer(communityOwnerToken)))
                .andExpect(jsonPath("$.data[0].event_type").value("PILOT_IMPORT"))
                .andExpect(jsonPath("$.data[0].external_status").value("NOT_CONNECTED"));
        mvc.perform(get("/api/evidence/verify/" + evidenceId).header("Authorization", bearer(communityOwnerToken)))
                .andExpect(jsonPath("$.data.is_valid").value(true))
                .andExpect(jsonPath("$.data.verification_status").value("LOCAL_INTEGRITY_VERIFIED"));
        mvc.perform(post(path).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.already_recorded").value(true));
        assertThat(evidences.countByCommunityIdAndEventType(community.id, "PILOT_IMPORT")).isEqualTo(1);
    }

    private UserAccount account(String phone, String role, Integer communityId) {
        UserAccount user = new UserAccount();
        user.phone = phone;
        user.idCard = "0000000" + phone;
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
