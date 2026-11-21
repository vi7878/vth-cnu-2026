package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

import java.util.Objects;

/**
 * Aggregate Root representing a recorded competitive match and its calculated rating outcome.
 */
public class Match {
    private final MatchId id;
    private final RosterId rosterId;
    private final PlayerId playerId;
    private final String mapName;
    private final MatchResult result;
    private final PlayerStats stats;
    private final RatingChange ratingChange;

    public Match(
            MatchId id,
            RosterId rosterId,
            PlayerId playerId,
            String mapName,
            MatchResult result,
            PlayerStats stats,
            RatingChange ratingChange) {
        if (mapName == null || mapName.isBlank()) {
            throw new DomainException("INVALID_MAP_NAME", "Map name is required");
        }
        this.id = Objects.requireNonNull(id, "MatchId cannot be null");
        this.rosterId = Objects.requireNonNull(rosterId, "RosterId cannot be null");
        this.playerId = Objects.requireNonNull(playerId, "PlayerId cannot be null");
        this.mapName = mapName.trim();
        this.result = Objects.requireNonNull(result, "MatchResult cannot be null");
        this.stats = Objects.requireNonNull(stats, "PlayerStats cannot be null");
        this.ratingChange = Objects.requireNonNull(ratingChange, "RatingChange cannot be null");
    }

    public static Match record(
            RosterId rosterId,
            PlayerId playerId,
            String mapName,
            boolean isWin,
            PlayerStats stats,
            RatingChange ratingChange) {
        return new Match(
            MatchId.generate(),
            rosterId,
            playerId,
            mapName,
            MatchResult.fromWin(isWin),
            stats,
            ratingChange
        );
    }

    public MatchId getId() { return id; }
    public RosterId getRosterId() { return rosterId; }
    public PlayerId getPlayerId() { return playerId; }
    public String getMapName() { return mapName; }
    public MatchResult getResult() { return result; }
    public PlayerStats getStats() { return stats; }
    public RatingChange getRatingChange() { return ratingChange; }
}
