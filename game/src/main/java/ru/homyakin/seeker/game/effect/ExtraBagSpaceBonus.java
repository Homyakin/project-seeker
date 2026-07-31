package ru.homyakin.seeker.game.effect;

import java.time.LocalDateTime;

import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;

public final class ExtraBagSpaceBonus {
    private ExtraBagSpaceBonus() {
    }

    public static int valueFromEffect(Effect effect) {
        return effect instanceof Effect.ExtraBagSpace extra ? extra.value() : 0;
    }

    /**
     * Same expiry rule as other group building passives: {@link GroupBuildingPassiveEffect#expiresAt()} empty =
     * permanent; otherwise counted only while {@code expiresAt.isAfter(now)}.
     */
    public static int sumGroupPassiveEffects(Iterable<GroupPassiveEffect> passives, LocalDateTime now) {
        int sum = 0;
        for (final var passive : passives) {
            sum += switch (passive) {
                case GroupBuildingPassiveEffect g -> {
                    if (g.expiresAt().map(expires -> expires.isAfter(now)).orElse(true)) {
                        yield valueFromEffect(g.effect());
                    }
                    yield 0;
                }
            };
        }
        return Math.max(0, sum);
    }
}
