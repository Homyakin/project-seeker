package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SeededBattleRandomTest {
    @Test
    void sameSeedAndSequenceAreReproducible() {
        final var first = values(new SeededBattleRandom(42), "critical");
        final var second = values(new SeededBattleRandom(42), "critical");

        Assertions.assertEquals(first, second);
    }

    @Test
    void drawFromAnotherSequenceDoesNotShiftResult() {
        final var withUnrelatedRoll = new SeededBattleRandom(42);
        withUnrelatedRoll.nextInt("unused-skill", 1, 10_000);

        Assertions.assertEquals(
            values(new SeededBattleRandom(42), "target"),
            values(withUnrelatedRoll, "target")
        );
    }

    @Test
    void chanceUsesBasisPoints() {
        final BattleRandom minimum = (sequence, min, max) -> min;
        final BattleRandom maximum = (sequence, min, max) -> max;

        Assertions.assertTrue(minimum.chance("skill", 1));
        Assertions.assertFalse(maximum.chance("skill", 9_999));
        Assertions.assertFalse(minimum.chance("skill", 0));
        Assertions.assertTrue(maximum.chance("skill", 10_000));
    }

    private static ArrayList<Integer> values(BattleRandom random, String sequence) {
        final var values = new ArrayList<Integer>();
        for (int i = 0; i < 20; i++) {
            values.add(random.nextInt(sequence, -100, 100));
        }
        return values;
    }
}
