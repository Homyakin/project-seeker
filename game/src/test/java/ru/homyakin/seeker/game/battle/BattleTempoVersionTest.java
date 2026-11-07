package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BattleTempoVersionTest {
    @ParameterizedTest
    @CsvSource({"0, 0", "1, 2", "2, 4", "3, 5", "4, 7", "101, 177", "102, 179"})
    void acceleratedPacketsUseExactSevenQuartersWithHalfUpRounding(int damage, int expected) {
        Assertions.assertEquals(expected, BattleTempoVersion.ACCELERATED_V1.scaleDamage(damage));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 101, 1_227_133_512, Integer.MAX_VALUE})
    void baselinePacketsRemainUnchanged(int damage) {
        Assertions.assertEquals(damage, BattleTempoVersion.BASELINE_V1.scaleDamage(damage));
    }

    @Test
    void acceleratedPacketsRejectOverflowInsteadOfWrapping() {
        Assertions.assertEquals(
            Integer.MAX_VALUE - 1,
            BattleTempoVersion.ACCELERATED_V1.scaleDamage(1_227_133_512)
        );
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> BattleTempoVersion.ACCELERATED_V1.scaleDamage(1_227_133_513)
        );
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> BattleTempoVersion.ACCELERATED_V1.scaleDamage(Integer.MAX_VALUE)
        );
    }

    @Test
    void neitherVersionAcceptsNegativeDamage() {
        for (final var version : BattleTempoVersion.values()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> version.scaleDamage(-1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> version.scaleDamage(Integer.MIN_VALUE));
        }
    }

    @Test
    void expectedDamageMultipliersMatchTheSelectedTempo() {
        Assertions.assertEquals(1.0, BattleTempoVersion.BASELINE_V1.damageMultiplier());
        Assertions.assertEquals(1.75, BattleTempoVersion.ACCELERATED_V1.damageMultiplier());
    }

    @Test
    void historicalSnapshotConstructorAndNullTempoPreserveBaseline() {
        Assertions.assertEquals(
            BattleTempoVersion.BASELINE_V1,
            new BattleInitState(List.of(), Map.of()).tempoVersion()
        );
        Assertions.assertEquals(
            BattleTempoVersion.BASELINE_V1,
            new BattleInitState(List.of(), Map.of(), null).tempoVersion()
        );
    }

    @Test
    void historicalJsonWithoutTempoAndExplicitNullDeserializeAsBaseline() throws Exception {
        final var mapper = new ObjectMapper();
        for (final var json : List.of(
            "{\"lines\":[],\"personagesById\":{}}",
            "{\"lines\":[],\"personagesById\":{},\"tempoVersion\":null}"
        )) {
            Assertions.assertEquals(
                BattleTempoVersion.BASELINE_V1,
                mapper.readValue(json, BattleInitState.class).tempoVersion()
            );
        }
    }

    @Test
    void explicitlyAcceleratedSnapshotRoundTripsWithoutChangingOtherFields() throws Exception {
        final var mapper = new ObjectMapper();
        final var snapshot = new BattleInitState(List.of(), Map.of(), BattleTempoVersion.ACCELERATED_V1);

        final var json = mapper.writeValueAsString(snapshot);

        Assertions.assertEquals(snapshot, mapper.readValue(json, BattleInitState.class));
        Assertions.assertEquals("ACCELERATED_V1", mapper.readTree(json).get("tempoVersion").asText());
    }
}
