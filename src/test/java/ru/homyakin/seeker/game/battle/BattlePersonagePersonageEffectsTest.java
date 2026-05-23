package ru.homyakin.seeker.game.battle;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

class BattlePersonagePersonageEffectsTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 24, 12, 0);

    private static List<Item> sampleItems() {
        return List.of(
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
            Item.armor(DefenseType.PLATE, 50, 1_000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
        );
    }

    @Test
    void givenHealthMultiplierEffect_whenForCombat_thenHealthIncreasedButPowerUnchanged() {
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.WORKER_OF_DAY_EFFECT,
                new PersonageEffect(new Effect.Multiplier(10, EffectCharacteristic.HEALTH), NOW.plusHours(1))
            )
        );
        final var items = sampleItems();
        final var withoutEffects = new BattlePersonage(items, Position.FRONT);
        final var withEffects = new BattlePersonage(items, Position.FRONT, Map.of(), effects, NOW);

        Assertions.assertEquals(1_000, withoutEffects.maxHealth());
        Assertions.assertEquals(1_100, withEffects.maxHealth());
        Assertions.assertEquals(withoutEffects.power(), withEffects.power(), 0.001);
    }

    @Test
    void givenAttackMultiplierEffect_whenForCombat_thenAttackIncreasedButPowerUnchanged() {
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.CONTRABAND_BUFF,
                new PersonageEffect(new Effect.Multiplier(20, EffectCharacteristic.ATTACK), NOW.plusHours(1))
            )
        );
        final var items = sampleItems();
        final var withoutEffects = new BattlePersonage(items, Position.FRONT);
        final var withEffects = new BattlePersonage(items, Position.FRONT, Map.of(), effects, NOW);

        Assertions.assertEquals(100, withoutEffects.slotOneAttackSum());
        Assertions.assertEquals(120, withEffects.slotOneAttackSum());
        Assertions.assertEquals(withoutEffects.power(), withEffects.power(), 0.001);
    }

    @Test
    void givenExpiredEffect_whenForCombat_thenStatsUnchanged() {
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.MENU_ITEM_EFFECT,
                new PersonageEffect(new Effect.Multiplier(50, EffectCharacteristic.HEALTH), NOW.minusHours(1))
            )
        );
        final var items = sampleItems();
        final var withoutEffects = new BattlePersonage(items, Position.FRONT);
        final var withEffects = new BattlePersonage(items, Position.FRONT, Map.of(), effects, NOW);

        Assertions.assertEquals(withoutEffects.maxHealth(), withEffects.maxHealth());
        Assertions.assertEquals(withoutEffects.power(), withEffects.power(), 0.001);
    }
}
