package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

/**
 * Value Object representing a Roster identifier.
 */
public record RosterId(String value) {
    public RosterId {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_ROSTER_ID", "RosterId cannot be null or blank");
        }
        value = value.trim();
    }
}
