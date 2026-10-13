package com.placementprep.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end through HTTP, security, validation, JPA and Flyway on an in-memory database. */
@SpringBootTest(properties = "app.demo-data=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String register(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "email", email, "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.dailyGoal").value(3))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private long createApplication(String auth, String company) throws Exception {
        String body = mvc.perform(post("/api/applications").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("company", company, "jobTitle", "Java Developer",
                                "source", "LINKEDIN", "packageLpa", 6.5, "location", "Pune"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("APPLIED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void apiRequiresAToken() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/applications").header("Authorization", "Bearer nonsense"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWorksAndBadPasswordsDoNot() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "A", "email", email, "password", "secret123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email.toUpperCase(), "password", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-one"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "A", "email", email, "password", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validationErrorsNameTheFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void applicationBoardFlowWithHistoryAndFunnel() throws Exception {
        String auth = register("Board");
        long id = createApplication(auth, "Persistent Systems");

        mvc.perform(patch("/api/applications/{id}/stage", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"OA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("OA"));
        mvc.perform(patch("/api/applications/{id}/stage", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"INTERVIEW\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/applications/{id}/history", id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].to").value("INTERVIEW"));

        mvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(1))
                .andExpect(jsonPath("$.funnel[0].count").value(1))
                .andExpect(jsonPath("$.funnel[2].stage").value("INTERVIEW"))
                .andExpect(jsonPath("$.funnel[2].count").value(1))
                .andExpect(jsonPath("$.byStage.INTERVIEW").value(1));

        mvc.perform(put("/api/applications/{id}", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("company", "Persistent", "jobTitle", "GET",
                                "source", "CAMPUS", "nextStep", "HR round", "nextStepOn", "2099-01-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("HR round"));

        mvc.perform(delete("/api/applications/{id}", id).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/{id}", id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        String alice = register("Alice");
        String bob = register("Bob");
        long id = createApplication(alice, "Infosys");

        mvc.perform(get("/api/applications/{id}", id).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/applications/{id}/stage", id).header("Authorization", bob)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"OFFER\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications").header("Authorization", bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void problemsAndSpacedRevision() throws Exception {
        String auth = register("Solver");
        String created = mvc.perform(post("/api/problems").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Coin Change", "platform", "LEETCODE",
                                "difficulty", "MEDIUM", "topic", "DP", "solvedOn", "2020-01-01"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextReviewOn").value("2020-01-02"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/problems/due").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(post("/api/problems/{id}/review", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quality\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetitions").value(2))
                .andExpect(jsonPath("$.intervalDays").value(6));

        mvc.perform(get("/api/problems/due").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/problems/{id}/review", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quality\":9}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/problems").param("topic", "DP").param("q", "coin").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/problems").param("topic", "GRAPHS").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(jsonPath("$.totalSolved").value(1))
                .andExpect(jsonPath("$.todayCount").value(1))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.heatmap.weeks", hasSize(26)));

        mvc.perform(put("/api/me/goal").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dailyGoal\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dailyGoal").value(5));
        mvc.perform(put("/api/me/goal").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dailyGoal\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/problems/{id}", id).header("Authorization", auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void healthAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/dashboard']").exists());
    }

    @Test
    void unknownEnumValueIsABadRequest() throws Exception {
        String auth = register("Enum");
        mvc.perform(get("/api/applications").param("stage", "HIRED").header("Authorization", auth))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dashboard").param("weeks", "52").header("Authorization", auth))
                .andExpect(jsonPath("$.heatmap.weeks", hasSize(52)));
    }
}
