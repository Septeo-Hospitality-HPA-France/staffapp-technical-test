package com.staffapp.housekeeping.task;

import static com.staffapp.housekeeping.DemoData.ALICE;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_A;
import static com.staffapp.housekeeping.DemoData.ESTABLISHMENT_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.staffapp.housekeeping.TestJwtFactory;
import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.tenant.TenantFilter;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Date;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * "As an employee or a manager, I see the cleaning tasks of my campsite, for a given day or by status. In high season,
 * a campsite has several hundred tasks a day."
 */
@SpringBootTest(properties = {
        // To count the SQL statements of a request
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CleaningTaskListTest {

    private static final LocalDate DAY = LocalDate.parse("2026-07-14");
    private static final LocalDate NEXT_DAY = DAY.plusDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Les Pins: A01 in progress, A02 done and A03 to do (assigned to alice) on DAY, A04 in progress on NEXT_DAY.
     * La Plage: B01 to do on DAY.
     */
    @BeforeEach
    void insertTasks() {
        insertTask(ESTABLISHMENT_A, 3L, DAY, "TODO", ALICE);
        insertTask(ESTABLISHMENT_A, 1L, DAY, "IN_PROGRESS", null);
        insertTask(ESTABLISHMENT_A, 2L, DAY, "DONE", null);
        insertTask(ESTABLISHMENT_A, 4L, NEXT_DAY, "IN_PROGRESS", null);
        insertTask(ESTABLISHMENT_B, 11L, DAY, "TODO", null);
    }

    @AfterEach
    void deleteTasks() {
        jdbcTemplate.update("DELETE FROM pms_notification");
        jdbcTemplate.update("DELETE FROM cleaning_task");
    }

    @Test
    @DisplayName("Lists the tasks of a day in the campsite, by room number, never those of another campsite")
    void listsTheTasksOfADay() throws Exception {
        list("alice", "date=" + DAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].roomNumber", contains("A01", "A02", "A03")))
                .andExpect(jsonPath("$.content[2].assigneeUsername").value("alice"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("Lists the tasks by status, whatever their day, by day then room number")
    void listsTheTasksByStatus() throws Exception {
        list("marc", "status=IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].roomNumber", contains("A01", "A04")));
    }

    @Test
    @DisplayName("Lists the tasks of a day with a given status")
    void listsTheTasksOfADayWithAStatus() throws Exception {
        list("alice", "date=" + DAY + "&status=TODO")
                .andExpect(jsonPath("$.content[*].roomNumber", contains("A03")));
    }

    @Test
    @DisplayName("Another campsite only sees its own tasks")
    void anotherCampsiteSeesOnlyItsTasks() throws Exception {
        mockMvc.perform(get("/api/cleaning-tasks?date=" + DAY)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("nina"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, ESTABLISHMENT_B))
                .andExpect(jsonPath("$.content[*].roomNumber", contains("B01")));
    }

    @Test
    @DisplayName("Pages the tasks: several hundred a day in high season")
    void pagesTheTasks() throws Exception {
        list("alice", "date=" + DAY + "&size=2&page=1")
                .andExpect(jsonPath("$.content[*].roomNumber", contains("A03")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Reads a page in two SQL statements, whatever its size: the page and the count, no query per task")
    void readsAPageWithoutAQueryPerTask() throws Exception {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        list("alice", "date=" + DAY).andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Requires a day or a status")
    void requiresADayOrAStatus() throws Exception {
        list("alice", "")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Rejects an unknown status")
    void rejectsAnUnknownStatus() throws Exception {
        list("alice", "status=CLEAN").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rejects a page larger than 100 tasks")
    void rejectsAPageTooLarge() throws Exception {
        list("alice", "date=" + DAY + "&size=101")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    private ResultActions list(String username, String query) throws Exception {
        return mockMvc.perform(get("/api/cleaning-tasks?" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token(username))
                .header(TenantFilter.ESTABLISHMENT_HEADER, ESTABLISHMENT_A));
    }

    private void insertTask(Long establishmentId, Long roomId, LocalDate day, String status, Long assigneeId) {
        var started = status.equals("TODO") ? null : "now()";
        var done = status.equals("DONE") ? "now()" : null;
        jdbcTemplate.update("""
                INSERT INTO cleaning_task (establishment_id, room_id, assignee_id, scheduled_on, status, created_by,
                                           created_at, started_at, done_at)
                VALUES (?, ?, ?, ?, ?, 'marc', now(), %s, %s)""".formatted(started, done),
                establishmentId, roomId, assigneeId, Date.valueOf(day), status);
    }
}
