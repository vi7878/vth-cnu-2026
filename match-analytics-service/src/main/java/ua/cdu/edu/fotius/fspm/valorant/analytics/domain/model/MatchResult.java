package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

/**
 * Outcome of a competitive match.
 */
public enum MatchResult {
    WIN,
    LOSS;

    public static MatchResult fromWin(boolean isWin) {
        return isWin ? WIN : LOSS;
    }

    public boolean isWin() {
        return this == WIN;
    }
}
