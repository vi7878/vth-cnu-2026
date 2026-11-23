package ua.cdu.edu.fotius.fspm.valorant.analytics;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ua.cdu.edu.fotius.fspm.valorant.analytics.application.MatchApplicationService;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchResult;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerStats;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RatingChange;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RosterId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.rest.MatchController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class MatchControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    MatchApplicationService matchApplicationService;

    @MockBean(MatchApplicationService.class)
    MatchApplicationService mockMatchApplicationService() {
        return Mockito.mock(MatchApplicationService.class);
    }

    @Test
    @DisplayName("GIVEN valid match submission WHEN posted THEN returns 200 OK with MatchSubmissionResponse")
    void testSubmitMatchReturns200() {
        var recorded = new Match(
            new MatchId("m-101"),
            new RosterId("r-101"),
            new PlayerId("p-001"),
            "Ascent",
            MatchResult.WIN,
            new PlayerStats(5, 22, 12, 5, 310, false),
            new RatingChange(25, "PROMOTED")
        );
        Mockito.when(matchApplicationService.submitMatch(any(), any(), anyString(), anyBoolean(), any(), anyString()))
            .thenReturn(recorded);

        var payload = new MatchController.MatchSubmissionRequest(
            "r-101", "p-001", "Ascent", true, 5, 22, 12, 5, 310, false
        );
        var req = HttpRequest.POST("/api/v1/matches/submit", payload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var res = client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
        assertEquals("m-101", res.body().matchId());
        assertEquals(25, res.body().deltaRR());
        assertEquals("PROMOTED", res.body().tierStatus());
    }

    @Test
    @DisplayName("GIVEN ROSTER_NOT_LOCKED domain error WHEN match submitted THEN 400 Bad Request")
    void testSubmitMatchNotLockedReturns400() {
        Mockito.when(matchApplicationService.submitMatch(any(), any(), anyString(), anyBoolean(), any(), anyString()))
            .thenThrow(new DomainException("ROSTER_NOT_LOCKED", "Roster must be LOCKED before match submission"));

        var payload = new MatchController.MatchSubmissionRequest(
            "r-101", "p-001", "Ascent", true, 5, 22, 12, 5, 310, false
        );
        var req = HttpRequest.POST("/api/v1/matches/submit", payload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN MATCH_ALREADY_SUBMITTED domain error WHEN match submitted THEN 409 Conflict")
    void testSubmitMatchAlreadySubmittedReturns409() {
        Mockito.when(matchApplicationService.submitMatch(any(), any(), anyString(), anyBoolean(), any(), anyString()))
            .thenThrow(new DomainException("MATCH_ALREADY_SUBMITTED", "Match already submitted for roster r-101"));

        var payload = new MatchController.MatchSubmissionRequest(
            "r-101", "p-001", "Ascent", true, 5, 22, 12, 5, 310, false
        );
        var req = HttpRequest.POST("/api/v1/matches/submit", payload)
            .header("Authorization", "Bearer jwt-token-for-ProGamer#EUW");
        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN missing Authorization header WHEN match submitted THEN 401 Unauthorized")
    void testSubmitMatchWithoutTokenReturns401() {
        var payload = new MatchController.MatchSubmissionRequest(
            "r-101", "p-001", "Ascent", true, 5, 22, 12, 5, 310, false
        );
        var req = HttpRequest.POST("/api/v1/matches/submit", payload);
        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("GIVEN player history WHEN requested THEN returns 200 OK with mapped DTOs")
    void testGetMatchHistoryReturns200() {
        Mockito.when(matchApplicationService.getMatchHistory(new PlayerId("p-001")))
            .thenReturn(List.of(new MatchHistoryEntry(
                new MatchId("m-101"),
                "Ascent",
                MatchResult.WIN,
                new RatingChange(25, "PROMOTED")
            )));

        var req = HttpRequest.GET("/api/v1/matches/history/p-001");
        var res = client.toBlocking().exchange(req, MatchController.MatchHistoryItemDto[].class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
        assertEquals(1, res.body().length);
        assertEquals("m-101", res.body()[0].matchId());
        assertEquals("Ascent", res.body()[0].mapName());
        assertEquals("WIN", res.body()[0].result());
        assertEquals(25, res.body()[0].deltaRR());
        assertEquals("PROMOTED", res.body()[0].tierStatus());
    }
}
