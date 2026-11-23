package ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.rest;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import ua.cdu.edu.fotius.fspm.valorant.analytics.application.MatchApplicationService;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerStats;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RosterId;

/**
 * Inbound REST Adapter handling HTTP requests, DTO mapping, and delegation to {@link MatchApplicationService}.
 */
@Controller("/api/v1/matches")
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final MatchApplicationService matchService;

    @Inject
    public MatchController(MatchApplicationService matchService) {
        this.matchService = matchService;
    }

    @Serdeable public record MatchSubmissionRequest(String rosterId, String playerId, String mapName, boolean isWin, int roundMargin, int kills, int deaths, int assists, int combatScore, boolean isAfk) {}
    @Serdeable public record MatchSubmissionResponse(String matchId, int deltaRR, String tierStatus) {}
    @Serdeable public record MatchHistoryItemDto(String matchId, String mapName, String result, int deltaRR, String tierStatus) {}
    @Serdeable public record ErrorDto(String code, String message) {}

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post("/submit")
    public MatchSubmissionResponse submitMatch(@Body MatchSubmissionRequest request,
                                               @Header("Authorization") Optional<String> authHeader) {
        log.info("POST /matches/submit - rosterId={}, playerId={}, map={}, isWin={}, kills={}, deaths={}, assists={}, combatScore={}",
            request.rosterId(), request.playerId(), request.mapName(), request.isWin(),
            request.kills(), request.deaths(), request.assists(), request.combatScore());

        String token = authHeader
            .filter(h -> !h.isBlank() && h.startsWith("Bearer "))
            .orElseThrow(() -> {
                log.warn("POST /matches/submit - missing or invalid Authorization header, rejecting");
                return new HttpStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token");
            });

        PlayerStats stats = new PlayerStats(
            request.roundMargin(),
            request.kills(),
            request.deaths(),
            request.assists(),
            request.combatScore(),
            request.isAfk()
        );

        Match recorded = matchService.submitMatch(
            new RosterId(request.rosterId()),
            new PlayerId(request.playerId()),
            request.mapName(),
            request.isWin(),
            stats,
            token
        );

        return new MatchSubmissionResponse(
            recorded.getId().value(),
            recorded.getRatingChange().deltaRR(),
            recorded.getRatingChange().tierStatus()
        );
    }

    @Get("/history/{playerId}")
    public HttpResponse<List<MatchHistoryItemDto>> getMatchHistory(String playerId) {
        log.info("GET /matches/history/{}", playerId);
        List<MatchHistoryItemDto> history = matchService.getMatchHistory(new PlayerId(playerId)).stream()
            .map(entry -> new MatchHistoryItemDto(
                entry.matchId().value(),
                entry.mapName(),
                entry.result().name(),
                entry.ratingChange().deltaRR(),
                entry.ratingChange().tierStatus()
            ))
            .toList();
        log.info("GET /matches/history/{} - {} matches found", playerId, history.size());
        return HttpResponse.ok(history);
    }

    @Error(exception = DomainException.class)
    public HttpResponse<ErrorDto> handleDomainError(HttpRequest<?> request, DomainException ex) {
        HttpStatus status;
        if ("MATCH_ALREADY_SUBMITTED".equals(ex.getCode()) || "ROSTER_ALREADY_ARCHIVED".equals(ex.getCode())) {
            status = HttpStatus.CONFLICT;
            log.warn("Domain error (conflict): {} - {}", ex.getCode(), ex.getMessage());
        } else if ("MATCH_PERSISTENCE_FAILED".equals(ex.getCode())) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Domain error (persistence failure): {} - {}", ex.getCode(), ex.getMessage());
        } else {
            status = HttpStatus.BAD_REQUEST;
            log.warn("Domain error (bad request): {} - {}", ex.getCode(), ex.getMessage());
        }
        return HttpResponse.status(status).body(new ErrorDto(ex.getCode(), ex.getMessage()));
    }
}
