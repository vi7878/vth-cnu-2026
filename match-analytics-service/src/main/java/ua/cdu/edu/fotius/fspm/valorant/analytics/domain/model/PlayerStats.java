package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

/**
 * Value Object encapsulating player performance statistics in a match and enforcing non-negative invariants.
 */
public record PlayerStats(int roundMargin, int kills, int deaths, int assists, int combatScore, boolean isAfk) {
    public PlayerStats {
        if (roundMargin < 0) {
            throw new DomainException("INVALID_ROUND_MARGIN", "Round margin cannot be negative");
        }
        if (kills < 0 || deaths < 0 || assists < 0) {
            throw new DomainException("INVALID_KDA", "Kills, deaths, and assists cannot be negative");
        }
        if (combatScore < 0) {
            throw new DomainException("INVALID_COMBAT_SCORE", "Combat score cannot be negative");
        }
    }
}
