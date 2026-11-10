package com.staffapp.housekeeping.task;

import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_A;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_B;
import static com.staffapp.housekeeping.DemoData.ROOM_A01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.staffapp.housekeeping.TestJwtFactory;
import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.pms.PmsClient;
import com.staffapp.housekeeping.tenant.TenantFilter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * "As an employee or a manager, I mark a task as started, then as done." and "When a task is done, the accommodation
 * is marked as cleaned and the PMS is notified."
 * <p>
 * Not transactional on purpose: each request commits, as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CleaningTaskWorkflowTest {

    // The PMS is only called by the outbox relay, never during the request
    @MockitoBean
    private PmsClient pmsClient;

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
    @DisplayName("An employee starts a task: it is in progress")
    void anEmployeeStartsATask() throws Exception {
        var taskId = planTask();

        act("alice", ESTABLISHMENT_A, taskId, "start")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    @DisplayName("An employee completes a task in progress: it is done, the room is cleaned, the PMS will be told")
    void anEmployeeCompletesATask() throws Exception {
        var taskId = planTask();
        act("alice", ESTABLISHMENT_A, taskId, "start").andExpect(status().isOk());

        var doneAt = JsonPath.<String>read(act("alice", ESTABLISHMENT_A, taskId, "complete")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andReturn().getResponse().getContentAsString(), "$.doneAt");

        mockMvc.perform(authenticated(get("/api/rooms/" + ROOM_A01), "alice", ESTABLISHMENT_A))
                .andExpect(jsonPath("$.lastCleanedAt").value(doneAt));
        assertThat(pendingNotifications()).singleElement().satisfies(notification -> {
            assertThat(notification.get("task_id")).isEqualTo(taskId);
            assertThat(notification.get("room_id")).isEqualTo(ROOM_A01);
            assertThat(((Timestamp) notification.get("cleaned_at")).toInstant().toString()).isEqualTo(doneAt);
        });
        verifyNoInteractions(pmsClient);
    }

    @Test
    @DisplayName("A manager can also start and complete a task")
    void aManagerCompletesATask() throws Exception {
        var taskId = planTask();

        act("marc", ESTABLISHMENT_A, taskId, "start").andExpect(status().isOk());
        act("marc", ESTABLISHMENT_A, taskId, "complete").andExpect(status().isOk());
    }

    @Test
    @DisplayName("No shortcut: a task to do cannot be completed, the room stays dirty and the PMS is not told")
    void refusesToCompleteATaskNotStarted() throws Exception {
        var taskId = planTask();

        act("alice", ESTABLISHMENT_A, taskId, "complete")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot move a task from TODO to DONE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_cleaned_at FROM room WHERE id = ?", Timestamp.class, ROOM_A01)).isNull();
        assertThat(pendingNotifications()).isEmpty();
    }

    @Test
    @DisplayName("No way back: a task cannot be started twice, nor completed twice, and the PMS is told once")
    void refusesToRepeatATransition() throws Exception {
        var taskId = planTask();
        act("alice", ESTABLISHMENT_A, taskId, "start");
        act("bob", ESTABLISHMENT_A, taskId, "start").andExpect(status().isConflict());

        act("alice", ESTABLISHMENT_A, taskId, "complete").andExpect(status().isOk());
        act("bob", ESTABLISHMENT_A, taskId, "complete").andExpect(status().isConflict());

        assertThat(pendingNotifications()).hasSize(1);
    }

    @Test
    @DisplayName("A campsite cannot start or complete the tasks of another one")
    void refusesATaskOfAnotherCampsite() throws Exception {
        var taskId = planTask();

        act("claire", ESTABLISHMENT_B, taskId, "start").andExpect(status().isNotFound());
        act("nina", ESTABLISHMENT_B, taskId, "complete").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Once the task is done, the room can be planned again")
    void aRoomCanBePlannedAgainOnceItsTaskIsDone() throws Exception {
        var taskId = planTask();
        act("alice", ESTABLISHMENT_A, taskId, "start");
        act("alice", ESTABLISHMENT_A, taskId, "complete");

        planTask();
    }

    private Long planTask() throws Exception {
        var response = mockMvc.perform(authenticated(post("/api/cleaning-tasks"), "marc", ESTABLISHMENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId": %d, "date": "2026-07-14"}
                                """.formatted(ROOM_A01)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions act(String username, Long establishmentId, Long taskId, String action) throws Exception {
        return mockMvc.perform(authenticated(
                post("/api/cleaning-tasks/{id}/{action}", taskId, action), username, establishmentId));
    }

    private static MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, String username, Long establishmentId) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token(username))
                .header(TenantFilter.ESTABLISHMENT_HEADER, establishmentId);
    }

    private List<Map<String, Object>> pendingNotifications() {
        return jdbcTemplate.queryForList("SELECT * FROM pms_notification WHERE sent_at IS NULL");
    }
}
