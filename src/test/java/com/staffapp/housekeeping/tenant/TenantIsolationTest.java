package com.staffapp.housekeeping.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.staffapp.housekeeping.DemoData;
import com.staffapp.housekeeping.TestJwtFactory;
import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.security.SecurityConfiguration;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The establishment header must match the establishment of the token: a campsite never sees another one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Rejects a staff member asking for another establishment than the one of their token")
    void rejectsAStaffMemberAskingForAnotherEstablishment() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_B))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Rejects a manager asking for another establishment: the role grants no cross-establishment access")
    void rejectsAManagerAskingForAnotherEstablishment() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("marc"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_B))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Still rejects a mismatched establishment when the client does not accept problem details")
    void rejectsAMismatchedEstablishmentWhateverTheAcceptHeader() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .accept(MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_B))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "establishment_id = {0}")
    @MethodSource("nonIntegerEstablishmentClaims")
    @DisplayName("Rejects a token whose establishment claim is not an integer, instead of converting it")
    void rejectsANonIntegerEstablishmentClaim(Object claim) throws Exception {
        var token = TestJwtFactory.tokenWithClaims(Map.of(
                "sub", "alice",
                SecurityConfiguration.ROLE_CLAIM, "STAFF",
                SecurityConfiguration.ESTABLISHMENT_CLAIM, claim));
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_A))
                .andExpect(status().isForbidden());
    }

    static Stream<Object> nonIntegerEstablishmentClaims() {
        return Stream.of("1", 1.9);
    }

    @Test
    @DisplayName("Lists only the rooms of the establishment of the token")
    void listsOnlyTheRoomsOfTheEstablishmentOfTheToken() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("claire"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].number").value("B01"))
                .andExpect(jsonPath("$[9].number").value("B10"));
    }

    @Test
    @DisplayName("Rejects a request without the establishment header")
    void rejectsARequestWithoutTheEstablishmentHeader() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Rejects a malformed establishment header")
    void rejectsAMalformedEstablishmentHeader() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.token("alice"))
                        .header(TenantFilter.ESTABLISHMENT_HEADER, "1 OR 1=1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Authenticates before checking the establishment: no token is a 401, whatever the header")
    void authenticatesBeforeCheckingTheEstablishment() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(TenantFilter.ESTABLISHMENT_HEADER, DemoData.ESTABLISHMENT_B))
                .andExpect(status().isUnauthorized());
    }
}
