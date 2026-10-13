package com.placementprep.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementprep.config.DemoData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The seeded demo account must give a lively dashboard on first start. */
@SpringBootTest(properties = {"app.demo-data=true", "spring.datasource.url=jdbc:h2:mem:demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class DemoDataIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void demoAccountHasHistoryStreakAndRevisionQueue() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", DemoData.DEMO_EMAIL,
                                "password", DemoData.DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + json.readTree(body).get("token").asText();

        mvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(30))
                .andExpect(jsonPath("$.totalApplications").value(14))
                .andExpect(jsonPath("$.offers").value(1))
                .andExpect(jsonPath("$.dueCount").value(5))
                .andExpect(jsonPath("$.todayCount").value(1));
        mvc.perform(get("/api/problems/due").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(5)));
    }
}
