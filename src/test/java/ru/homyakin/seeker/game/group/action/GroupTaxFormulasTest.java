package ru.homyakin.seeker.game.group.action;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.TimeUtils;

class GroupTaxFormulasTest {

    @Test
    void discrete_massLeave_day1_matchesScenario4() {
        Assertions.assertEquals(96, GroupTaxFormulas.computeNextTax(100, 80));
    }

    @Test
    void discrete_massLeave_day2_matchesScenario4() {
        Assertions.assertEquals(92, GroupTaxFormulas.computeNextTax(95, 80));
    }

    @Test
    void discrete_smallGroup_matchesScenario6() {
        Assertions.assertEquals(4, GroupTaxFormulas.computeNextTax(5, 4));
    }

    @Test
    void discrete_neverBelowActual() {
        Assertions.assertEquals(10, GroupTaxFormulas.computeNextTax(10, 10));
    }

    @Test
    void discrete_emptyGroup_minimumOne() {
        Assertions.assertEquals(GroupTaxFormulas.MIN_TAX_LEVEL, GroupTaxFormulas.computeNextTax(0, 0));
    }

    @Test
    void wallClock_emptyGroup_minimumOne() {
        Assertions.assertEquals(
            GroupTaxFormulas.MIN_TAX_LEVEL,
            GroupTaxFormulas.computeNextTax(0, 0, Optional.empty(), TimeUtils.moscowTime(), Duration.ofHours(24))
        );
    }

    @Test
    void wallClock_sameAsStored_whenNoTimePassed() {
        final var t = LocalDateTime.of(2026, 4, 5, 12, 0);
        Assertions.assertEquals(
            5,
            GroupTaxFormulas.computeNextTax(5, 4, Optional.of(t), t, Duration.ofHours(24))
        );
    }
}
