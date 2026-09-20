package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.BattleEvent.ThreatReason;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class VersionedBattleBoundaryTest {
    private static final int LARGE_HEALTH = 20_000;
    private static final int DEFAULT_SPEED = 100;
    private static final int DEFAULT_THREAT = 10;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rangeWindowsAndGuardUseOneSelectionAndApprovedOrder(boolean originalBeyondHitAndRun) {
        final var attacker = scalingPersonageV2(
            Position.MID,
            LARGE_HEALTH,
            List.of(
                attack(AttackType.SLASH, 1, 2, 100),
                attack(AttackType.BLUNT, 1, 1, 40)
            ),
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            Map.of(ActiveEnum.HIT_AND_RUN, 8, ActiveEnum.PENETRATION, 8)
        );
        final var originalTarget = personage(
            originalBeyondHitAndRun ? Position.BACK : Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, originalBeyondHitAndRun ? 3 : 2, 1))
        );
        final var guard = scalingPersonageV2(
            originalBeyondHitAndRun ? Position.MID : Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 2, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var firstBack = personage(Position.BACK, LARGE_HEALTH, List.of());
        final var firstFront = personage(Position.FRONT, LARGE_HEALTH, List.of());
        final var secondFront = personage(Position.FRONT, LARGE_HEALTH, List.of());
        final var secondTeam = List.of(originalTarget, guard, secondFront);
        final var context = new BattleContext(
            List.of(attacker, firstBack, firstFront),
            secondTeam,
            new ScriptedRandom()
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var selection = onlyEvent(log, BattleEvent.TargetSelected.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(originalTarget.id(), selection.originalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selection.finalTargetId()),
            () -> Assertions.assertEquals(originalBeyondHitAndRun ? 3 : 2, selection.distance())
        );
        final var interception = onlyEvent(log, BattleEvent.AttackIntercepted.class);
        Assertions.assertEquals(guard.id(), interception.interceptorId());
        Assertions.assertEquals(
            List.of(
                "target",
                "interception",
                "window:GUARD",
                "window:PENETRATION",
                "damage"
            ),
            causalLabels(log)
        );
        Assertions.assertEquals(
            Map.of(AttackType.SLASH, 100, AttackType.BLUNT, 40),
            onlyEvent(log, BattleEvent.DamageReceived.class).roll().attack()
        );
        Assertions.assertEquals(
            3,
            attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION)
        );
        Assertions.assertEquals(
            0,
            attacker.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN)
        );
        Assertions.assertEquals(3, context.teamSkillState(originalTarget).guardAttemptsRemaining());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void guardProtectsAnAllyExactlyOneLineBehindInBothFieldDirections(boolean guardedTeamFirst) {
        final var attacker = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 2, 10))
        );
        final var ward = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 2, 1))
        );
        final var guard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var guardedTeam = List.of(ward, guard);
        final var context = guardedTeamFirst
            ? new BattleContext(guardedTeam, List.of(attacker), new ScriptedRandom())
            : new BattleContext(List.of(attacker), guardedTeam, new ScriptedRandom());
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var selection = onlyEvent(log, BattleEvent.TargetSelected.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ward.id(), selection.originalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selection.finalTargetId()),
            () -> Assertions.assertEquals(
                guard.currentPosition() - guard.advanceDirection().indexDelta(),
                ward.currentPosition()
            ),
            () -> Assertions.assertEquals(guardedTeamFirst, guard.advanceDirection()
                == BattleAdvanceDirection.TOWARD_SECOND_TEAM),
            () -> Assertions.assertEquals(3, context.teamSkillState(ward).guardAttemptsRemaining())
        );
        Assertions.assertEquals(guard.id(), onlyEvent(log, BattleEvent.AttackIntercepted.class).interceptorId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void guardDoesNotConsumeForSameLineOrTwoLinesBehindInBothFieldDirections(boolean guardedTeamFirst) {
        assertGuardMissesGeometry(guardedTeamFirst, Position.FRONT, 1);
        assertGuardMissesGeometry(guardedTeamFirst, Position.BACK, 3);
    }

    @Test
    void guardDoesNotInterceptOrConsumeCooldownForDirectAndPeriodicSkillDamage() {
        assertGuardBypassesDirectCounterAttack();
        assertGuardBypassesPeriodicBleeding();
    }

    @Test
    void guardChoosesHighestThreatAndThenLowestUuid() {
        final var original = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 2, 1))
        );
        final var lowThreatGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var highThreatGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.GUARD, 8)
        );
        highThreatGuard.changeBonusThreat(20);
        assertGuardWinner(original, List.of(lowThreatGuard, highThreatGuard), highThreatGuard);

        final var tiedOriginal = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 2, 1))
        );
        final var firstTiedGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var secondTiedGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var expected = firstTiedGuard.id().compareTo(secondTiedGuard.id()) < 0
            ? firstTiedGuard
            : secondTiedGuard;
        assertGuardWinner(tiedOriginal, List.of(firstTiedGuard, secondTiedGuard), expected);
    }

    @Test
    void guardRejectsOwnerSameLineAndTwoLinesBehindWithoutConsumingCooldown() {
        final var ownerTarget = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var ownerAttacker = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 2, 10))
        );
        final var ownerContext = new BattleContext(
            List.of(ownerAttacker),
            List.of(ownerTarget),
            new ScriptedRandom()
        );
        assertNoGuardInterception(ownerAttacker, ownerTarget, ownerContext);
        Assertions.assertEquals(0, ownerContext.teamSkillState(ownerTarget).guardAttemptsRemaining());

        final var sameLineTarget = personage(Position.FRONT, LARGE_HEALTH, List.of());
        final var sameLineGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var sameLineAttacker = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 10))
        );
        final var sameLineContext = new BattleContext(
            List.of(sameLineAttacker),
            List.of(sameLineTarget, sameLineGuard),
            new ScriptedRandom()
        );
        assertNoGuardInterception(sameLineAttacker, sameLineTarget, sameLineContext);
        Assertions.assertEquals(0, sameLineContext.teamSkillState(sameLineTarget).guardAttemptsRemaining());

        final var twoLinesBehindTarget = personage(Position.BACK, LARGE_HEALTH, List.of());
        final var twoLinesBehindGuard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var penetratingAttacker = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 10)),
            Map.of(ActiveEnum.PENETRATION, 8)
        );
        final var twoLinesBehindContext = new BattleContext(
            List.of(penetratingAttacker),
            List.of(twoLinesBehindTarget, twoLinesBehindGuard),
            new ScriptedRandom()
        );
        assertNoGuardInterception(penetratingAttacker, twoLinesBehindTarget, twoLinesBehindContext);
        Assertions.assertEquals(
            0,
            twoLinesBehindContext.teamSkillState(twoLinesBehindTarget).guardAttemptsRemaining()
        );
    }

    @ParameterizedTest
    @EnumSource(InvalidTempoState.class)
    void tempoBreakDoesNotConsumeWindowForInvalidTarget(InvalidTempoState state) {
        final var source = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            Map.of(ActiveEnum.TEMPO_BREAK, 5)
        );
        final int speed = state == InvalidTempoState.ZERO_SPEED ? 0 : DEFAULT_SPEED;
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            0,
            0,
            speed,
            DEFAULT_THREAT,
            0,
            Map.of()
        );
        switch (state) {
            case READY -> {
                target.setInitiativeGaugeForTest(950);
                Assertions.assertTrue(target.tick(new BattleActionLog(), 0));
            }
            case IMMUNE -> {
                target.setInitiativeGaugeForTest(200);
                target.scalingSkills().setTempoBreakImmune(true);
            }
            case ZERO_GAUGE -> target.setInitiativeGaugeForTest(0);
            case ZERO_SPEED -> target.setInitiativeGaugeForTest(200);
        }
        final int gaugeBefore = target.initiativeGauge();
        final var log = new BattleActionLog();

        source.move(new BattleContext(List.of(source), List.of(target), new ScriptedRandom()), log, 1);

        Assertions.assertEquals(1, events(log, BattleEvent.DamageReceived.class).size());
        Assertions.assertTrue(events(log, BattleEvent.InitiativeDelayed.class).isEmpty());
        Assertions.assertTrue(skillWindows(log, ActiveEnum.TEMPO_BREAK).isEmpty());
        Assertions.assertEquals(0, source.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
        Assertions.assertEquals(gaugeBefore, target.initiativeGauge());
    }

    @Test
    void tempoBreakUsesExactTargetWeightsAndLegacyWeightsDuringCooldown() {
        final var readySource = tempoSource(0, 1);
        readySource.setTargetingTactic(
            ru.homyakin.seeker.game.battle.targeting.TargetingTactic.INITIATIVE_INTERCEPTION
        );
        final var invalidReadyTarget = readyTarget();
        final var validTarget = tempoTarget(600);
        final var readyRandom = new ScriptedRandom(11, Set.of());
        final var readyLog = new BattleActionLog();

        readySource.move(
            new BattleContext(List.of(readySource), List.of(invalidReadyTarget, validTarget), readyRandom),
            readyLog,
            1
        );

        Assertions.assertEquals(validTarget.id(), onlyEvent(readyLog, BattleEvent.TargetSelected.class).originalTargetId());
        Assertions.assertEquals(30, readyRandom.firstCall("target-selection:").maximum());
        final var delay = onlyEvent(readyLog, BattleEvent.InitiativeDelayed.class);
        Assertions.assertEquals(128, delay.amount());
        Assertions.assertEquals(472, delay.gaugeAfter());

        final var coolingSource = tempoSource(0, 1);
        coolingSource.setTargetingTactic(
            ru.homyakin.seeker.game.battle.targeting.TargetingTactic.INITIATIVE_INTERCEPTION
        );
        coolingSource.scalingSkills().startCooldown(ActiveEnum.TEMPO_BREAK, 2);
        final var coolingReadyTarget = readyTarget();
        final var coolingValidTarget = tempoTarget(600);
        final var coolingRandom = new ScriptedRandom(11, Set.of());
        final var coolingLog = new BattleActionLog();

        coolingSource.move(
            new BattleContext(
                List.of(coolingSource),
                List.of(coolingReadyTarget, coolingValidTarget),
                coolingRandom
            ),
            coolingLog,
            1
        );

        Assertions.assertEquals(
            coolingReadyTarget.id(),
            onlyEvent(coolingLog, BattleEvent.TargetSelected.class).originalTargetId()
        );
        Assertions.assertEquals(50, coolingRandom.firstCall("target-selection:").maximum());
        Assertions.assertTrue(events(coolingLog, BattleEvent.InitiativeDelayed.class).isEmpty());
        Assertions.assertEquals(1, coolingSource.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
    }

    @ParameterizedTest
    @MethodSource("tempoAmountCases")
    void tempoBreakRoundsImpactAndCapsByCurrentGauge(
        int impact,
        int points,
        int gauge,
        int expectedRemoved
    ) {
        final var source = tempoSource(impact, points);
        final var target = tempoTarget(gauge);
        final var log = new BattleActionLog();

        source.move(new BattleContext(List.of(source), List.of(target), new ScriptedRandom()), log, 1);

        final var delay = onlyEvent(log, BattleEvent.InitiativeDelayed.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(expectedRemoved, delay.amount()),
            () -> Assertions.assertEquals(gauge - expectedRemoved, delay.gaugeAfter()),
            () -> Assertions.assertEquals(1, source.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertTrue(target.scalingSkills().tempoBreakImmune()),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.TEMPO_BREAK).size())
        );
    }

    @Test
    void zeroSavedBasisDoesNotRollOrChangeAttackDependentSkills() {
        final var hitAttacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 2, 2, 100)),
            Map.of(
                ActiveEnum.DOUBLE_ATTACK, 8,
                ActiveEnum.BLEEDING, 8,
                ActiveEnum.ACCUMULATION, 8
            )
        );
        final var hitTarget = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 2, 2, 100)),
            Map.of(ActiveEnum.COUNTER_ATTACK, 8, ActiveEnum.THORNS, 8)
        );
        final var hitRandom = new ScriptedRandom();
        final var hitLog = new BattleActionLog();

        hitAttacker.move(new BattleContext(List.of(hitAttacker), List.of(hitTarget), hitRandom), hitLog, 1);

        final var normalHit = onlyEvent(hitLog, BattleEvent.DamageReceived.class);
        Assertions.assertTrue(normalHit.roll().attack().isEmpty());
        Assertions.assertEquals(0, normalHit.damageTaken());
        assertNoScalingSkillActivity(hitAttacker, hitLog, hitRandom);

        final var dodgeAttacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 2, 2, 100)),
            Map.of(ActiveEnum.PRECISE_STRIKE, 8)
        );
        final var dodgeTarget = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 2, 2, 100)),
            0,
            100,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            Map.of(ActiveEnum.FEINT, 8)
        );
        final var dodgeRandom = new ScriptedRandom();
        final var dodgeLog = new BattleActionLog();

        dodgeAttacker.move(
            new BattleContext(List.of(dodgeAttacker), List.of(dodgeTarget), dodgeRandom),
            dodgeLog,
            1
        );

        Assertions.assertEquals(1, events(dodgeLog, BattleEvent.AttackDodged.class).size());
        assertNoScalingSkillActivity(dodgeAttacker, dodgeLog, dodgeRandom);
    }

    @Test
    void threatUsesDamageLossAndMutuallyExclusiveAttemptOutcome() {
        assertLiveHitThreat();
        assertDodgedAttemptThreat();
        assertPreciseStrikeKillThreat();
        assertDelayedBleedingKillThreat();
    }

    @ParameterizedTest
    @MethodSource("typedDamageCases")
    void scalingDamageKeepsAttackTypesAndAppliesEveryDefenseType(
        Map<AttackType, Integer> attackByType,
        Map<DefenseType, Integer> defenses,
        int expectedDamage
    ) {
        final var attacks = attackByType.entrySet().stream()
            .map(entry -> attack(entry.getKey(), 1, 1, entry.getValue()))
            .toList();
        final var attacker = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            attacks,
            Map.of(ActiveEnum.DOUBLE_ATTACK, 8)
        );
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            defenses
        );
        final var log = new BattleActionLog();

        attacker.move(new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom()), log, 1);

        final var skillDamage = onlyEvent(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ActiveEnum.DOUBLE_ATTACK, skillDamage.skill()),
            () -> Assertions.assertEquals(attackByType, skillDamage.basis()),
            () -> Assertions.assertEquals(30, skillDamage.coefficientNumerator()),
            () -> Assertions.assertEquals(100, skillDamage.coefficientDenominator()),
            () -> Assertions.assertFalse(skillDamage.periodic()),
            () -> Assertions.assertEquals(expectedDamage, skillDamage.damageTaken())
        );
    }

    @Test
    void movementStopsAtRearBoundaryAndFailedRollsDoNotMove() {
        assertHitAndRunAtRearBoundary();
        assertRetreatAndKnockbackAtRearBoundary();
        assertFailedRetreatAndKnockbackDoNotMove();
    }

    private static void assertGuardWinner(
        BattlePersonage original,
        List<BattlePersonage> guards,
        BattlePersonage expected
    ) {
        final var attacker = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 2, 10))
        );
        final var targets = new ArrayList<BattlePersonage>();
        targets.add(original);
        targets.addAll(guards);
        final var log = new BattleActionLog();

        attacker.move(new BattleContext(List.of(attacker), targets, new ScriptedRandom()), log, 1);

        Assertions.assertEquals(expected.id(), onlyEvent(log, BattleEvent.TargetSelected.class).finalTargetId());
        Assertions.assertEquals(expected.id(), onlyEvent(log, BattleEvent.AttackIntercepted.class).interceptorId());
        Assertions.assertEquals(List.of("target", "interception", "window:GUARD", "damage"), causalLabels(log));
    }

    private static void assertGuardBypassesDirectCounterAttack() {
        final var ward = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 2, 100))
        );
        final var guard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var counterOwner = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 2, 100)),
            Map.of(ActiveEnum.COUNTER_ATTACK, 8)
        );
        final var context = new BattleContext(
            List.of(ward, guard),
            List.of(counterOwner),
            new ScriptedRandom()
        );
        final var log = new BattleActionLog();

        ward.move(context, log, 1);

        final var directDamage = onlyEvent(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ActiveEnum.COUNTER_ATTACK, directDamage.skill()),
            () -> Assertions.assertEquals(ward.id(), directDamage.targetId()),
            () -> Assertions.assertFalse(directDamage.periodic()),
            () -> Assertions.assertTrue(events(log, BattleEvent.AttackIntercepted.class).isEmpty()),
            () -> Assertions.assertEquals(0, context.teamSkillState(ward).guardAttemptsRemaining()),
            () -> Assertions.assertEquals(LARGE_HEALTH, guard.health())
        );
    }

    private static void assertGuardBypassesPeriodicBleeding() {
        final var source = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 2, 1))
        );
        final var ward = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 2, 1))
        );
        final var guard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        ward.addOrReplaceScalingBleeding(new ScalingBleedingEffect(
            source,
            Map.of(AttackType.SLASH, 100),
            1,
            1,
            1
        ));
        final var context = new BattleContext(
            List.of(ward, guard),
            List.of(source),
            new ScriptedRandom()
        );
        final var log = new BattleActionLog();

        ward.move(context, log, 1);

        final var periodicDamage = onlyEvent(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ActiveEnum.BLEEDING, periodicDamage.skill()),
            () -> Assertions.assertEquals(ward.id(), periodicDamage.targetId()),
            () -> Assertions.assertTrue(periodicDamage.periodic()),
            () -> Assertions.assertTrue(events(log, BattleEvent.AttackIntercepted.class).isEmpty()),
            () -> Assertions.assertEquals(0, context.teamSkillState(ward).guardAttemptsRemaining()),
            () -> Assertions.assertEquals(LARGE_HEALTH, guard.health())
        );
    }

    private static void assertGuardMissesGeometry(
        boolean guardedTeamFirst,
        Position wardPosition,
        int range
    ) {
        final var attacker = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, range, 10))
        );
        final var ward = personage(
            wardPosition,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, range, 1))
        );
        final var guard = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            Map.of(ActiveEnum.GUARD, 8)
        );
        final var guardedTeam = List.of(ward, guard);
        final var context = guardedTeamFirst
            ? new BattleContext(guardedTeam, List.of(attacker), new ScriptedRandom())
            : new BattleContext(List.of(attacker), guardedTeam, new ScriptedRandom());

        assertNoGuardInterception(attacker, ward, context);
        Assertions.assertEquals(0, context.teamSkillState(ward).guardAttemptsRemaining());
    }

    private static void assertNoGuardInterception(
        BattlePersonage attacker,
        BattlePersonage original,
        BattleContext context
    ) {
        final var log = new BattleActionLog();
        attacker.move(context, log, 1);

        final var selection = onlyEvent(log, BattleEvent.TargetSelected.class);
        Assertions.assertEquals(original.id(), selection.originalTargetId());
        Assertions.assertEquals(original.id(), selection.finalTargetId());
        Assertions.assertTrue(events(log, BattleEvent.AttackIntercepted.class).isEmpty());
        Assertions.assertTrue(skillWindows(log, ActiveEnum.GUARD).isEmpty());
    }

    private static BattlePersonage tempoSource(int impact, int points) {
        return scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            impact,
            Map.of(),
            Map.of(ActiveEnum.TEMPO_BREAK, points)
        );
    }

    private static BattlePersonage tempoTarget(int gauge) {
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of()
        );
        target.setInitiativeGaugeForTest(gauge);
        return target;
    }

    private static BattlePersonage readyTarget() {
        final var target = tempoTarget(950);
        Assertions.assertTrue(target.tick(new BattleActionLog(), 0));
        return target;
    }

    private static Stream<Arguments> tempoAmountCases() {
        return Stream.of(
            Arguments.of(10, 5, 300, 300),
            Arguments.of(20, 8, 40, 40)
        );
    }

    private static void assertNoScalingSkillActivity(
        BattlePersonage attacker,
        BattleActionLog log,
        ScriptedRandom random
    ) {
        Assertions.assertTrue(events(log, BattleEvent.ScalingSkillDamage.class).isEmpty());
        Assertions.assertTrue(events(log, BattleEvent.SkillWindowUsed.class).isEmpty());
        Assertions.assertTrue(events(log, BattleEvent.SkillChargeChanged.class).isEmpty());
        Assertions.assertEquals(0, attacker.scalingSkills().cooldown(ActiveEnum.BLEEDING));
        Assertions.assertEquals(0, attacker.scalingSkills().accumulationCharges());
        Assertions.assertTrue(random.calls().stream().noneMatch(call -> call.sequence().startsWith("skill-")));
    }

    private static void assertLiveHitThreat() {
        final var attacker = scalingPersonageV2(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var target = personage(Position.FRONT, LARGE_HEALTH, List.of());
        target.changeBonusThreat(10);
        final var log = new BattleActionLog();
        attacker.move(new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom()), log, 1);

        final var threat = events(log, BattleEvent.ThreatChanged.class);
        Assertions.assertEquals(2, threat.size());
        assertThreat(threat.get(0), target, attacker, null, -8, 12, ThreatReason.DAMAGE_TAKEN);
        assertThreat(threat.get(1), attacker, attacker, null, 5, 15, ThreatReason.NORMAL_HIT);
    }

    private static void assertDodgedAttemptThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            0,
            100,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of()
        );
        final var log = new BattleActionLog();
        attacker.move(new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom()), log, 1);

        Assertions.assertEquals(1, events(log, BattleEvent.AttackDodged.class).size());
        Assertions.assertTrue(events(log, BattleEvent.ThreatChanged.class).isEmpty());
    }

    private static void assertPreciseStrikeKillThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            Map.of(ActiveEnum.PRECISE_STRIKE, 8)
        );
        final var target = personage(
            Position.FRONT,
            80,
            List.of(),
            0,
            100,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of()
        );
        final var log = new BattleActionLog();
        attacker.move(new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom()), log, 1);

        Assertions.assertEquals(1, events(log, BattleEvent.AttackDodged.class).size());
        Assertions.assertEquals(
            ActiveEnum.PRECISE_STRIKE,
            onlyEvent(log, BattleEvent.ScalingSkillDamage.class).skill()
        );
        Assertions.assertEquals(1, events(log, BattleEvent.PersonageDefeated.class).size());
        final var threat = onlyEvent(log, BattleEvent.ThreatChanged.class);
        assertThreat(threat, attacker, attacker, null, 50, 60, ThreatReason.KILL);
    }

    private static void assertDelayedBleedingKillThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            Map.of(ActiveEnum.BLEEDING, 8)
        );
        final var target = personage(Position.FRONT, 130, List.of());
        final var context = new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom());
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        target.move(context, log, 2);

        final var attackerThreat = events(log, BattleEvent.ThreatChanged.class).stream()
            .filter(event -> event.personageId().equals(attacker.id()))
            .toList();
        Assertions.assertEquals(1, attackerThreat.size());
        assertThreat(
            attackerThreat.getFirst(),
            attacker,
            attacker,
            null,
            5,
            15,
            ThreatReason.NORMAL_HIT
        );
        final var bleeding = onlyEvent(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertTrue(bleeding.periodic());
        Assertions.assertEquals(ActiveEnum.BLEEDING, bleeding.skill());
        Assertions.assertEquals(1, events(log, BattleEvent.PersonageDefeated.class).size());
        Assertions.assertTrue(events(log, BattleEvent.ThreatChanged.class).stream()
            .noneMatch(event -> event.reason() == ThreatReason.KILL));
    }

    private static void assertThreat(
        BattleEvent.ThreatChanged event,
        BattlePersonage personage,
        BattlePersonage source,
        ActiveEnum skill,
        int delta,
        int resultingThreat,
        ThreatReason reason
    ) {
        Assertions.assertAll(
            () -> Assertions.assertEquals(personage.id(), event.personageId()),
            () -> Assertions.assertEquals(source.id(), event.sourceId()),
            () -> Assertions.assertEquals(skill, event.skill()),
            () -> Assertions.assertEquals(delta, event.delta()),
            () -> Assertions.assertEquals(resultingThreat, event.resultingThreat()),
            () -> Assertions.assertEquals(reason, event.reason())
        );
    }

    private static Stream<Arguments> typedDamageCases() {
        return Stream.of(
            Arguments.of(Map.of(AttackType.SLASH, 1_000), Map.of(DefenseType.CLOTH, 100), 260),
            Arguments.of(Map.of(AttackType.BLUNT, 1_000), Map.of(DefenseType.LEATHER, 100), 245),
            Arguments.of(Map.of(AttackType.PIERCE, 1_000), Map.of(DefenseType.PLATE, 100), 245),
            Arguments.of(Map.of(AttackType.MAGICAL, 1_000), Map.of(DefenseType.ARCANE, 100), 240),
            Arguments.of(
                Map.of(
                    AttackType.SLASH, 100,
                    AttackType.BLUNT, 100,
                    AttackType.PIERCE, 100,
                    AttackType.MAGICAL, 100
                ),
                Map.of(
                    DefenseType.CLOTH, 100,
                    DefenseType.LEATHER, 100,
                    DefenseType.PLATE, 100,
                    DefenseType.ARCANE, 100
                ),
                66
            )
        );
    }

    private static void assertHitAndRunAtRearBoundary() {
        final var attacker = scalingPersonageV2(
            Position.BACK,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            Map.of(ActiveEnum.HIT_AND_RUN, 8)
        );
        final var target = personage(Position.BACK, LARGE_HEALTH, List.of());
        final var log = new BattleActionLog();

        attacker.move(new BattleContext(List.of(attacker), List.of(target), new ScriptedRandom()), log, 1);

        Assertions.assertEquals(1, skillWindows(log, ActiveEnum.HIT_AND_RUN).size());
        Assertions.assertEquals(4, attacker.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
    }

    private static void assertRetreatAndKnockbackAtRearBoundary() {
        final var attacker = scalingPersonage(
            Position.BACK,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            100,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            Map.of(ActiveEnum.KNOCKBACK, 8)
        );
        final var target = scalingPersonage(
            Position.BACK,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.RETREAT, 8)
        );
        final var random = new ScriptedRandom();
        final var log = new BattleActionLog();

        attacker.move(new BattleContext(List.of(attacker), List.of(target), random), log, 1);

        Assertions.assertTrue(random.hasCall("skill-chance:RETREAT:"));
        Assertions.assertTrue(random.hasCall("skill-chance:KNOCKBACK:"));
        Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
    }

    private static void assertFailedRetreatAndKnockbackDoNotMove() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            100,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            Map.of(ActiveEnum.KNOCKBACK, 8)
        );
        final var target = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            Map.of(ActiveEnum.RETREAT, 8)
        );
        final var firstBack = personage(Position.BACK, LARGE_HEALTH, List.of());
        final var secondBack = personage(Position.BACK, LARGE_HEALTH, List.of());
        final var random = new ScriptedRandom(
            1,
            EnumSet.of(ActiveEnum.RETREAT, ActiveEnum.KNOCKBACK)
        );
        final var context = new BattleContext(
            List.of(attacker, firstBack),
            List.of(target, secondBack),
            random
        );
        final int positionBefore = target.currentPosition();
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        Assertions.assertTrue(random.hasCall("skill-chance:RETREAT:"));
        Assertions.assertTrue(random.hasCall("skill-chance:KNOCKBACK:"));
        Assertions.assertEquals(positionBefore, target.currentPosition());
        Assertions.assertTrue(events(log, BattleEvent.PersonageForcedMove.class).isEmpty());
    }

    private static List<String> causalLabels(BattleActionLog log) {
        return log.events().stream()
            .map(VersionedBattleBoundaryTest::causalLabel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    private static Optional<String> causalLabel(BattleEvent event) {
        if (event instanceof BattleEvent.TargetSelected) {
            return Optional.of("target");
        }
        if (event instanceof BattleEvent.AttackIntercepted) {
            return Optional.of("interception");
        }
        if (event instanceof BattleEvent.SkillWindowUsed window) {
            return Optional.of("window:" + window.skill().name());
        }
        if (event instanceof BattleEvent.DamageReceived) {
            return Optional.of("damage");
        }
        if (event instanceof BattleEvent.PersonageForcedMove move) {
            return Optional.of("move:" + move.skill().name());
        }
        return Optional.empty();
    }

    private static List<BattleEvent.SkillWindowUsed> skillWindows(BattleActionLog log, ActiveEnum skill) {
        return events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == skill)
            .toList();
    }

    private static <T extends BattleEvent> T onlyEvent(BattleActionLog log, Class<T> type) {
        final var result = events(log, type);
        Assertions.assertEquals(1, result.size(), () -> "Expected one " + type.getSimpleName());
        return result.getFirst();
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
        List<ItemAttack> attacks,
        Map<ActiveEnum, Integer> skills
    ) {
        return scalingPersonage(
            position,
            health,
            attacks,
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            skills
        );
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance,
        int speed,
        int threat,
        int impact,
        Map<DefenseType, Integer> defenses,
        Map<ActiveEnum, Integer> skills
    ) {
        return BattlePersonage.forScalingSkills(
            items(health, attacks, criticalChance, dodgeChance, speed, threat, impact, defenses),
            position,
            skills
        );
    }

    private static BattlePersonage scalingPersonageV2(
        Position position,
        int health,
        List<ItemAttack> attacks,
        Map<ActiveEnum, Integer> skills
    ) {
        return scalingPersonageV2(
            position,
            health,
            attacks,
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of(),
            skills
        );
    }

    private static BattlePersonage scalingPersonageV2(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance,
        int speed,
        int threat,
        int impact,
        Map<DefenseType, Integer> defenses,
        Map<ActiveEnum, Integer> skills
    ) {
        return BattlePersonage.forScalingSkills(
            items(health, attacks, criticalChance, dodgeChance, speed, threat, impact, defenses),
            position,
            skills,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static BattlePersonage personage(Position position, int health, List<ItemAttack> attacks) {
        return personage(
            position,
            health,
            attacks,
            0,
            0,
            DEFAULT_SPEED,
            DEFAULT_THREAT,
            0,
            Map.of()
        );
    }

    private static BattlePersonage personage(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance,
        int speed,
        int threat,
        int impact,
        Map<DefenseType, Integer> defenses
    ) {
        return new BattlePersonage(
            items(health, attacks, criticalChance, dodgeChance, speed, threat, impact, defenses),
            position
        );
    }

    private static List<Item> items(
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance,
        int speed,
        int threat,
        int impact,
        Map<DefenseType, Integer> defenses
    ) {
        final var result = new ArrayList<Item>();
        result.add(item(
            health,
            attacks,
            Optional.empty(),
            criticalChance,
            dodgeChance,
            speed,
            threat,
            impact
        ));
        for (final var type : DefenseType.values()) {
            final int defense = defenses.getOrDefault(type, 0);
            if (defense > 0) {
                result.add(item(
                    0,
                    List.of(),
                    Optional.of(new ItemDefense(type, defense)),
                    0,
                    0,
                    0,
                    0,
                    0
                ));
            }
        }
        return List.copyOf(result);
    }

    private static Item item(
        int health,
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense,
        int criticalChance,
        int dodgeChance,
        int speed,
        int threat,
        int impact
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                attacks,
                defense,
                health,
                criticalChance,
                dodgeChance,
                0,
                speed,
                threat,
                impact,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static ItemAttack attack(
        AttackType type,
        int minimumRange,
        int maximumRange,
        int amount
    ) {
        return new ItemAttack(type, minimumRange, maximumRange, amount);
    }

    private enum InvalidTempoState {
        READY,
        IMMUNE,
        ZERO_GAUGE,
        ZERO_SPEED,
    }

    private record RandomCall(String sequence, int minimum, int maximum) {
    }

    private static final class ScriptedRandom implements BattleRandom {
        private final int targetSelectionRoll;
        private final Set<ActiveEnum> failedSkills;
        private final List<RandomCall> calls = new ArrayList<>();

        private ScriptedRandom() {
            this(1, Set.of());
        }

        private ScriptedRandom(int targetSelectionRoll, Set<ActiveEnum> failedSkills) {
            this.targetSelectionRoll = targetSelectionRoll;
            this.failedSkills = Set.copyOf(failedSkills);
        }

        @Override
        public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
            calls.add(new RandomCall(sequence, minimumInclusive, maximumInclusive));
            if (sequence.startsWith("target-selection:")) {
                if (targetSelectionRoll < minimumInclusive || targetSelectionRoll > maximumInclusive) {
                    throw new IllegalArgumentException("Target roll is outside the requested range: " + sequence);
                }
                return targetSelectionRoll;
            }
            if (sequence.startsWith("normal-damage:")) {
                return minimumInclusive + (maximumInclusive - minimumInclusive) / 2;
            }
            if (sequence.startsWith("skill-damage-")) {
                return 0;
            }
            for (final var skill : failedSkills) {
                if (sequence.startsWith("skill-chance:" + skill.name() + ":")) {
                    return maximumInclusive;
                }
            }
            return minimumInclusive;
        }

        private List<RandomCall> calls() {
            return List.copyOf(calls);
        }

        private boolean hasCall(String prefix) {
            return calls.stream().anyMatch(call -> call.sequence().startsWith(prefix));
        }

        private RandomCall firstCall(String prefix) {
            return calls.stream()
                .filter(call -> call.sequence().startsWith(prefix))
                .findFirst()
                .orElseThrow();
        }
    }
}
