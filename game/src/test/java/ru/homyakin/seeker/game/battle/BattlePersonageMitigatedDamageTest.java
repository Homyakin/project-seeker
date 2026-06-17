package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.utils.RandomUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BattlePersonageMitigatedDamageTest {

    private static final int ATTACK = 100;
    private static final int DEFENSE = 100;

    static Stream<Arguments> mitigatedDamageCases() {
        return Stream.of(
            arguments(AttackType.SLASH, DefenseType.CLOTH, 86),
            arguments(AttackType.BLUNT, DefenseType.CLOTH, 80),
            arguments(AttackType.PIERCE, DefenseType.CLOTH, 84),
            arguments(AttackType.MAGICAL, DefenseType.CLOTH, 81),
            arguments(AttackType.SLASH, DefenseType.LEATHER, 84),
            arguments(AttackType.BLUNT, DefenseType.LEATHER, 81),
            arguments(AttackType.PIERCE, DefenseType.LEATHER, 80),
            arguments(AttackType.MAGICAL, DefenseType.LEATHER, 86),
            arguments(AttackType.SLASH, DefenseType.PLATE, 80),
            arguments(AttackType.BLUNT, DefenseType.PLATE, 86),
            arguments(AttackType.PIERCE, DefenseType.PLATE, 81),
            arguments(AttackType.MAGICAL, DefenseType.PLATE, 84),
            arguments(AttackType.SLASH, DefenseType.ARCANE, 81),
            arguments(AttackType.BLUNT, DefenseType.ARCANE, 84),
            arguments(AttackType.PIERCE, DefenseType.ARCANE, 86),
            arguments(AttackType.MAGICAL, DefenseType.ARCANE, 80)
        );
    }

    @ParameterizedTest(name = "{0} attack against {1} defense equals {2} damage")
    @MethodSource("mitigatedDamageCases")
    void mitigatedDamage(
        AttackType attackType,
        DefenseType defenseType,
        int expectedDamage
    ) {
        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            final var attackerItems = List.of(
                Item.weapon(attackType, 1, ATTACK, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
            );
            final var defenderItems = List.of(
                Item.armor(defenseType, DEFENSE, 50_000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
            );

            final var attacker = new BattlePersonage(attackerItems, Position.FRONT);
            final var defender = new BattlePersonage(defenderItems, Position.FRONT);
            final var context = new BattleContext(List.of(attacker), List.of(defender));

            final int healthBefore = defender.health();
            defender.receiveDamageFrom(
                attacker,
                new DamageRoll(Map.of(attackType, ATTACK), false),
                new BattleActionLog(),
                1,
                context
            );

            assertEquals(healthBefore - expectedDamage, defender.health());
        }
    }
}
