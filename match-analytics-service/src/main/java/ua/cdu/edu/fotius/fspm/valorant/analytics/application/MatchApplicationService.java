package ua.cdu.edu.fotius.fspm.valorant.analytics.application;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerStats;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RatingChange;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RosterId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.repository.MatchRepository;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RankCalculatorClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RosterServiceClient;

import java.util.List;

/**
 * Application Service orchestrating Match Analytics use cases (roster verification,
 * rating calculation, roster archival, and persistence).
 */
@Singleton
public class MatchApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MatchApplicationService.class);

    private final MatchRepository matchRepository;
    private final RankCalculatorClient rankClient;
    private final RosterServiceClient rosterClient;

    @Inject
    public MatchApplicationService(
            MatchRepository matchRepository,
            RankCalculatorClient rankClient,
            RosterServiceClient rosterClient) {
        this.matchRepository = matchRepository;
        this.rankClient = rankClient;
        this.rosterClient = rosterClient;
    }

    public Match submitMatch(
            RosterId rosterId,
            PlayerId playerId,
            String mapName,
            boolean isWin,
            PlayerStats stats,
            String authToken) {

        verifyRosterReadyForSubmission(rosterId, authToken);

        var rankRes = rankClient.calculateRating(new RankCalculatorClient.RankCalculationRequest(
            isWin,
            stats.roundMargin(),
            stats.kills(),
            stats.deaths(),
            stats.assists(),
            stats.combatScore(),
            stats.isAfk()
        ));
        log.info("MatchApplicationService.submitMatch - rank calc: deltaRR={}, tierStatus={}",
            rankRes.deltaRR(), rankRes.tierStatus());

        var ratingChange = new RatingChange(rankRes.deltaRR(), rankRes.tierStatus());
        Match match = Match.record(rosterId, playerId, mapName, isWin, stats, ratingChange);

        archiveRosterForMatch(rosterId, authToken);

        try {
            matchRepository.save(match);
        } catch (Exception e) {
            // Compensate: unarchive roster so the match submission can be retried
            try {
                rosterClient.unarchiveRoster(rosterId.value(), authToken);
                log.info("MatchApplicationService.submitMatch - compensated: roster {} unarchived", rosterId.value());
            } catch (Exception ignored) {
                log.warn("MatchApplicationService.submitMatch - compensation failed for roster {}: {}",
                    rosterId.value(), ignored.getMessage());
            }
            throw new DomainException("MATCH_PERSISTENCE_FAILED", "Failed to persist match: " + e.getMessage());
        }

        log.info("MatchApplicationService.submitMatch - match recorded: matchId={}", match.getId().value());
        return match;
    }

    public List<MatchHistoryEntry> getMatchHistory(PlayerId playerId) {
        return matchRepository.findHistoryByPlayerId(playerId);
    }

    private void verifyRosterReadyForSubmission(RosterId rosterId, String authToken) {
        try {
            var roster = rosterClient.getRoster(rosterId.value(), authToken);
            if ("ARCHIVED".equals(roster.status())) {
                log.warn("Roster {} is ARCHIVED (already submitted), rejecting", rosterId.value());
                throw new DomainException(
                    "MATCH_ALREADY_SUBMITTED",
                    "Match already submitted for roster " + rosterId.value()
                );
            }
            if (!"LOCKED".equals(roster.status())) {
                log.warn("Roster {} is {} (not LOCKED), rejecting", rosterId.value(), roster.status());
                throw new DomainException(
                    "ROSTER_NOT_LOCKED",
                    "Roster must be LOCKED before match submission (current status: " + roster.status() + ")"
                );
            }
        } catch (DomainException e) {
            throw e;
        } catch (HttpClientResponseException e) {
            if (e.getStatus().getCode() == 404) {
                throw new DomainException(
                    "ROSTER_VERIFICATION_FAILED",
                    "Roster not found for roster " + rosterId.value()
                );
            }
            throw new DomainException(
                "ROSTER_VERIFICATION_FAILED",
                "Could not verify roster status for roster " + rosterId.value() + ": " + e.getMessage()
            );
        } catch (Exception e) {
            throw new DomainException(
                "ROSTER_VERIFICATION_FAILED",
                "Could not verify roster status for roster " + rosterId.value() + ": " + e.getMessage()
            );
        }
    }

    private void archiveRosterForMatch(RosterId rosterId, String authToken) {
        try {
            rosterClient.archiveRoster(rosterId.value(), authToken);
            log.info("Roster {} archived", rosterId.value());
        } catch (HttpClientResponseException e) {
            if (e.getStatus().getCode() == 409) {
                log.warn("Roster {} already archived (409), client inconsistent", rosterId.value());
                throw new DomainException(
                    "MATCH_ALREADY_SUBMITTED",
                    "Match already submitted for roster " + rosterId.value()
                );
            }
            log.warn("Failed to archive roster {}: {}", rosterId.value(), e.getMessage());
            throw new DomainException(
                "ROSTER_ARCHIVE_FAILED",
                "Failed to archive roster " + rosterId.value() + ": " + e.getMessage()
            );
        } catch (Exception e) {
            log.warn("Failed to archive roster {}: {}", rosterId.value(), e.getMessage());
            throw new DomainException(
                "ROSTER_ARCHIVE_FAILED",
                "Failed to archive roster " + rosterId.value() + ": " + e.getMessage()
            );
        }
    }
}
