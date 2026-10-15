package com.huiting.parking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiting.parking.entity.Community;
import com.huiting.parking.entity.UserAccount;
import com.huiting.parking.repo.Repos;
import com.huiting.parking.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductionSystemSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Repos.UserRepo users;
    @Autowired Repos.CommunityRepo communities;
    @Autowired PasswordService passwords;

    @Test
    void ownerPropertyAndPlatformRoutesAndCredentialChangesWorkTogether() throws Exception {
        Community community = new Community();
        community.name = "系统全链路验收小区";
        community.totalSpaces = 0;
        community.auditMode = "FLEXIBLE_HYBRID";
        community = communities.save(community);
        account("13800000901", "OWNER", community.id, "业主验收");
        account("13900000901", "PROPERTY_ADMIN", community.id, "物业验收");

        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/common/districts")).andExpect(status().isOk());
        String owner = login("/api/auth/login", "13800000901", "password123", false);
        String property = login("/api/auth/login", "13900000901", "password123", false);
        String platform = login("/api/admin/login", "13900000000", "test-bootstrap-admin-password", true);

        for (String route : new String[]{"/api/user/home", "/api/parking/audit-mode", "/api/parking/queue",
                "/api/parking/my-timeline", "/api/notifications", "/api/evidence/list"}) {
            mvc.perform(get(route).header("Authorization", bearer(owner))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/user/home").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.view_state").value("NO_APPLY"));
        for (String route : new String[]{"/api/property/stats", "/api/property/spaces",
                "/api/property/audit-config", "/api/property/community-users", "/api/property/applications",
                "/api/property/export-spaces"}) {
            mvc.perform(get(route).header("Authorization", bearer(property))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/property/stats").header("Authorization", bearer(property)))
                .andExpect(jsonPath("$.data.occupancy_rate").value("--"));
        for (String route : new String[]{"/api/admin/stats", "/api/admin/users", "/api/admin/login-logs",
                "/api/admin/operation-logs", "/api/admin/external-api-logs"}) {
            mvc.perform(get(route).header("Authorization", bearer(platform))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/property/stats").header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(property)))
                .andExpect(status().isForbidden());

        changePasswordAndVerifyOldSessionRevoked("/api/auth/login", "13800000901", "password123", "Owner#8901", owner, false);
        changePasswordAndVerifyOldSessionRevoked("/api/auth/login", "13900000901", "password123", "Manager#901", property, false);
        changePasswordAndVerifyOldSessionRevoked("/api/admin/login", "13900000000", "test-bootstrap-admin-password", "Admin#8901", platform, true);
    }

    private void account(String phone, String role, Integer communityId, String name) {
        UserAccount user = new UserAccount();
        user.phone = phone;
        user.idCard = "0000000" + phone;
        user.passwordHash = passwords.encode("password123");
        user.realName = name;
        user.role = role;
        user.communityId = communityId;
        users.save(user);
    }

    private String login(String path, String phone, String password, boolean admin) throws Exception {
        String identifier = admin ? "username" : "identifier";
        String body = json.writeValueAsString(java.util.Map.of(identifier, phone, "password", password));
        String response = mvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("token").asText();
    }

    private void changePasswordAndVerifyOldSessionRevoked(String loginPath, String phone, String oldPassword,
                                                           String newPassword, String oldToken, boolean admin) throws Exception {
        mvc.perform(post("/api/user/change-password").header("Authorization", bearer(oldToken))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("old_password", oldPassword,
                        "new_password", "short"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/user/change-password").header("Authorization", bearer(oldToken))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("old_password", oldPassword,
                        "new_password", newPassword))))
                .andExpect(status().isOk());
        mvc.perform(get(admin ? "/api/admin/stats" : "/api/user/home")
                .header("Authorization", bearer(oldToken))).andExpect(status().isUnauthorized());
        String freshToken = login(loginPath, phone, newPassword, admin);
        mvc.perform(get(admin ? "/api/admin/stats" : "/api/user/home")
                .header("Authorization", bearer(freshToken))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(freshToken))).andExpect(status().isOk());
        mvc.perform(get("/api/user/home").header("Authorization", bearer(freshToken)))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(String token) { return "Bearer " + token; }
}
