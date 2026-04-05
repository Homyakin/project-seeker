package ru.homyakin.seeker.game.group.action;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public final class GroupTaxFormulas {
    public static final int MIN_TAX_LEVEL = 1;

    private GroupTaxFormulas() {
    }

    public static int computeNextTax(int effectiveTax, int memberCount) {
        if (effectiveTax == memberCount) {
            return clampTaxLevel(effectiveTax);
        }
        final var diff = effectiveTax - memberCount;
        return Math.max(effectiveTax - Math.max(Math.round(diff * 0.2f), 1), MIN_TAX_LEVEL);
    }

    public static int computeNextTax(
        int effectiveTax,
        int memberCount,
        Optional<LocalDateTime> lastChange,
        LocalDateTime now,
        Duration refreshInterval
    ) {
        if (lastChange.isEmpty()) {
            return clampTaxLevel(Math.max(memberCount, effectiveTax));
        }
        if (memberCount >= effectiveTax) {
            return clampTaxLevel(memberCount);
        }
        final LocalDateTime last = lastChange.get();
        if (now.isBefore(last.plus(refreshInterval))) {
            return clampTaxLevel(effectiveTax);
        }
        return computeNextTax(effectiveTax, memberCount);
    }

    private static int clampTaxLevel(int tax) {
        return Math.max(MIN_TAX_LEVEL, tax);
    }
}
