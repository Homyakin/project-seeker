package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.homyakin.seeker.game.battle.BattleActionLog;
import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.DamageRoll;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.RandomUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Two personages: attacker items sum to {@code attackTotal} of {@code attackType};
 * defender items sum to {@code defenseTotal} of {@code defenseType}.
 * {@link BattlePersonage#receiveDamageFrom} applies matrix-weighted defense; random damage spread is disabled.
 */
class BattlePersonageMitigatedDamageTest {

    /** Same constant as {@code BattlePersonage} mitigation curve (not public on production type). */
    private static final int DEFENSE_COEF = 500;

    private static int expectedMitigatedPortion(
        int rawAttack,
        int defenseTotal,
        DefenseType defenseType,
        AttackType attackType
    ) {
        final double effectiveDef =
            defenseTotal * BattlePersonage.damageMitigationMultiplier(defenseType, attackType);
        final double defenseReduce = 1 - effectiveDef / (effectiveDef + DEFENSE_COEF);
        return (int) (rawAttack * defenseReduce);
    }

    static Stream<Arguments> singleLayerCases() {
        return Stream.of(
            arguments(100, 100, AttackType.SLASH, DefenseType.PLATE),
            arguments(200, 50, AttackType.BLUNT, DefenseType.CLOTH),
            arguments(150, 80, AttackType.MAGICAL, DefenseType.ARCANE)
        );
    }

    @ParameterizedTest
    @MethodSource("singleLayerCases")
    void givenAttackerAndDefender_whenReceiveDamage_thenHealthDropsByMitigatedAmount(
        int attackTotal,
        int defenseTotal,
        AttackType attackType,
        DefenseType defenseType
    ) {
        final int expectedDamageTaken =
            expectedMitigatedPortion(attackTotal, defenseTotal, defenseType, attackType);

        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            final var attackerItems = List.of(
                Item.weapon(attackType, 1, attackTotal, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
            );
            final var defenderItems = List.of(
                Item.armor(defenseType, defenseTotal, 50_000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
            );

            final var attacker = new BattlePersonage(attackerItems, Position.FRONT);
            final var defender = new BattlePersonage(defenderItems, Position.FRONT);
            final var context = new BattleContext(List.of(attacker), List.of(defender));

            final int healthBefore = defender.health();
            final var roll = new DamageRoll(Map.of(attackType, attackTotal), false);
            defender.receiveDamageFrom(attacker, roll, new BattleActionLog(), 1, context);

            assertEquals(healthBefore - expectedDamageTaken, defender.health());
        }
    }

    @Test
    void givenStackedWeaponsAndArmors_whenReceiveDamage_thenUsesSummedStats() {
        final int expectedDamageTaken =
            expectedMitigatedPortion(100, 100, DefenseType.PLATE, AttackType.SLASH);

        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            final var attackerItems = List.of(
                Item.weapon(AttackType.SLASH, 1, 40, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
                Item.weapon(AttackType.SLASH, 1, 60, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
            );
            final var defenderItems = List.of(
                Item.armor(DefenseType.PLATE, 50, 25_000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON),
                Item.armor(DefenseType.PLATE, 50, 25_000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
            );

            final var attacker = new BattlePersonage(attackerItems, Position.FRONT);
            final var defender = new BattlePersonage(defenderItems, Position.FRONT);
            final var context = new BattleContext(List.of(attacker), List.of(defender));

            final int healthBefore = defender.health();
            final var roll = new DamageRoll(Map.of(AttackType.SLASH, 100), false);
            defender.receiveDamageFrom(attacker, roll, new BattleActionLog(), 1, context);

            assertEquals(healthBefore - expectedDamageTaken, defender.health());
        }
    }

    @Test
    void givenOverkillDamage_whenReceiveDamage_thenHealthDoesNotGoBelowZero() {
        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            final var attackerItems = List.of(
                Item.weapon(AttackType.SLASH, 1, 500, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
            );
            final var defenderItems = List.of(
                Item.armor(DefenseType.PLATE, 10, 100, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
            );

            final var attacker = new BattlePersonage(attackerItems, Position.FRONT);
            final var defender = new BattlePersonage(defenderItems, Position.FRONT);
            final var context = new BattleContext(List.of(attacker), List.of(defender));

            defender.receiveDamageFrom(
                attacker,
                new DamageRoll(Map.of(AttackType.SLASH, 500), false),
                new BattleActionLog(),
                1,
                context
            );

            assertEquals(0, defender.health());
        }
    }
}
