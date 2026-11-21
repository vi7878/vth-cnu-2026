package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

import java.util.UUID;

/**
 * Value Object representing a unique Match identifier.
 */
public record MatchId(String value) {
    public MatchId {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_MATCH_ID", "MatchId cannot be null or blank");
        }
        value = value.trim();
    }

    public static MatchId generate() {
        return new MatchId(UUID.randomUUID().toString());
    }
}
