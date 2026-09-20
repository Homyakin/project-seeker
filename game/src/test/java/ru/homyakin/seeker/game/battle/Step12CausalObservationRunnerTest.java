package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Step12CausalObservationRunnerTest {
    private static final long BENCHMARK_SEED = 2_026_092_096L;

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void tempoBenchmarkKeepsThePromisedLongHorizonBound(int level) {
        final var observation = Step12CausalObservationRunner.tempoBenchmark(
            level,
            BENCHMARK_SEED,
            10_000,
            100_000
        );
        final long lostTurns = observation.controlBruiserTurns() - observation.activeBruiserTurns();

        Assertions.assertAll(
            () -> Assertions.assertEquals(32_000, observation.controlBruiserTurns()),
            () -> Assertions.assertTrue(lostTurns > 0),
            () -> Assertions.assertTrue(
                Math.multiplyExact(20L, lostTurns)
                    <= Math.multiplyExact(3L, observation.controlBruiserTurns())
            ),
            () -> Assertions.assertEquals(
                (double) lostTurns / observation.controlBruiserTurns(),
                observation.relativeLoss()
            )
        );
    }
}
