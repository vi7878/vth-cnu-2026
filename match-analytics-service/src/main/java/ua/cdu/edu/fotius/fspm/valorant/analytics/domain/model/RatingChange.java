package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

/**
 * Value Object representing the Rank Rating delta and resulting tier status after a match.
 */
public record RatingChange(int deltaRR, String tierStatus) {
    public RatingChange {
        if (tierStatus == null || tierStatus.isBlank()) {
            throw new DomainException("INVALID_TIER_STATUS", "Tier status cannot be null or blank");
        }
        tierStatus = tierStatus.trim();
    }
}
