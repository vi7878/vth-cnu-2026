package ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchResult;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RatingChange;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.repository.MatchRepository;

import java.util.List;
import java.util.UUID;

/**
 * Outbound Persistence Adapter implementing {@link MatchRepository} using JDBI 3 and PostgreSQL.
 */
@Singleton
public class JdbiMatchRepository implements MatchRepository {

    private final Jdbi jdbi;

    @Inject
    public JdbiMatchRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(Match match) {
        jdbi.useTransaction(handle -> {
            handle.createUpdate(
                    "INSERT INTO matches (id, roster_id, map_name, result, round_margin) " +
                    "VALUES (:id, :rid, :map, :res, :margin)")
                .bind("id", match.getId().value())
                .bind("rid", match.getRosterId().value())
                .bind("map", match.getMapName())
                .bind("res", match.getResult().name())
                .bind("margin", match.getStats().roundMargin())
                .execute();

            handle.createUpdate(
                    "INSERT INTO match_players (id, match_id, player_id, kills, deaths, assists, combat_score, delta_rr, tier_status) " +
                    "VALUES (:id, :mid, :pid, :k, :d, :a, :score, :rr, :tier)")
                .bind("id", UUID.randomUUID().toString())
                .bind("mid", match.getId().value())
                .bind("pid", match.getPlayerId().value())
                .bind("k", match.getStats().kills())
                .bind("d", match.getStats().deaths())
                .bind("a", match.getStats().assists())
                .bind("score", match.getStats().combatScore())
                .bind("rr", match.getRatingChange().deltaRR())
                .bind("tier", match.getRatingChange().tierStatus())
                .execute();
        });
    }

    @Override
    public List<MatchHistoryEntry> findHistoryByPlayerId(PlayerId playerId) {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                    "SELECT m.id, m.map_name, m.result, mp.delta_rr, mp.tier_status " +
                    "FROM matches m JOIN match_players mp ON m.id = mp.match_id " +
                    "WHERE mp.player_id = :pid ORDER BY m.played_at DESC")
                .bind("pid", playerId.value())
                .map((rs, ctx) -> new MatchHistoryEntry(
                    new MatchId(rs.getString("id")),
                    rs.getString("map_name"),
                    MatchResult.valueOf(rs.getString("result")),
                    new RatingChange(rs.getInt("delta_rr"), rs.getString("tier_status"))
                ))
                .list()
        );
    }
}
