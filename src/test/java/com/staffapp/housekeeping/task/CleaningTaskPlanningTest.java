package com.staffapp.housekeeping.task;

import static com.staffapp.housekeeping.DemoData.ALICE;
import static com.staffapp.housekeeping.DemoData.CLAIRE;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_A;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_B;
import static com.staffapp.housekeeping.DemoData.ROOM_A01;
import static com.staffapp.housekeeping.DemoData.ROOM_A02;
import static com.staffapp.housekeeping.DemoData.ROOM_B05;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.staffapp.housekeeping.TestJwtFactory;
import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.tenant.TenantFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * "As a manager, I plan the cleaning of an accommodation of my campsite for a given day. I can assign it to one of my
 * employees right away or later, and leave a comment."
 * <p>
 * Not transactional on purpose: each request commits, as in production, so that the conflict on an already open task is
 * detected across two transactions. The tasks are deleted after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CleaningTaskPlanningTest {

    private static final String DAY = "2026-07-14";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteTasks() {
        jdbcTemplate.update("DELETE FROM pms_notification");
        jdbcTemplate.update("DELETE FROM cleaning_task");
    }

    @Test
    @DisplayName("A manager plans the cleaning of a room, assigned to one of their employees, with a comment")
    void aManagerPlansAnAssignedCleaning() throws Exception {
        plan("marc", ESTABLISHMENT_A, """
                {"roomId": %d, "date": "%s", "assigneeId": %d, "comment": "Linen for 6"}
                """.formatted(ROOM_A01, DAY, ALICE))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern("http://[^/]+/api/cleaning-tasks/\\d+")))
                .andExpect(jsonPath("$.roomNumber").value("A01"))
                .andExpect(jsonPath("$.date").value(DAY))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeUsername").value("alice"))
                .andExpect(jsonPath("$.comment").value("Linen for 6"))
                .andExpect(jsonPath("$.createdBy").value("marc"));
    }

    @Test
    @DisplayName("A manager plans a cleaning without assigning it yet, and reads it back at its Location")
    void aManagerPlansAnUnassignedCleaning() throws Exception {
        var location = plan("marc", ESTABLISHMENT_A, request(ROOM_A01))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(authenticated(get(location), "alice", ESTABLISHMENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("A01"));
    }

    @Test
    @DisplayName("Only managers plan the cleaning, whatever the body: the role is checked before it is read")
    void anEmployeeCannotPlan() throws Exception {
        plan("alice", ESTABLISHMENT_A, request(ROOM_A01))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        plan("alice", ESTABLISHMENT_A, "{}").andExpect(status().isForbidden());

        assertNoTaskWasCreated();
    }

    @Test
    @DisplayName("Never two cleaning tasks open on the same accommodation")
    void refusesASecondOpenTaskOnTheSameRoom() throws Exception {
        plan("marc", ESTABLISHMENT_A, request(ROOM_A01)).andExpect(status().isCreated());

        plan("marc", ESTABLISHMENT_A, request(ROOM_A01))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Room A01 already has an open cleaning task"));
    }

    @Test
    @DisplayName("The open task rule is per accommodation: another room can be planned")
    void acceptsATaskOnAnotherRoom() throws Exception {
        plan("marc", ESTABLISHMENT_A, request(ROOM_A01)).andExpect(status().isCreated());

        plan("marc", ESTABLISHMENT_A, request(ROOM_A02)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A manager cannot plan the cleaning of an accommodation of another campsite")
    void refusesARoomOfAnotherCampsite() throws Exception {
        plan("marc", ESTABLISHMENT_A, request(ROOM_B05))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Room 15 not found"));
    }

    @Test
    @DisplayName("A manager cannot assign a task to an employee of another campsite")
    void refusesAnAssigneeOfAnotherCampsite() throws Exception {
        plan("marc", ESTABLISHMENT_A, """
                {"roomId": %d, "date": "%s", "assigneeId": %d}
                """.formatted(ROOM_A01, DAY, CLAIRE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Staff 4 not found"));

        assertNoTaskWasCreated();
    }

    @Test
    @DisplayName("A campsite never sees the tasks of another one")
    void aTaskIsInvisibleFromAnotherCampsite() throws Exception {
        var location = plan("marc", ESTABLISHMENT_A, request(ROOM_A01))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(authenticated(get(location), "nina", ESTABLISHMENT_B))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Rejects a request without room or date, or with a comment longer than 500 characters")
    void rejectsAnInvalidRequest() throws Exception {
        plan("marc", ESTABLISHMENT_A, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        plan("marc", ESTABLISHMENT_A, """
                {"roomId": %d, "date": "%s", "comment": "%s"}
                """.formatted(ROOM_A01, DAY, "x".repeat(501)))
                .andExpect(status().isBadRequest());

        assertNoTaskWasCreated();
    }

    private String request(Long roomId) {
        return """
                {"roomId": %d, "date": "%s"}
                """.formatted(roomId, DAY);
    }

    private ResultActions plan(String username, Long establishmentId, String body) throws Exception {
        return mockMvc.perform(authenticated(post("/api/cleaning-tasks"), username, establishmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, String username, Long establishmentId) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token(username))
                .header(TenantFilter.ESTABLISHMENT_HEADER, establishmentId);
    }

    private void assertNoTaskWasCreated() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cleaning_task", Integer.class)).isZero();
    }
}
