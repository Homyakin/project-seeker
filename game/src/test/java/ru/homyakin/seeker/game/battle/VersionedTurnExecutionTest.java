package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class VersionedTurnExecutionTest {
    private static final int DEFAULT_THREAT = 10;
    private static final BattleRandom CENTERED_RANDOM = (sequence, minimum, maximum) -> {
        if (sequence.startsWith("skill-damage-")) {
            return 0;
        }
        if (sequence.startsWith("turn-order:")) {
            return maximum;
        }
        return minimum + (maximum - minimum) / 2;
    };

    @Test
    void queuedPersonageDefeatedBeforeItsOrderDoesNotRecordAnAction() {
        final var firstMover = scalingPersonage(
            1_000,
            200,
            1_000,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var defeatedBeforeOrder = personage(50, 1, 1_000, 0);

        final var result = new Battle(CENTERED_RANDOM).process(
            List.of(firstMover),
            List.of(defeatedBeforeOrder),
            1
        );

        final var firstFinish = finishedTurn(result.actionLog(), firstMover);
        final var defeatedFinish = finishedTurn(result.actionLog(), defeatedBeforeOrder);
        Assertions.assertAll(
            () -> Assertions.assertTrue(firstFinish.actionPerformed()),
            () -> Assertions.assertTrue(firstFinish.alive()),
            () -> Assertions.assertFalse(defeatedFinish.actionPerformed()),
            () -> Assertions.assertFalse(defeatedFinish.alive()),
            () -> Assertions.assertEquals(1, firstMover.battlePersonageStats().turnsCount()),
            () -> Assertions.assertEquals(1, defeatedBeforeOrder.battlePersonageStats().turnsCount())
        );
    }

    @Test
    void periodicDefeatBeforeActionDoesNotRecordAnAction() {
        final var source = scalingPersonage(
            1_000,
            1,
            100,
            Map.of(ActiveEnum.BLEEDING, 1)
        );
        final var target = personage(50, 1, 100, 0);
        target.addOrReplaceScalingBleeding(new ScalingBleedingEffect(
            source,
            Map.of(AttackType.SLASH, 100),
            1,
            1,
            1
        ));
        final var context = new BattleContext(List.of(target), List.of(source), CENTERED_RANDOM);
        final var log = new BattleActionLog();

        target.move(context, log, 1);

        final var finish = finishedTurn(log, target);
        Assertions.assertAll(
            () -> Assertions.assertFalse(finish.actionPerformed()),
            () -> Assertions.assertFalse(finish.alive()),
            () -> Assertions.assertEquals(1, target.battlePersonageStats().turnsCount()),
            () -> Assertions.assertTrue(log.traceEvents().stream()
                .noneMatch(BattleTraceEvent.NormalAttackAttempt.class::isInstance))
        );
    }

    @Test
    void actionEndingInDeathFromCounterAttackRemainsRecorded() {
        final var attacker = personage(30, 1, 100, 0);
        final var counterAttacker = scalingPersonage(
            500,
            100,
            100,
            Map.of(ActiveEnum.COUNTER_ATTACK, 8)
        );
        final var context = new BattleContext(
            List.of(attacker),
            List.of(counterAttacker),
            CENTERED_RANDOM
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var finish = finishedTurn(log, attacker);
        Assertions.assertAll(
            () -> Assertions.assertTrue(finish.actionPerformed()),
            () -> Assertions.assertFalse(finish.alive()),
            () -> Assertions.assertEquals(1, attacker.battlePersonageStats().turnsCount()),
            () -> Assertions.assertEquals(1, log.traceEvents().stream()
                .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
                .count())
        );
    }

    private static BattleTraceEvent.TurnFinished finishedTurn(
        BattleActionLog log,
        BattlePersonage personage
    ) {
        return log.traceEvents().stream()
            .filter(BattleTraceEvent.TurnFinished.class::isInstance)
            .map(BattleTraceEvent.TurnFinished.class::cast)
            .filter(turn -> turn.personageId().equals(personage.id()))
            .findFirst()
            .orElseThrow();
    }

    private static BattlePersonage scalingPersonage(
        int health,
        int attack,
        int speed,
        Map<ActiveEnum, Integer> skills
    ) {
        return BattlePersonage.forScalingSkills(
            List.of(item(health, attack, speed, 0)),
            Position.FRONT,
            skills,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static BattlePersonage personage(int health, int attack, int speed, int dodge) {
        return new BattlePersonage(List.of(item(health, attack, speed, dodge)), Position.FRONT);
    }

    private static Item item(int health, int attack, int speed, int dodge) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                List.of(new ItemAttack(AttackType.SLASH, 1, 1, attack)),
                Optional.empty(),
                health,
                0,
                dodge,
                0,
                speed,
                DEFAULT_THREAT,
                0,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }
}
