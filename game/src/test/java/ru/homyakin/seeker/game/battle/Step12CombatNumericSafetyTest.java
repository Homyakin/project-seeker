package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

class Step12CombatNumericSafetyTest {
    private static final BattleRandom EXACT_SUCCESSFUL_RANDOM = (sequence, minimum, maximum) -> {
        if (sequence.startsWith("normal-damage:") || sequence.startsWith("skill-damage-")) {
            return minimum + (maximum - minimum) / 2;
        }
        return minimum;
    };

    @ParameterizedTest
    @CsvSource({"1997659207", "1997659208", "2000000000", "2147483647"})
    void healingNearTheIntegerBoundaryClampsWithoutOverflow(int maxHealth) {
        final var owner = personage(maxHealth, 60, Map.of());
        owner.applyVersionedSkillDamage(1, 1, owner);

        final int actual = owner.healAndGetActual(150_000_000);

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, actual),
            () -> Assertions.assertEquals(maxHealth, owner.health()),
            () -> Assertions.assertTrue(owner.isAlive())
        );
    }

    @Test
    void selfHealAtTwoBillionHealthLogsOnlyTheActualMissingHealth() {
        final var owner = personage(2_000_000_000, 60, Map.of(ActiveEnum.SELF_HEAL, 8));
        final var enemy = personage(1_000, 60, Map.of());
        owner.applyVersionedSkillDamage(1, 1, enemy);
        final var log = new BattleActionLog();
        final var context = new BattleContext(List.of(owner), List.of(enemy), EXACT_SUCCESSFUL_RANDOM);

        owner.move(context, log, 1);

        final var healed = log.events().stream()
            .filter(BattleEvent.PersonageHealed.class::isInstance)
            .map(BattleEvent.PersonageHealed.class::cast)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(1, healed.amount()),
            () -> Assertions.assertEquals(2_000_000_000, healed.remainingHealth()),
            () -> Assertions.assertEquals(2, owner.scalingSkills().cooldown(ActiveEnum.SELF_HEAL)),
            () -> Assertions.assertTrue(owner.isAlive())
        );
    }

    @Test
    void healingStillIgnoresNonPositiveAmountsAndDoesNotRevive() {
        final var owner = personage(120, 60, Map.of());
        owner.applyVersionedSkillDamage(1, 1, owner);
        owner.heal(0);
        owner.heal(-1);
        Assertions.assertEquals(119, owner.health());
        owner.applyVersionedSkillDamage(119, 119, owner);

        owner.heal(Integer.MAX_VALUE);

        Assertions.assertEquals(0, owner.health());
        Assertions.assertFalse(owner.isAlive());
    }

    @ParameterizedTest
    @CsvSource({
        "715827882, 60, 2400, 30, 15",
        "715827883, 60, 2400, 30, 15",
        "715827900, 60, 2400, 30, 15",
        "60, 53687091, 180, 30, 1",
        "60, 53687092, 180, 30, 1",
        "2000000000, 200000000, 2000000000, 90, 37500000",
        "2000000000, 60000000, 60000000, 1200, 15000000"
    })
    void thornsUsesTheExactMinimumAndDelaysNarrowingUntilAfterFactoring(
        int health,
        int attack,
        int expectedEventBasis,
        int expectedCoefficient,
        int expectedDamage
    ) {
        final var owner = personage(health, attack, Map.of(ActiveEnum.THORNS, 8));
        final var enemy = personage(100_000_000, 1, Map.of());
        final var context = new BattleContext(List.of(enemy), List.of(owner), EXACT_SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        enemy.move(context, log, 1);

        final var thorns = log.events().stream()
            .filter(BattleEvent.ScalingSkillDamage.class::isInstance)
            .map(BattleEvent.ScalingSkillDamage.class::cast)
            .filter(event -> event.skill() == ActiveEnum.THORNS)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(Map.of(AttackType.PIERCE, expectedEventBasis), thorns.basis()),
            () -> Assertions.assertEquals(expectedCoefficient, thorns.coefficientNumerator()),
            () -> Assertions.assertEquals(4_800, thorns.coefficientDenominator()),
            () -> Assertions.assertEquals(expectedDamage, thorns.damageTaken()),
            () -> Assertions.assertEquals(100_000_000 - expectedDamage, enemy.health())
        );
    }

    private static BattlePersonage personage(int health, int attack, Map<ActiveEnum, Integer> skills) {
        final var object = new ItemObject(
            null,
            Set.of(),
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, attack)),
            Optional.empty(),
            health,
            0,
            0,
            0,
            100,
            10,
            0,
            ItemProgressionVersion.V1,
            Map.of()
        );
        return BattlePersonage.forScalingSkills(
            List.of(Item.fromObject(object)),
            Position.FRONT,
            skills,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }
}
