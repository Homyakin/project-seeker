package ru.homyakin.seeker.game.battle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class V3MobilitySkillsBattleTest {
    private static final int HEALTH = 20_000;
    private static final int ATTACK = 100;
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;

    @ParameterizedTest
    @CsvSource({
        "1, 3000", "2, 4000", "3, 5000", "4, 6000",
        "5, 7000", "6, 8000", "7, 9000", "8, 10000", "9, 10000",
    })
    void hitAndRunUsesTheInclusiveV3ChanceAndOneWindowAtEveryRank(int points, int threshold) {
        final var source = mobile(points, 1);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom(threshold);
        final var context = new BattleContext(List.of(source), List.of(target), random);
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, attempts(log).getFirst().access()),
            () -> Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size()),
            () -> Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN)),
            () -> Assertions.assertEquals(Math.min(8, points), source.scalingSkills().points(ActiveEnum.HIT_AND_RUN)),
            () -> Assertions.assertEquals(before - 1, source.currentPosition()),
            () -> Assertions.assertEquals(1, random.hitAndRunChecks)
        );
    }

    @ParameterizedTest
    @CsvSource({"1, 3001", "2, 4001", "3, 5001", "4, 6001", "5, 7001", "6, 8001", "7, 9001"})
    void failedWindowPreservesAnOrdinaryAttackWithoutRetreatOrCooldown(int points, int roll) {
        final var source = mobile(points, 1);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom(roll);
        final var context = new BattleContext(List.of(source), List.of(target), random);
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.NORMAL, attempts(log).getFirst().access()),
            () -> Assertions.assertEquals(HEALTH - ATTACK, target.health()),
            () -> Assertions.assertEquals(before, source.currentPosition()),
            () -> Assertions.assertEquals(0, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN)),
            () -> Assertions.assertTrue(windows(log, ActiveEnum.HIT_AND_RUN).isEmpty()),
            () -> Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty()),
            () -> Assertions.assertEquals(1, random.hitAndRunChecks)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlySuccessfulWindowGrantsTheExtraLineInEitherFieldDirection(boolean reverse) {
        for (final boolean succeeds : List.of(false, true)) {
            final var source = mobile(4, 1);
            final var target = target(Position.FRONT);
            final var random = new MobilityRandom(succeeds ? 6_000 : 6_001);
            final var context = context(source, target, random, reverse);
            context.moveBackward(source, 1);
            final int before = source.currentPosition();
            Assertions.assertEquals(2, source.distanceTo(target));
            final var log = new BattleActionLog();

            source.move(context, log, 1);

            final int direction = source.advanceDirection().indexDelta();
            if (succeeds) {
                Assertions.assertEquals(AttackAccess.HIT_AND_RUN, attempts(log).getFirst().access());
                Assertions.assertEquals(2, attempts(log).getFirst().distance());
                Assertions.assertEquals(before - direction, source.currentPosition());
                Assertions.assertEquals(1, events(log, BattleEvent.PersonageForcedMove.class).size());
                Assertions.assertEquals(HEALTH - ATTACK, target.health());
                Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
            } else {
                Assertions.assertTrue(attempts(log).isEmpty());
                Assertions.assertEquals(before + direction, source.currentPosition());
                Assertions.assertEquals(1, events(log, BattleEvent.MovedTowardEnemy.class).size());
                Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
                Assertions.assertEquals(HEALTH, target.health());
                Assertions.assertEquals(0, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
            }
            Assertions.assertEquals(1, random.hitAndRunChecks);
        }
    }

    @Test
    void windowExtendsEachWeaponPartButNeverRemovesItsMinimumRange() {
        final var source = personage(Position.FRONT, HEALTH, List.of(
            new ItemAttack(AttackType.SLASH, 1, 1, ATTACK),
            new ItemAttack(AttackType.BLUNT, 3, 3, 40)
        ), 0, Map.of(ActiveEnum.HIT_AND_RUN, 4), V3);
        final var target = target(Position.FRONT);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom(6_000));
        context.moveBackward(source, 1);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(2, attempts(log).getFirst().distance());
        Assertions.assertEquals(Map.of(AttackType.SLASH, ATTACK), attempts(log).getFirst().savedBasis());
        Assertions.assertEquals(HEALTH - ATTACK, target.health());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 10})
    void windowIsCheckedOncePerOwnTurnNotOncePerCandidate(int candidates) {
        final var source = mobile(4, 1);
        final var targets = new ArrayList<BattlePersonage>();
        for (int index = 0; index < candidates; index++) {
            targets.add(target(Position.FRONT));
        }
        final var random = new MobilityRandom(6_001);
        final var context = new BattleContext(List.of(source), targets, random);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(1, random.hitAndRunChecks);
        Assertions.assertEquals(1, attempts(log).size());
        Assertions.assertEquals(AttackAccess.NORMAL, attempts(log).getFirst().access());
    }

    @Test
    void guardInterceptionDoesNotRerollTheWindow() {
        final var source = mobile(4, 2);
        final var original = target(Position.BACK);
        final var guard = personage(Position.MID, HEALTH, attacks(2), 0, Map.of(ActiveEnum.GUARD, 4), V3);
        final var front = target(Position.FRONT);
        final var random = new MobilityRandom(6_000);
        final var context = new BattleContext(List.of(source), List.of(original, guard, front), random);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        final var selection = events(log, BattleEvent.TargetSelected.class).getFirst();
        Assertions.assertEquals(original.id(), selection.originalTargetId());
        Assertions.assertEquals(guard.id(), selection.finalTargetId());
        Assertions.assertEquals(1, events(log, BattleEvent.AttackIntercepted.class).size());
        Assertions.assertEquals(1, random.hitAndRunChecks);
        Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size());
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @Test
    void failedWindowCanBeRetriedOnTheNextOwnTurn() {
        final var source = mobile(4, 1);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom(6_001, 6_000);
        final var context = new BattleContext(List.of(source), List.of(target), random);
        final var log = new BattleActionLog();

        source.move(context, log, 1);
        Assertions.assertEquals(0, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        source.move(context, log, 2);

        Assertions.assertEquals(List.of(AttackAccess.NORMAL, AttackAccess.HIT_AND_RUN),
            attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList());
        Assertions.assertEquals(2, random.hitAndRunChecks);
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @Test
    void successfulWindowWaitsForEightCompleteOwnTurnsBeforeCheckingAgain() {
        final var source = mobile(4, 3);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom(6_000, 6_000);
        final var context = new BattleContext(List.of(source), List.of(target), random);
        final var log = new BattleActionLog();
        source.move(context, log, 1);

        for (int turn = 2; turn <= 9; turn++) {
            source.move(context, log, turn);
            Assertions.assertEquals(9 - turn, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
            Assertions.assertEquals(1, random.hitAndRunChecks);
            Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size());
        }
        source.move(context, log, 10);

        Assertions.assertEquals(2, random.hitAndRunChecks);
        Assertions.assertEquals(List.of(1, 10), windows(log, ActiveEnum.HIT_AND_RUN).stream()
            .map(BattleEvent.SkillWindowUsed::round).toList());
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @Test
    void dodgedAttackStillConsumesTheSuccessfulWindowAndRetreats() {
        final var source = mobile(4, 1);
        final var target = personage(Position.FRONT, HEALTH, attacks(3), 100, Map.of(), V3);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom(6_000));
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertTrue(attempts(log).getFirst().dodged());
        Assertions.assertEquals(HEALTH, target.health());
        Assertions.assertEquals(before - 1, source.currentPosition());
        Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size());
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @Test
    void reactionKilledAttackerCannotRetreatAfterItsSuccessfulWindow() {
        final var source = personage(Position.FRONT, 20, attacks(1), 0, Map.of(ActiveEnum.HIT_AND_RUN, 4), V3);
        final var target = personage(Position.FRONT, HEALTH, attacks(1), 100, Map.of(ActiveEnum.FEINT, 8), V3);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom(6_000));
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertFalse(source.isAlive());
        Assertions.assertEquals(before, source.currentPosition());
        Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
        Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size());
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void successfulWindowDoesNotMovePastEitherRearBoundary(boolean reverse) {
        final var source = mobile(4, 3);
        final var target = target(Position.FRONT);
        final var context = context(source, target, new MobilityRandom(6_000), reverse);
        context.moveBackward(source, 2);
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(before, source.currentPosition());
        Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
        Assertions.assertEquals(1, windows(log, ActiveEnum.HIT_AND_RUN).size());
        Assertions.assertEquals(8, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void nonPositivePointsNeverCreateOrRollAWindow(int points) {
        final var source = mobile(points, 1);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom();
        final var context = new BattleContext(List.of(source), List.of(target), random);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(AttackAccess.NORMAL, attempts(log).getFirst().access());
        Assertions.assertFalse(source.scalingSkills().has(ActiveEnum.HIT_AND_RUN));
        Assertions.assertTrue(windows(log, ActiveEnum.HIT_AND_RUN).isEmpty());
        Assertions.assertEquals(0, random.hitAndRunChecks);
    }

    @Test
    void mixedHistoricalHitAndRunVersionsKeepGuaranteedWindowsAndTheirOldCooldowns() {
        final var v1 = personage(Position.FRONT, HEALTH, attacks(1), 0,
            Map.of(ActiveEnum.HIT_AND_RUN, 2), SkillFormulaVersion.SCALING_SKILLS_V1);
        final var v2 = personage(Position.FRONT, HEALTH, attacks(1), 0,
            Map.of(ActiveEnum.HIT_AND_RUN, 2), SkillFormulaVersion.SCALING_SKILLS_V2);
        final var v3 = mobile(2, 1);
        final var target = target(Position.FRONT);
        final var random = new MobilityRandom(4_001);
        final var context = new BattleContext(List.of(v1, v2, v3), List.of(target), random);
        final var log = new BattleActionLog();

        v1.move(context, log, 1);
        v2.move(context, log, 2);
        v3.move(context, log, 3);

        Assertions.assertEquals(List.of(AttackAccess.HIT_AND_RUN, AttackAccess.HIT_AND_RUN, AttackAccess.NORMAL),
            attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList());
        Assertions.assertEquals(5, v1.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(10, v2.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(0, v3.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(1, random.hitAndRunChecks);
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void penetrationInheritsTheOpeningDamageCooldownAndExactlyTwoRemoteFollowUps(BattleTempoVersion tempo) {
        final var source = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.PENETRATION, 2), V3);
        final var target = target(Position.FRONT);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom(), tempo);
        context.moveBackward(source, 2);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        final var skillDamage = events(log, BattleEvent.ScalingSkillDamage.class).getFirst();
        final int expectedPacket = tempo.scaleDamage(ATTACK);
        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempts(log).getFirst().access()),
            () -> Assertions.assertEquals(ActiveEnum.PENETRATION, skillDamage.skill()),
            () -> Assertions.assertEquals(Map.of(AttackType.SLASH, ATTACK), skillDamage.basis()),
            () -> Assertions.assertEquals(12, skillDamage.coefficientNumerator()),
            () -> Assertions.assertEquals(12, skillDamage.coefficientDenominator()),
            () -> Assertions.assertEquals(expectedPacket, skillDamage.damageTaken()),
            () -> Assertions.assertEquals(HEALTH - 2 * expectedPacket, target.health()),
            () -> Assertions.assertEquals(6, source.scalingSkills().cooldown(ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(Optional.of(target.id()), source.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(2, source.scalingSkills().penetrationFollowUpsRemaining())
        );

        source.move(context, log, 2);

        Assertions.assertEquals(2, attempts(log).size());
        Assertions.assertEquals(1, source.scalingSkills().penetrationFollowUpsRemaining());
        Assertions.assertEquals(5, source.scalingSkills().cooldown(ActiveEnum.PENETRATION));
        source.move(context, log, 3);
        Assertions.assertEquals(3, attempts(log).size());
        Assertions.assertEquals(HEALTH - 4 * expectedPacket, target.health());
        Assertions.assertTrue(source.scalingSkills().penetrationTargetId().isEmpty());
        Assertions.assertEquals(0, source.scalingSkills().penetrationFollowUpsRemaining());
        source.move(context, log, 4);

        Assertions.assertEquals(3, attempts(log).size(), "Only two remote follow-ups belong to the opening");
        Assertions.assertEquals(1, events(log, BattleEvent.MovedTowardEnemy.class).size());
        Assertions.assertEquals(1, windows(log, ActiveEnum.PENETRATION).size());
        Assertions.assertEquals(1, events(log, BattleEvent.ScalingSkillDamage.class).size());
        Assertions.assertEquals(3, source.scalingSkills().cooldown(ActiveEnum.PENETRATION));
    }

    @Test
    void penetrationOpeningHasPriorityOverAnAvailableHitAndRunWindow() {
        final var source = personage(Position.FRONT, HEALTH, attacks(1), 0,
            Map.of(ActiveEnum.PENETRATION, 2, ActiveEnum.HIT_AND_RUN, 4), V3);
        final var target = target(Position.FRONT);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom(6_000));
        context.moveBackward(source, 1);
        final int before = source.currentPosition();
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(AttackAccess.PENETRATION, attempts(log).getFirst().access());
        Assertions.assertEquals(1, windows(log, ActiveEnum.PENETRATION).size());
        Assertions.assertTrue(windows(log, ActiveEnum.HIT_AND_RUN).isEmpty());
        Assertions.assertEquals(0, source.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(before, source.currentPosition());
        Assertions.assertEquals(Optional.of(target.id()), source.scalingSkills().penetrationTargetId());
        Assertions.assertEquals(2, source.scalingSkills().penetrationFollowUpsRemaining());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void penetrationNeverLocksADodgedOrAlreadyDefeatedTarget(boolean dodged) {
        final var source = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.PENETRATION, 2), V3);
        final var target = personage(Position.FRONT, dodged ? HEALTH : 50, attacks(3), dodged ? 100 : 0, Map.of(), V3);
        final var context = new BattleContext(List.of(source), List.of(target), new MobilityRandom());
        context.moveBackward(source, 1);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        Assertions.assertEquals(dodged, attempts(log).getFirst().dodged());
        Assertions.assertEquals(dodged, target.isAlive());
        Assertions.assertEquals(6, source.scalingSkills().cooldown(ActiveEnum.PENETRATION));
        Assertions.assertTrue(source.scalingSkills().penetrationTargetId().isEmpty());
        Assertions.assertEquals(0, source.scalingSkills().penetrationFollowUpsRemaining());
    }

    @Test
    void penetrationInterceptionInheritsTheV2LockOnTheActualGuard() {
        final var source = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.PENETRATION, 2), V3);
        final var ward = target(Position.MID);
        final var guard = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.GUARD, 4), V3);
        final var context = new BattleContext(List.of(source), List.of(ward, guard), new MobilityRandom());
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        final var selection = events(log, BattleEvent.TargetSelected.class).getFirst();
        Assertions.assertEquals(ward.id(), selection.originalTargetId());
        Assertions.assertEquals(guard.id(), selection.finalTargetId());
        Assertions.assertEquals(1, windows(log, ActiveEnum.PENETRATION).size());
        Assertions.assertEquals(Optional.of(guard.id()), source.scalingSkills().penetrationTargetId());
        Assertions.assertEquals(2, source.scalingSkills().penetrationFollowUpsRemaining());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void v2AndV3KeepTwoRemoteFollowUpsWhenAnOrdinaryTargetIsAvailable(boolean historicalV2) {
        final var version = historicalV2 ? SkillFormulaVersion.SCALING_SKILLS_V2 : V3;
        final var source = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.PENETRATION, 2), version);
        final var far = target(Position.BACK);
        final var close = target(Position.FRONT);
        final var context = new BattleContext(List.of(source), List.of(far, close), new MobilityRandom());
        final var log = new BattleActionLog();

        for (int turn = 1; turn <= 4; turn++) {
            source.move(context, log, turn);
        }

        Assertions.assertEquals(List.of(far.id(), far.id(), far.id(), close.id()),
            attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::originalTargetId).toList());
        Assertions.assertEquals(1, windows(log, ActiveEnum.PENETRATION).size());
        Assertions.assertEquals(1, events(log, BattleEvent.ScalingSkillDamage.class).size());
        Assertions.assertTrue(source.scalingSkills().penetrationTargetId().isEmpty());
    }

    @Test
    void mixedV2AndV3PenetrationKeepIdenticalOpeningAndFollowUpRules() {
        final var v2 = personage(Position.FRONT, HEALTH, attacks(1), 0,
            Map.of(ActiveEnum.PENETRATION, 2), SkillFormulaVersion.SCALING_SKILLS_V2);
        final var v3 = personage(Position.FRONT, HEALTH, attacks(1), 0, Map.of(ActiveEnum.PENETRATION, 2), V3);
        final var far = target(Position.BACK);
        final var close = target(Position.FRONT);
        final var context = new BattleContext(List.of(v2, v3), List.of(far, close), new MobilityRandom());
        final var log = new BattleActionLog();

        for (int turn = 1; turn <= 4; turn++) {
            v2.move(context, log, 2 * turn - 1);
            v3.move(context, log, 2 * turn);
        }

        for (final var source : List.of(v2, v3)) {
            final var sourceAttempts = attempts(log).stream().filter(attempt -> attempt.attackerId().equals(source.id())).toList();
            Assertions.assertEquals(List.of(far.id(), far.id(), far.id(), close.id()),
                sourceAttempts.stream().map(BattleTraceEvent.NormalAttackAttempt::originalTargetId).toList());
            Assertions.assertEquals(List.of(
                AttackAccess.PENETRATION, AttackAccess.PENETRATION, AttackAccess.PENETRATION, AttackAccess.NORMAL
            ), sourceAttempts.stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList());
            Assertions.assertTrue(source.scalingSkills().penetrationTargetId().isEmpty());
            Assertions.assertEquals(0, source.scalingSkills().penetrationFollowUpsRemaining());
            Assertions.assertEquals(3, source.scalingSkills().cooldown(ActiveEnum.PENETRATION));
        }
        Assertions.assertEquals(2, windows(log, ActiveEnum.PENETRATION).size());
        Assertions.assertEquals(2, events(log, BattleEvent.ScalingSkillDamage.class).size());
    }

    private static BattlePersonage mobile(int points, int range) {
        return personage(Position.FRONT, HEALTH, attacks(range), 0, Map.of(ActiveEnum.HIT_AND_RUN, points), V3);
    }

    private static BattlePersonage target(Position position) {
        return personage(position, HEALTH, attacks(3), 0, Map.of(), V3);
    }

    private static List<ItemAttack> attacks(int range) {
        return List.of(new ItemAttack(AttackType.SLASH, 1, range, ATTACK));
    }

    private static BattlePersonage personage(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int dodge,
        Map<ActiveEnum, Integer> points,
        SkillFormulaVersion version
    ) {
        final var item = new Item(new ItemObject(
            null, Set.of(), attacks, Optional.empty(), health, 0, dodge, 0, 100, 10, 0,
            ItemProgressionVersion.V1, Map.of()
        ), Optional.empty(), ItemRarity.COMMON);
        return version == V3
            ? BattlePersonage.forScalingSkillsV3Combat(List.of(item), position, points, Optional.empty())
            : BattlePersonage.forScalingSkills(List.of(item), position, points, version);
    }

    private static BattleContext context(
        BattlePersonage source,
        BattlePersonage target,
        BattleRandom random,
        boolean reverse
    ) {
        return reverse
            ? new BattleContext(List.of(target), List.of(source), random)
            : new BattleContext(List.of(source), List.of(target), random);
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attempts(BattleActionLog log) {
        return log.traceEvents().stream().filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast).toList();
    }

    private static List<BattleEvent.SkillWindowUsed> windows(BattleActionLog log, ActiveEnum skill) {
        return events(log, BattleEvent.SkillWindowUsed.class).stream().filter(event -> event.skill() == skill).toList();
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static final class MobilityRandom implements BattleRandom {
        private final Deque<Integer> hitAndRunRolls = new ArrayDeque<>();
        private int hitAndRunChecks;

        private MobilityRandom(int... rolls) {
            for (final int roll : rolls) {
                hitAndRunRolls.addLast(roll);
            }
        }

        @Override
        public boolean chance(String sequence, int basisPoints) {
            if (sequence.startsWith("skill-chance:HIT_AND_RUN:")) {
                hitAndRunChecks++;
            }
            return BattleRandom.super.chance(sequence, basisPoints);
        }

        @Override
        public int nextInt(String sequence, int minimum, int maximum) {
            if (sequence.startsWith("skill-chance:HIT_AND_RUN:")) {
                Assertions.assertFalse(hitAndRunRolls.isEmpty(), "Unexpected extra HitAndRun roll");
                final int roll = hitAndRunRolls.removeFirst();
                Assertions.assertTrue(roll >= minimum && roll <= maximum);
                return roll;
            }
            if (sequence.startsWith("target-selection:") || sequence.startsWith("skill-chance:")) {
                return minimum;
            }
            if (sequence.startsWith("skill-damage-")) {
                return 0;
            }
            return minimum + (maximum - minimum) / 2;
        }
    }
}
