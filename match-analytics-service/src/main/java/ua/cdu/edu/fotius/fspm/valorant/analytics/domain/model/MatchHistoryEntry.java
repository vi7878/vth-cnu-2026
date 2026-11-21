package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

import java.util.Objects;

/**
 * Value Object representing a player's historical match summary entry.
 */
public record MatchHistoryEntry(MatchId matchId, String mapName, MatchResult result, RatingChange ratingChange) {
    public MatchHistoryEntry {
        Objects.requireNonNull(matchId, "MatchId cannot be null");
        if (mapName == null || mapName.isBlank()) {
            throw new DomainException("INVALID_MAP_NAME", "Map name is required");
        }
        mapName = mapName.trim();
        Objects.requireNonNull(result, "MatchResult cannot be null");
        Objects.requireNonNull(ratingChange, "RatingChange cannot be null");
    }
}
