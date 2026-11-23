package ua.cdu.edu.fotius.fspm.valorant.analytics.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchResult;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerStats;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RatingChange;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RosterId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pure Domain Unit Tests: Match Aggregate & Value Objects")
class MatchAggregateTest {

    @Test
    @DisplayName("GIVEN negative roundMargin WHEN creating PlayerStats THEN throws INVALID_ROUND_MARGIN")
    void shouldRejectNegativeRoundMargin() {
        DomainException ex = assertThrows(DomainException.class,
            () -> new PlayerStats(-1, 15, 10, 4, 240, false));
        assertEquals("INVALID_ROUND_MARGIN", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN negative KDA values WHEN creating PlayerStats THEN throws INVALID_KDA")
    void shouldRejectNegativeKda() {
        DomainException exKills = assertThrows(DomainException.class,
            () -> new PlayerStats(3, -1, 10, 4, 240, false));
        assertEquals("INVALID_KDA", exKills.getCode());

        DomainException exDeaths = assertThrows(DomainException.class,
            () -> new PlayerStats(3, 15, -2, 4, 240, false));
        assertEquals("INVALID_KDA", exDeaths.getCode());

        DomainException exAssists = assertThrows(DomainException.class,
            () -> new PlayerStats(3, 15, 10, -1, 240, false));
        assertEquals("INVALID_KDA", exAssists.getCode());
    }

    @Test
    @DisplayName("GIVEN negative combatScore WHEN creating PlayerStats THEN throws INVALID_COMBAT_SCORE")
    void shouldRejectNegativeCombatScore() {
        DomainException ex = assertThrows(DomainException.class,
            () -> new PlayerStats(3, 15, 10, 4, -50, false));
        assertEquals("INVALID_COMBAT_SCORE", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN blank mapName WHEN recording Match THEN throws INVALID_MAP_NAME")
    void shouldRejectBlankMapName() {
        var stats = new PlayerStats(5, 22, 12, 5, 310, false);
        var rating = new RatingChange(25, "PROMOTED");

        DomainException ex = assertThrows(DomainException.class,
            () -> Match.record(new RosterId("r-101"), new PlayerId("p-001"), "   ", true, stats, rating));
        assertEquals("INVALID_MAP_NAME", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN blank tierStatus WHEN creating RatingChange THEN throws INVALID_TIER_STATUS")
    void shouldRejectBlankTierStatus() {
        DomainException ex = assertThrows(DomainException.class,
            () -> new RatingChange(20, " "));
        assertEquals("INVALID_TIER_STATUS", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN valid inputs WHEN recording Match THEN sets fields and trims mapName")
    void shouldRecordValidMatch() {
        var stats = new PlayerStats(5, 22, 12, 5, 310, false);
        var rating = new RatingChange(25, " PROMOTED ");

        Match match = Match.record(new RosterId(" r-101 "), new PlayerId(" p-001 "), " Ascent ", true, stats, rating);

        assertNotNull(match.getId());
        assertEquals("r-101", match.getRosterId().value());
        assertEquals("p-001", match.getPlayerId().value());
        assertEquals("Ascent", match.getMapName());
        assertEquals(MatchResult.WIN, match.getResult());
        assertEquals(25, match.getRatingChange().deltaRR());
        assertEquals("PROMOTED", match.getRatingChange().tierStatus());
    }

    @Test
    @DisplayName("GIVEN two identical Value Objects WHEN compared THEN are equal by value")
    void shouldCompareValueObjectsByValue() {
        var stats1 = new PlayerStats(5, 20, 10, 5, 280, false);
        var stats2 = new PlayerStats(5, 20, 10, 5, 280, false);
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());

        var id1 = new MatchId("m-1");
        var id2 = new MatchId("  m-1 ");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}
