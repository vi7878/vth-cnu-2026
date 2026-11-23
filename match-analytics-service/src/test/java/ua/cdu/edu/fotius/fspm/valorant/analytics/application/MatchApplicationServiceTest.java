package ua.cdu.edu.fotius.fspm.valorant.analytics.application;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RankCalculatorClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RosterServiceClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.rest.MatchController;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class MatchApplicationServiceTest {

    private static final String TEST_ROSTER_ID = "r-test-analytics";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    Jdbi jdbi;

    @Inject
    RankCalculatorClient rankCalculatorClient;

    @Inject
    RosterServiceClient rosterServiceClient;

    @BeforeEach
    @AfterEach
    void cleanupTestMatches() {
        jdbi.useHandle(handle ->
            handle.createUpdate("DELETE FROM matches WHERE roster_id = :rid")
                .bind("rid", TEST_ROSTER_ID)
                .execute()
        );
    }

    @MockBean(RankCalculatorClient.class)
    RankCalculatorClient mockRankCalculatorClient() {
        return Mockito.mock(RankCalculatorClient.class);
    }

    @MockBean(RosterServiceClient.class)
    RosterServiceClient mockRosterServiceClient() {
        return Mockito.mock(RosterServiceClient.class);
    }

    @Test
    @DisplayName("GIVEN match submission WHEN posted THEN calls RankCalculator and persists to DB")
    void testMatchSubmissionWithMockedInterService() {
        Mockito.when(rankCalculatorClient.calculateRating(any()))
                .thenReturn(new RankCalculatorClient.RankCalculationResponse(25, "PROMOTED"));
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "LOCKED", null, null));

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var res = client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
        assertEquals(25, res.body().deltaRR());
        assertEquals("PROMOTED", res.body().tierStatus());
    }

    @Test
    @DisplayName("GIVEN DRAFT roster WHEN match submitted THEN 400 Bad Request")
    void testMatchSubmissionDraftRosterReturns400() {
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "DRAFT", null, null));

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN ARCHIVED roster WHEN match submitted THEN 409 Conflict (already submitted)")
    void testMatchSubmissionArchivedRosterReturns409() {
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "ARCHIVED", null, null));

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN archive returns 409 (race condition) WHEN match submitted THEN 409 Conflict")
    void testMatchSubmissionArchiveRaceConditionReturns409() {
        Mockito.when(rankCalculatorClient.calculateRating(any()))
                .thenReturn(new RankCalculatorClient.RankCalculationResponse(25, "PROMOTED"));
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "LOCKED", null, null));
        Mockito.doThrow(new HttpClientResponseException("Already archived",
                HttpResponse.status(HttpStatus.CONFLICT)))
                .when(rosterServiceClient).archiveRoster(anyString(), anyString());

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN archive fails (non-409) WHEN match submitted THEN 400 Bad Request")
    void testMatchSubmissionArchiveFailureReturns400() {
        Mockito.when(rankCalculatorClient.calculateRating(any()))
                .thenReturn(new RankCalculatorClient.RankCalculationResponse(25, "PROMOTED"));
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "LOCKED", null, null));
        Mockito.doThrow(new HttpClientResponseException("Server error",
                HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)))
                .when(rosterServiceClient).archiveRoster(anyString(), anyString());

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN roster service throws exception WHEN match submitted THEN 400 Bad Request (fail-fast)")
    void testMatchSubmissionRosterServiceThrowsReturns400() {
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenThrow(new RuntimeException("Roster service unavailable"));

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN match submission WHEN posted THEN persists to DB and appears in history")
    void testMatchSubmissionPersistsToDbAndHistory() {
        Mockito.when(rankCalculatorClient.calculateRating(any()))
                .thenReturn(new RankCalculatorClient.RankCalculationResponse(20, "MAINTAINED"));
        Mockito.when(rosterServiceClient.getRoster(anyString(), anyString()))
                .thenReturn(new RosterServiceClient.RosterResponse(TEST_ROSTER_ID, "p-001", "Ascent", "LOCKED", null, null));

        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-hist-1", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var res = client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
        assertEquals(20, res.body().deltaRR());
        assertEquals("MAINTAINED", res.body().tierStatus());

        var histReq = HttpRequest.GET("/api/v1/matches/history/p-hist-1");
        var histRes = client.toBlocking().exchange(histReq, MatchController.MatchHistoryItemDto[].class);
        assertEquals(HttpStatus.OK, histRes.getStatus());
        assertNotNull(histRes.body());
        assertTrue(histRes.body().length >= 1);
        assertEquals("Ascent", histRes.body()[0].mapName());
        assertEquals("WIN", histRes.body()[0].result());
        assertEquals(20, histRes.body()[0].deltaRR());
        assertEquals("MAINTAINED", histRes.body()[0].tierStatus());
    }

    @Test
    @DisplayName("GIVEN no auth token WHEN match submitted THEN 401 Unauthorized")
    void testMatchSubmissionWithoutTokenReturns401() {
        var reqPayload = new MatchController.MatchSubmissionRequest(TEST_ROSTER_ID, "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload);
        var ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN match history WHEN requested THEN returns list of past matches")
    void testMatchHistoryReturnsList() {
        var req = HttpRequest.GET("/api/v1/matches/history/p-001");
        var res = client.toBlocking().exchange(req, MatchController.MatchHistoryItemDto[].class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
    }
}
