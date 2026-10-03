package com.learning.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * TOPIC 06 proofs. Full context + MockMvc, so the REAL security filter chain runs.
 *   401 Unauthorized = not authenticated (no/invalid credentials)
 *   403 Forbidden    = authenticated, but not allowed
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired MockMvc mvc;

    // ---------- the real end-to-end flow: get a token, then use it ----------
    @Test
    void loginIssuesJwt_thenTokenAuthenticatesRequests() throws Exception {
        String userToken = token("user", "password");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));

        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());                         // URL rule: ADMIN only

        String adminToken = token("admin", "admin123");
        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"WRONG\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_returns401_publicEndpointsStillWork() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products")).andExpect(status().isOk());       // permitAll
    }

    @Test
    void tamperedToken_returns401() throws Exception {
        String token = token("user", "password");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";   // break the signature
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpBasicAlsoWorks() throws Exception {
        mvc.perform(get("/api/admin/stats").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    // ---------- method security with spring-security-test's jwt() helper ----------
    // jwt() fakes an already-validated token: no need to call /auth/token in every test.
    @Test
    void preAuthorize_onServiceMethod() throws Exception {
        mvc.perform(get("/api/reports/sales").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());                         // URL allows, METHOD denies
        mvc.perform(get("/api/reports/sales").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void preAuthorize_withMethodArguments_ownProfileOnly() throws Exception {
        var ravi = jwt().jwt(j -> j.subject("ravi")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
        mvc.perform(get("/api/users/ravi/profile").with(ravi)).andExpect(status().isOk());
        mvc.perform(get("/api/users/asha/profile").with(ravi)).andExpect(status().isForbidden());
    }

    @Test
    void actuatorSensitiveEndpoints_needAdmin() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());    // public (k8s probes)
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    private String token(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}
