package ru.homyakin.seeker.game.battle.v4;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BattlePersonageDamageMatrixTest {

    static Stream<Arguments> expectedMitigationMultipliers() {
        return Stream.of(
            arguments(DefenseType.CLOTH, AttackType.SLASH, 0.75),
            arguments(DefenseType.CLOTH, AttackType.BLUNT, 1.2),
            arguments(DefenseType.CLOTH, AttackType.PIERCE, 0.8),
            arguments(DefenseType.CLOTH, AttackType.MAGICAL, 1.25),
            arguments(DefenseType.LEATHER, AttackType.SLASH, 0.8),
            arguments(DefenseType.LEATHER, AttackType.BLUNT, 1.2),
            arguments(DefenseType.LEATHER, AttackType.PIERCE, 1.25),
            arguments(DefenseType.LEATHER, AttackType.MAGICAL, 0.75),
            arguments(DefenseType.PLATE, AttackType.SLASH, 1.25),
            arguments(DefenseType.PLATE, AttackType.BLUNT, 0.75),
            arguments(DefenseType.PLATE, AttackType.PIERCE, 1.2),
            arguments(DefenseType.PLATE, AttackType.MAGICAL, 0.8),
            arguments(DefenseType.ARCANE, AttackType.SLASH, 1.2),
            arguments(DefenseType.ARCANE, AttackType.BLUNT, 0.85),
            arguments(DefenseType.ARCANE, AttackType.PIERCE, 0.75),
            arguments(DefenseType.ARCANE, AttackType.MAGICAL, 1.20)
        );
    }

    @ParameterizedTest
    @MethodSource("expectedMitigationMultipliers")
    void damageMitigationMultiplier_matchesMatrix(
        DefenseType defenseType,
        AttackType attackType,
        double expected
    ) {
        assertEquals(
            expected,
            BattlePersonage.damageMitigationMultiplier(defenseType, attackType),
            1e-12
        );
    }
}
