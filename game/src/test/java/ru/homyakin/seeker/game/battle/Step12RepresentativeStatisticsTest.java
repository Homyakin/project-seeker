package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.HashSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class Step12RepresentativeStatisticsTest {
    @Test
    void protectedGroupDoesNotLeaveTheTestedMageBeyondTheGuardsNextLine() {
        for (final var scenario : Step12RepresentativeFollowup.scenarios()) {
            if (scenario.code().startsWith("PROTECTED_")) {
                Assertions.assertThat(scenario.left()).noneMatch(build -> build.position() == Position.BACK);
                Assertions.assertThat(scenario.right()).noneMatch(build -> build.position() == Position.BACK);
            }
        }
    }

    @Test
    void followupDefinitionsRemainUniqueAndUseRealFullEquipment() {
        final var cases = Step12RepresentativeFollowup.scenarios();
        Assertions.assertThat(cases).hasSize(210);
        Assertions.assertThat(cases.stream().map(Step12RepresentativeMatrix.Scenario::code).distinct().count())
            .isEqualTo(cases.size());
        for (final var scenario : cases) {
            Assertions.assertThat(scenario.left()).hasSameSizeAs(scenario.right());
            for (final var team : List.of(scenario.left(), scenario.right())) {
                for (final var build : team) {
                    Assertions.assertThat(build.items(scenario.level()).stream()
                        .mapToInt(item -> item.object().slots().size()).sum()).isEqualTo(7);
                }
            }
        }
    }

    @Test
    void unfinishedBattleHasNoScore() {
        Assertions.assertThatThrownBy(() -> Step12RepresentativeSimulator.score("TIMEOUT"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThat(Step12RepresentativeSimulator.score("DRAW")).isEqualTo(0.5);
    }

    @Test
    void perfectScreeningResultStillHasSamplingUncertainty() {
        final var interval = Step12RepresentativeSimulator.estimate(List.of(1.0, 1.0, 1.0));
        Assertions.assertThat(interval[0]).isEqualTo(1);
        Assertions.assertThat(interval[1]).isBetween(0.0, 0.9);
        Assertions.assertThat(interval[2]).isEqualTo(1);
        Assertions.assertThat(Step12RepresentativeSimulator.estimate(List.of())[0]).isNaN();
    }

    @Test
    void seedSeparatesRootsLevelsAndPairsButIsRepeatable() {
        final long seed = Step12RepresentativeSimulator.seed(1, "A", 10, 0);
        Assertions.assertThat(seed).isEqualTo(Step12RepresentativeSimulator.seed(1, "A", 10, 0));
        Assertions.assertThat(seed).isNotEqualTo(Step12RepresentativeSimulator.seed(2, "A", 10, 0));
        Assertions.assertThat(seed).isNotEqualTo(Step12RepresentativeSimulator.seed(1, "A", 20, 0));
        Assertions.assertThat(seed).isNotEqualTo(Step12RepresentativeSimulator.seed(1, "A", 10, 1));
    }

    @Test
    void nearbyRootsDoNotJustPermuteTheSamePairSeeds() {
        final var first = new HashSet<Long>();
        final var second = new HashSet<Long>();
        for (int index = 0; index < 2000; index++) {
            first.add(Step12RepresentativeSimulator.seed(2026102301L, "A", 10, index));
            second.add(Step12RepresentativeSimulator.seed(2026102302L, "A", 10, index));
        }
        Assertions.assertThat(first).hasSize(2000);
        Assertions.assertThat(second).hasSize(2000);
        Assertions.assertThat(first).doesNotContainAnyElementsOf(second);
    }
}
