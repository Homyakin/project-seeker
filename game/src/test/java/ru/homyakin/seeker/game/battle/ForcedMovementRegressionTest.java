package ru.homyakin.seeker.game.battle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.RandomUtils;

class ForcedMovementRegressionTest {
    private static final int ROUND = 7;
    private static final BattleRandom SUCCESSFUL_RANDOM = (sequence, minimum, maximum) ->
        sequence.startsWith("normal-damage:") ? minimum + (maximum - minimum) / 2 : minimum;

    @ParameterizedTest
    @CsvSource({
        "HIT_AND_RUN, true", "HIT_AND_RUN, false",
        "RETREAT, true", "RETREAT, false",
        "KNOCKBACK, true", "KNOCKBACK, false"
    })
    void everyMovementSkillLogsOneBackwardStepAfterCrossingTheField(
        ActiveEnum skill,
        boolean firstTeam
    ) {
        final var fixture = fixture(skill, firstTeam);
        fixture.context().moveTowardEnemy(fixture.mover());
        fixture.context().moveTowardEnemy(fixture.mover());
        final int from = firstTeam ? 4 : 1;
        final int to = firstTeam ? 3 : 2;
        Assertions.assertEquals(from, fixture.mover().currentPosition());
        final var log = new BattleActionLog();

        fixture.attacker().move(fixture.context(), log, ROUND);

        final var source = skill == ActiveEnum.HIT_AND_RUN ? fixture.mover() : fixture.opponent();
        Assertions.assertEquals(
            List.of(new BattleEvent.PersonageForcedMove(fixture.mover().id(), source.id(), skill, to, ROUND)),
            forcedMoves(log)
        );
        Assertions.assertEquals(to, fixture.mover().currentPosition());
        Assertions.assertSame(
            fixture.mover(),
            fixture.context().lines().get(to).battlePersonages().get(fixture.mover().id())
        );
        Assertions.assertFalse(fixture.context().lines().get(from).battlePersonages()
            .containsKey(fixture.mover().id()));
        if (skill == ActiveEnum.HIT_AND_RUN) {
            final var attempt = log.traceEvents().stream()
                .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
                .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
                .findFirst()
                .orElseThrow();
            Assertions.assertAll(
                () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, attempt.access()),
                () -> Assertions.assertEquals(from, attempt.lineBeforeRetreat()),
                () -> Assertions.assertEquals(to, attempt.lineAfterRetreat())
            );
        }
    }

    @ParameterizedTest
    @CsvSource({
        "HIT_AND_RUN, true", "HIT_AND_RUN, false",
        "RETREAT, true", "RETREAT, false",
        "KNOCKBACK, true", "KNOCKBACK, false"
    })
    void movementSkillsAtTheOwnFieldEdgeDoNotLogANonexistentMove(ActiveEnum skill, boolean firstTeam) {
        final var fixture = fixture(skill, firstTeam);
        fixture.context().moveBackward(fixture.mover(), Integer.MAX_VALUE);
        fixture.context().moveTowardEnemy(fixture.opponent());
        fixture.context().moveTowardEnemy(fixture.opponent());
        final int edge = firstTeam ? 0 : 5;
        Assertions.assertEquals(edge, fixture.mover().currentPosition());
        Assertions.assertEquals(1, fixture.mover().distanceTo(fixture.opponent()));
        final var log = new BattleActionLog();

        fixture.attacker().move(fixture.context(), log, ROUND);

        Assertions.assertEquals(edge, fixture.mover().currentPosition());
        Assertions.assertTrue(forcedMoves(log).isEmpty());
        Assertions.assertEquals(1, log.events().stream()
            .filter(BattleEvent.DamageReceived.class::isInstance)
            .count());
        if (skill == ActiveEnum.HIT_AND_RUN) {
            Assertions.assertTrue(log.events().contains(
                new BattleEvent.SkillWindowUsed(fixture.mover().id(), skill, ROUND)
            ));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "3289266323258170439, false, 56, G_GUARD_LEGENDARY, 4, 3",
        "6446094721776612200, true, 55, C_BRUISER_LEGENDARY, 1, 2"
    })
    void aLegalCompleteBattleKeepsEveryForcedMoveToOneStep(
        long seed,
        boolean reverse,
        int regressionRound,
        String regressionName,
        int regressionFrom,
        int regressionTo
    ) {
        final var result = legalBattle(seed, reverse);
        final var positions = new HashMap<UUID, Integer>();
        result.initState().personagesById().forEach((id, personage) -> positions.put(id, personage.lineIndex()));
        boolean regressionEncountered = false;
        int invadingForcedMoves = 0;
        for (final var event : result.actionLog().events()) {
            if (event instanceof BattleEvent.MovedTowardEnemy move) {
                positions.put(move.personageId(), move.newLineIndex());
            } else if (event instanceof BattleEvent.PersonageForcedMove move) {
                final int from = positions.put(move.personageId(), move.newLineIndex());
                final var initial = result.initState().personagesById().get(move.personageId());
                final int backwardDelta = -initial.advanceDirection().indexDelta();
                Assertions.assertEquals(backwardDelta, move.newLineIndex() - from, () -> move.toString());
                Assertions.assertTrue(move.newLineIndex() >= 0 && move.newLineIndex() < result.initState().lines().size());
                if (initial.firstTeam() ? from >= 3 : from < 3) {
                    invadingForcedMoves++;
                }
                if (move.round() == regressionRound
                    && initial.name().orElseThrow().equals(regressionName)
                    && from == regressionFrom
                    && move.skill() == ActiveEnum.RETREAT) {
                    Assertions.assertEquals(regressionTo, move.newLineIndex());
                    regressionEncountered = true;
                }
            }
        }
        Assertions.assertTrue(regressionEncountered, "The independently reproduced crossing must remain covered");
        Assertions.assertTrue(invadingForcedMoves > 0);
        Assertions.assertTrue(result.rounds() < 10_000);
    }

    private static BattleResult legalBattle(long seed, boolean reverse) {
        return RandomUtils.withSeed(seed, () -> {
            final var guard = Step12RepresentativeRisks.ranked("G", ItemRarity.LEGENDARY);
            final var bruiser = Step12RepresentativeRisks.ranked("C", ItemRarity.LEGENDARY);
            final var ranger = Step12RepresentativeRisks.ranked("D", ItemRarity.LEGENDARY);
            final var pressure = team(List.of(guard, bruiser, bruiser));
            final var rearRangers = team(List.of(guard, ranger, ranger));
            return new Battle(seed).process(
                reverse ? rearRangers : pressure,
                reverse ? pressure : rearRangers,
                10_000
            );
        });
    }

    private static List<BattlePersonage> team(List<Step12RepresentativeBuilds.Build> builds) {
        return builds.stream().map(build -> {
            final var gear = new Step123BattleRunner.Gear(build, EquipmentCatalogVersion.SCALING_V4);
            final var personage = BattlePersonage.forScalingSkillsV2Combat(
                gear.items(10), build.position(), Map.of(), Optional.of(build.code())
            );
            personage.setTargetingTactic(build.tactic());
            return personage;
        }).toList();
    }

    private static Fixture fixture(ActiveEnum skill, boolean firstTeam) {
        final var mover = personage(skill == ActiveEnum.KNOCKBACK ? Map.of() : Map.of(skill, 4));
        final var opponent = personage(skill == ActiveEnum.KNOCKBACK ? Map.of(skill, 4) : Map.of());
        final var context = new BattleContext(
            List.of(firstTeam ? mover : opponent),
            List.of(firstTeam ? opponent : mover),
            SUCCESSFUL_RANDOM
        );
        return new Fixture(context, mover, opponent, skill == ActiveEnum.HIT_AND_RUN ? mover : opponent);
    }

    private static BattlePersonage personage(Map<ActiveEnum, Integer> skills) {
        return BattlePersonage.forScalingSkillsV2Combat(
            List.of(new Item(
                new ItemObject(
                    null,
                    Set.of(),
                    List.of(new ItemAttack(AttackType.SLASH, 1, 1, 10)),
                    Optional.empty(),
                    5_000,
                    100,
                    0,
                    0,
                    100,
                    10,
                    0,
                    ItemProgressionVersion.V1,
                    Map.of()
                ),
                Optional.empty(),
                ItemRarity.COMMON
            )),
            Position.FRONT,
            skills,
            Optional.empty()
        );
    }

    private static List<BattleEvent.PersonageForcedMove> forcedMoves(BattleActionLog log) {
        return log.events().stream()
            .filter(BattleEvent.PersonageForcedMove.class::isInstance)
            .map(BattleEvent.PersonageForcedMove.class::cast)
            .toList();
    }

    private record Fixture(
        BattleContext context,
        BattlePersonage mover,
        BattlePersonage opponent,
        BattlePersonage attacker
    ) {
    }
}
