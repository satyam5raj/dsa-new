package com.learning.spring.actuator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * TOPIC 08 proofs: a custom health indicator affects the overall status and the
 * HTTP code, custom metrics are recorded, and info is exposed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorTest {

    @Autowired MockMvc mvc;
    @Autowired InventoryHealthIndicator inventoryHealth;

    @AfterEach
    void restore() {
        inventoryHealth.setWarehouseReachable(true);
    }

    @Test
    void healthIncludesCustomComponent() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.inventory.status").value("UP"))
                .andExpect(jsonPath("$.components.inventory.details.warehouse").value("reachable"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));      // auto-configured DataSource check
    }

    @Test
    void oneComponentDown_wholeAppDown_http503() throws Exception {
        inventoryHealth.setWarehouseReachable(false);
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())                        // load balancers stop routing here
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.inventory.details.hint").exists());
    }

    @Test
    void customCounterIsRecordedWithTags() throws Exception {
        mvc.perform(post("/api/orders").param("channel", "mobile")).andExpect(status().isOk());
        mvc.perform(post("/api/orders").param("channel", "mobile")).andExpect(status().isOk());
        mvc.perform(post("/api/orders").param("channel", "web")).andExpect(status().isOk());

        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(get("/actuator/metrics/orders.placed").param("tag", "channel:mobile").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").value(2.0));
        mvc.perform(get("/actuator/metrics/orders.checkout.duration").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[?(@.statistic=='COUNT')].value").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThanOrEqualTo(3.0))));
    }

    @Test
    void infoEndpoint() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.description").value("Spring Boot learning project"));
    }
}
