package ru.homyakin.seeker.game.battle;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

class BattlePersonageEquipmentPowerTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Test
    void originalListIsCopiedAndPowerIsComputedOnlyOnFirstRequest() throws Exception {
        final var original = item(20, 5);
        final var items = new ArrayList<>(List.of(original));
        final var participant = new BattlePersonage(items, Position.FRONT);
        final var cache = powerCache();
        Assertions.assertNull(cache.get(participant));
        items.clear();
        items.add(item(100, 100));

        final var expected = EquipmentPowerCalculator.calculate(List.of(original)).unscaled();
        Assertions.assertEquals(expected, participant.equipmentPowerUnscaled());
        final var saved = cache.get(participant);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals(expected, participant.equipmentPowerUnscaled());
        Assertions.assertSame(saved, cache.get(participant));
    }

    @Test
    void permanentEnhancementIsIncludedButSkillsPositionAndTacticAreNot() {
        final var base = item(20, 5);
        final var enhanced = new Item(base.object(), base.modifier(), base.rarity(), 60);
        final var front = BattlePersonage.forScalingSkillsV3Combat(
            List.of(enhanced), Position.FRONT, Map.of(), Optional.empty()
        );
        final var backWithSkill = BattlePersonage.forScalingSkillsV3Combat(
            List.of(enhanced), Position.BACK, Map.of(ActiveEnum.BERSERK, 4), Optional.empty()
        );
        backWithSkill.setTargetingTactic(TargetingTactic.EXECUTIONER);
        Assertions.assertAll(
            () -> Assertions.assertTrue(front.equipmentPowerUnscaled()
                > EquipmentPowerCalculator.calculate(List.of(base)).unscaled()),
            () -> Assertions.assertEquals(
                EquipmentPowerCalculator.calculate(List.of(enhanced)).unscaled(), front.equipmentPowerUnscaled()
            ),
            () -> Assertions.assertEquals(front.equipmentPowerUnscaled(), backWithSkill.equipmentPowerUnscaled())
        );
    }

    @Test
    void temporaryEffectsAndCombatMutationsCannotChangeEquipmentPowerEvenBeforeFirstRequest() {
        final var items = List.of(item(20, 5));
        final var effects = new PersonageEffects(Map.of(
            PersonageEffectType.WORKER_OF_DAY_EFFECT,
            new PersonageEffect(new Effect.Multiplier(50, EffectCharacteristic.HEALTH), NOW.plusHours(1)),
            PersonageEffectType.CONTRABAND_BUFF,
            new PersonageEffect(new Effect.Multiplier(20, EffectCharacteristic.ATTACK), NOW.plusHours(1))
        ));
        final var plain = new BattlePersonage(items, Position.FRONT);
        final var changed = new BattlePersonage(
            items, Position.BACK, Map.of(), effects, NOW, Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3
        );
        Assertions.assertEquals(1_500, changed.maxHealth());
        Assertions.assertEquals(120, changed.slotOneAttackSum());
        changed.increaseAttack(50);
        changed.decreaseDefense(90);
        changed.applyEffectDamageWithoutDefeatEvent(
            AttackType.SLASH, 100, plain.id(), ActiveEnum.KNOCKBACK, new BattleActionLog(), 1
        );
        changed.increaseMaxRange(2, 5);
        changed.setTargetingTactic(TargetingTactic.RELIABLE_STRIKE);
        Assertions.assertNotEquals(plain.health(), changed.health());
        Assertions.assertNotEquals(plain.attackAtRange(1), changed.attackAtRange(1));
        Assertions.assertNotEquals(plain.defenses(), changed.defenses());
        Assertions.assertEquals(plain.equipmentPowerUnscaled(), changed.equipmentPowerUnscaled());
    }

    @Test
    void historicalConstructionKeepsOutOfDomainProbabilitiesUntilPowerIsExplicitlyRequested() {
        for (final var outsideDomain : List.of(item(150, 0), item(0, 100))) {
            final var historical = Assertions.assertDoesNotThrow(
                () -> new BattlePersonage(List.of(outsideDomain), Position.FRONT)
            );
            Assertions.assertDoesNotThrow(historical::legacyPower);
            Assertions.assertThrows(IllegalArgumentException.class, historical::equipmentPowerUnscaled);
            final var current = Assertions.assertDoesNotThrow(() -> BattlePersonage.forCombat(
                List.of(outsideDomain), Position.FRONT, PersonageEffects.EMPTY,
                Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3
            ));
            Assertions.assertThrows(IllegalArgumentException.class, current::equipmentPowerUnscaled);
        }
    }

    @Test
    void zeroPowerIsCachedRatherThanTreatedAsMissing() throws Exception {
        final var participant = new BattlePersonage(List.of(), Position.FRONT);
        Assertions.assertEquals(0, participant.equipmentPowerUnscaled());
        final var saved = powerCache().get(participant);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals(0, participant.equipmentPowerUnscaled());
        Assertions.assertSame(saved, powerCache().get(participant));
    }

    private static Field powerCache() throws NoSuchFieldException {
        final var field = BattlePersonage.class.getDeclaredField("equipmentPowerUnscaled");
        field.setAccessible(true);
        return field;
    }

    private static Item item(int critChance, int dodgeChance) {
        return new Item(new ItemObject(
            null, Set.of(PersonageSlot.MAIN_HAND), List.of(new ItemAttack(AttackType.SLASH, 1, 1, 100)),
            Optional.of(new ItemDefense(DefenseType.PLATE, 100)),
            1_000, critChance, dodgeChance, 0.3, 500, 10, 0, ItemProgressionVersion.V1, Map.of()
        ), Optional.empty(), ItemRarity.COMMON, 0);
    }
}
