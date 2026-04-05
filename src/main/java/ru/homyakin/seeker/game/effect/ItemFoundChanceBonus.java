package ru.homyakin.seeker.game.effect;

import java.time.LocalDateTime;

import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

public final class ItemFoundChanceBonus {
    private ItemFoundChanceBonus() {
    }

    public static int percentFromEffect(Effect effect) {
        return effect instanceof Effect.ItemFoundChancePercent item ? item.percent() : 0;
    }

    public static int sumPersonageEffects(PersonageEffects effects, LocalDateTime now) {
        int sum = 0;
        for (final var personageEffect : effects.effects().values()) {
            if (personageEffect.expireDateTime().isAfter(now)) {
                sum += percentFromEffect(personageEffect.effect());
            }
        }
        return Math.max(0, sum);
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
                        yield percentFromEffect(g.effect());
                    }
                    yield 0;
                }
            };
        }
        return Math.max(0, sum);
    }
}
