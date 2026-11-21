package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

/**
 * Value Object representing a Player identifier.
 */
public record PlayerId(String value) {
    public PlayerId {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_PLAYER_ID", "PlayerId cannot be null or blank");
        }
        value = value.trim();
    }
}
