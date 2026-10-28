package com.staffapp.housekeeping.room;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.staffapp.housekeeping.DemoData;
import com.staffapp.housekeeping.TestJwtFactory;
import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.tenant.TenantFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Lists the rooms of the current establishment ordered by number")
    void listsTheRoomsOfTheCurrentEstablishment() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].number").value("A01"))
                .andExpect(jsonPath("$[9].number").value("A10"));
    }

    @Test
    @DisplayName("Returns a room by id")
    void returnsARoomById() throws Exception {
        mockMvc.perform(get("/api/rooms/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("A01"));
    }

    @Test
    @DisplayName("Rejects a request without a token")
    void rejectsARequestWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_A))
                .andExpect(status().isUnauthorized());
    }
}
