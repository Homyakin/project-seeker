package ru.homyakin.seeker.game.event.pve;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.BattlePersonage;

class PveEquipmentPowerSumTest {
    @Test
    void ascendingSumPreservesTheExistingMyconidRoundingAndIsIndependentOfInputOrder() {
        final var participants = List.of(personage(1e16), personage(1), personage(1));
        final double expected = 10_000_000_000_000_002.0;

        Assertions.assertEquals(expected, PveEquipmentPowerSum.sum(participants));
        Assertions.assertEquals(Double.doubleToRawLongBits(expected),
            Double.doubleToRawLongBits(PveEquipmentPowerSum.sum(participants.reversed())));
        Assertions.assertEquals("EQUIPMENT_POWER_SUM_V1", PveEquipmentPowerSum.VERSION);
    }

    @Test
    void emptyAndZeroAssessmentsHaveZeroSum() {
        Assertions.assertEquals(0, PveEquipmentPowerSum.sum(List.of()));
        Assertions.assertEquals(0, PveEquipmentPowerSum.sum(List.of(personage(0), personage(0))));
    }

    @Test
    void missingListOrParticipantIsRejectedExplicitly() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveEquipmentPowerSum.sum(null));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveEquipmentPowerSum.sum(Arrays.asList(personage(1), null)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidIndividualAssessmentCannotBeSummedOrReplacedByLegacyPower(double power) {
        final var participant = personage(power);
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveEquipmentPowerSum.sum(List.of(participant)));
        Mockito.verify(participant, Mockito.never()).legacyPower();
    }

    @Test
    void individuallyFiniteAssessmentsStillRejectSumOverflow() {
        final var error = Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveEquipmentPowerSum.sum(List.of(personage(Double.MAX_VALUE), personage(Double.MAX_VALUE))));
        Assertions.assertTrue(error.getMessage().contains("sum"));
    }

    private static BattlePersonage personage(double power) {
        final var participant = Mockito.mock(BattlePersonage.class);
        Mockito.when(participant.equipmentPowerUnscaled()).thenReturn(power);
        return participant;
    }
}
