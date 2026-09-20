package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class VersionedAttemptThreatTest {
    private static final int BASE_THREAT = 10;
    private static final BattleRandom CENTERED_RANDOM = (sequence, minimum, maximum) -> {
        if (sequence.startsWith("skill-damage-")) {
            return 0;
        }
        return minimum + (maximum - minimum) / 2;
    };

    @Test
    void penetrationV2PackageKillDoesNotRewardKillThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            1_000,
            100,
            1,
            0,
            Map.of(ActiveEnum.PENETRATION, 1)
        );
        final var farTarget = personage(Position.BACK, 150, 3, 0);
        final var closeTarget = personage(Position.FRONT, 1_000, 1, 0);
        final var log = new BattleActionLog();

        attacker.move(
            new BattleContext(
                List.of(attacker),
                List.of(farTarget, closeTarget),
                CENTERED_RANDOM
            ),
            log,
            1
        );

        final var penetration = events(log, BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == ActiveEnum.PENETRATION)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertFalse(farTarget.isAlive()),
            () -> Assertions.assertEquals(50, penetration.damageTaken()),
            () -> Assertions.assertEquals(BASE_THREAT, attacker.totalThreat()),
            () -> Assertions.assertTrue(attackerThreatEvents(log, attacker).isEmpty())
        );
    }

    @ParameterizedTest
    @EnumSource(RewardedKill.class)
    void approvedAttemptKillSourcesStillRewardExactlyFiftyThreat(RewardedKill killSource) {
        final var scenario = killSource.scenario();

        scenario.attacker().move(scenario.context(), scenario.log(), 1);

        final var threat = attackerThreatEvents(scenario.log(), scenario.attacker());
        Assertions.assertAll(
            () -> Assertions.assertFalse(scenario.target().isAlive()),
            () -> Assertions.assertEquals(1, threat.size()),
            () -> Assertions.assertEquals(50, threat.getFirst().delta()),
            () -> Assertions.assertEquals(60, threat.getFirst().resultingThreat()),
            () -> Assertions.assertEquals(BattleEvent.ThreatReason.KILL, threat.getFirst().reason())
        );
    }

    @Test
    void livingTargetAfterNormalHitStillRewardsExactlyFiveThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            1_000,
            100,
            1,
            0,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var target = personage(Position.FRONT, 500, 1, 0);
        final var log = new BattleActionLog();

        attacker.move(
            new BattleContext(List.of(attacker), List.of(target), CENTERED_RANDOM),
            log,
            1
        );

        final var threat = attackerThreatEvents(log, attacker);
        Assertions.assertAll(
            () -> Assertions.assertTrue(target.isAlive()),
            () -> Assertions.assertEquals(1, threat.size()),
            () -> Assertions.assertEquals(5, threat.getFirst().delta()),
            () -> Assertions.assertEquals(15, threat.getFirst().resultingThreat()),
            () -> Assertions.assertEquals(BattleEvent.ThreatReason.NORMAL_HIT, threat.getFirst().reason())
        );
    }

    private static List<BattleEvent.ThreatChanged> attackerThreatEvents(
        BattleActionLog log,
        BattlePersonage attacker
    ) {
        return events(log, BattleEvent.ThreatChanged.class).stream()
            .filter(event -> event.personageId().equals(attacker.id()))
            .toList();
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        int health,
        int attack,
        int range,
        int dodge,
        Map<ActiveEnum, Integer> skills
    ) {
        return BattlePersonage.forScalingSkills(
            List.of(item(health, attack, range, dodge)),
            position,
            skills,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static BattlePersonage personage(Position position, int health, int range, int dodge) {
        return new BattlePersonage(List.of(item(health, 1, range, dodge)), position);
    }

    private static Item item(int health, int attack, int range, int dodge) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                List.of(new ItemAttack(AttackType.SLASH, 1, range, attack)),
                Optional.empty(),
                health,
                0,
                dodge,
                0,
                100,
                BASE_THREAT,
                0,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private enum RewardedKill {
        NORMAL {
            @Override
            Scenario scenario() {
                return hitScenario(Map.of(ActiveEnum.BERSERK, 1), 80);
            }
        },
        PRECISE_STRIKE {
            @Override
            Scenario scenario() {
                return dodgeScenario();
            }
        },
        DOUBLE_ATTACK {
            @Override
            Scenario scenario() {
                return hitScenario(Map.of(ActiveEnum.DOUBLE_ATTACK, 8), 120);
            }
        },
        ACCUMULATION {
            @Override
            Scenario scenario() {
                final var scenario = hitScenario(Map.of(ActiveEnum.ACCUMULATION, 8), 150);
                for (int charge = 0; charge < 3; charge++) {
                    scenario.attacker().scalingSkills().addAccumulationCharge();
                }
                return scenario;
            }
        };

        abstract Scenario scenario();

        static Scenario hitScenario(Map<ActiveEnum, Integer> skills, int targetHealth) {
            final var attacker = scalingPersonage(Position.FRONT, 1_000, 100, 1, 0, skills);
            return scenario(attacker, personage(Position.FRONT, targetHealth, 1, 0));
        }

        static Scenario dodgeScenario() {
            final var attacker = scalingPersonage(
                Position.FRONT,
                1_000,
                100,
                1,
                0,
                Map.of(ActiveEnum.PRECISE_STRIKE, 8)
            );
            return scenario(attacker, personage(Position.FRONT, 80, 1, 100));
        }

        static Scenario scenario(BattlePersonage attacker, BattlePersonage target) {
            return new Scenario(
                attacker,
                target,
                new BattleContext(List.of(attacker), List.of(target), CENTERED_RANDOM),
                new BattleActionLog()
            );
        }
    }

    private record Scenario(
        BattlePersonage attacker,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log
    ) {
    }
}
