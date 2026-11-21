package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.repository;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;

import java.util.List;

/**
 * Outbound Repository Port for persisting and querying Match aggregates.
 */
public interface MatchRepository {
    void save(Match match);
    List<MatchHistoryEntry> findHistoryByPlayerId(PlayerId playerId);
}
