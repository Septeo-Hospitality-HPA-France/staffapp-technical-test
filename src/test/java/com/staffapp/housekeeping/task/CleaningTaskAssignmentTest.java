package com.staffapp.housekeeping.task;

import static com.staffapp.housekeeping.DemoData.ALICE;
import static com.staffapp.housekeeping.DemoData.BOB;
import static com.staffapp.housekeeping.DemoData.CLAIRE;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_A;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_B;
import static com.staffapp.housekeeping.DemoData.ROOM_A01;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
 * "I can assign it to one of my employees right away or later."
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CleaningTaskAssignmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pms_notification");
        jdbcTemplate.update("DELETE FROM cleaning_task");
        jdbcTemplate.update("UPDATE room SET last_cleaned_at = NULL");
    }

    @Test
    @DisplayName("A manager assigns a planned task to one of their employees, later")
    void aManagerAssignsATaskLater() throws Exception {
        var taskId = planTask();

        assign("marc", ESTABLISHMENT_A, taskId, ALICE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeUsername").value("alice"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("A manager hands a task in progress over to another employee")
    void aManagerReassignsATaskInProgress() throws Exception {
        var taskId = planTask();
        act("alice", taskId, "start");

        assign("marc", ESTABLISHMENT_A, taskId, BOB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeUsername").value("bob"));
    }

    @Test
    @DisplayName("Only managers assign a task, whatever the body")
    void anEmployeeCannotAssignATask() throws Exception {
        var taskId = planTask();

        assign("alice", ESTABLISHMENT_A, taskId, BOB)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(authenticated(patch("/api/cleaning-tasks/{id}", taskId), "alice", ESTABLISHMENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A task cannot be assigned to an employee of another campsite")
    void refusesAnAssigneeOfAnotherCampsite() throws Exception {
        var taskId = planTask();

        assign("marc", ESTABLISHMENT_A, taskId, CLAIRE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Staff 4 not found"));
    }

    @Test
    @DisplayName("A manager cannot assign a task of another campsite")
    void refusesATaskOfAnotherCampsite() throws Exception {
        var taskId = planTask();

        assign("nina", ESTABLISHMENT_B, taskId, CLAIRE).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A done task cannot be reassigned")
    void refusesToReassignADoneTask() throws Exception {
        var taskId = planTask();
        act("alice", taskId, "start");
        act("alice", taskId, "complete");

        assign("marc", ESTABLISHMENT_A, taskId, BOB)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A done task cannot be reassigned"));
    }

    @Test
    @DisplayName("Rejects an assignment without an employee")
    void rejectsAnAssignmentWithoutAnEmployee() throws Exception {
        var taskId = planTask();

        mockMvc.perform(authenticated(patch("/api/cleaning-tasks/{id}", taskId), "marc", ESTABLISHMENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private Long planTask() throws Exception {
        var body = mockMvc.perform(authenticated(post("/api/cleaning-tasks"), "marc", ESTABLISHMENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId": %d, "date": "2026-07-14"}""".formatted(ROOM_A01)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions assign(String username, Long establishmentId, Long taskId, Long assigneeId)
            throws Exception {
        return mockMvc.perform(authenticated(patch("/api/cleaning-tasks/{id}", taskId), username, establishmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"assigneeId": %d}""".formatted(assigneeId)));
    }

    private void act(String username, Long taskId, String action) throws Exception {
        mockMvc.perform(authenticated(post("/api/cleaning-tasks/{id}/{action}", taskId, action), username,
                        ESTABLISHMENT_A))
                .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, String username, Long establishmentId) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token(username))
                .header(TenantFilter.ESTABLISHMENT_HEADER, establishmentId);
    }
}
