package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Placement;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

/** Exact active/control snapshots preregistered for the six directed revision-two role experiments. */
class Step12V2ControlledCausalityTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void berserkAddsExactlyTwentyTwoPointFivePercentOnlyAtThreshold(int level) {
        final var active = bruiserThresholdScenario(level, false, 0);
        final var control = bruiserThresholdScenario(level, true, 0);
        final var above = bruiserThresholdScenario(level, false, 1);

        final var activeAttempt = onlyAttempt(active);
        final var controlAttempt = onlyAttempt(control);
        final var aboveAttempt = onlyAttempt(above);
        final int base = ItemProgression.valueAtLevel(480, level);
        final int bonus = roundHalfUp(Math.multiplyExact(45L, base), 200);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                control.teams().evaluatedTeam().getFirst().id(),
                active.teams().evaluatedTeam().getFirst().id(),
                "paired snapshots must preserve the participant identity"
            ),
            () -> Assertions.assertEquals(Map.of(AttackType.BLUNT, base), controlAttempt.savedBasis()),
            () -> Assertions.assertEquals(Map.of(AttackType.BLUNT, base + bonus), activeAttempt.savedBasis()),
            () -> Assertions.assertEquals(Map.of(AttackType.BLUNT, base), aboveAttempt.savedBasis()),
            () -> Assertions.assertEquals(
                base + bonus,
                activeAttempt.savedBasis().get(AttackType.BLUNT),
                "the registered half-up result is an exact 22.5% bonus"
            ),
            () -> Assertions.assertTrue(used(active, ActiveEnum.BERSERK)),
            () -> Assertions.assertFalse(used(control, ActiveEnum.BERSERK)),
            () -> Assertions.assertFalse(used(above, ActiveEnum.BERSERK))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void bruiserMovedToBackMustSpendTurnsClosingBeforeFirstAttack(int level) {
        final var front = Step12V2CausalTestSupport.scenario(V2Matchup.BRUISER_V2, level);
        final var back = Step12V2CausalTestSupport.scenario(V2Matchup.BRUISER_V2, level);
        final var frontBruiser = Step12V2CausalTestSupport.named(
            front.teams().evaluatedTeam(),
            V2Build.BRUISER_BLUNT
        );
        final var backBruiser = Step12V2CausalTestSupport.named(
            back.teams().evaluatedTeam(),
            V2Build.BRUISER_BLUNT
        );
        final var frontGuardian = Step12V2CausalTestSupport.named(
            front.teams().opponents(),
            V2Build.GUARDIAN_BLUNT
        );
        final var backGuardian = Step12V2CausalTestSupport.named(
            back.teams().opponents(),
            V2Build.GUARDIAN_BLUNT
        );
        back.context().moveBackward(backBruiser, 2);
        front.random().forceTarget(frontGuardian);
        back.random().forceTarget(backGuardian);

        frontBruiser.move(front.context(), front.log(), 1);
        backBruiser.move(back.context(), back.log(), 1);
        backBruiser.move(back.context(), back.log(), 2);

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, attempts(front, frontBruiser).size()),
            () -> Assertions.assertTrue(attempts(back, backBruiser).isEmpty()),
            () -> Assertions.assertEquals(0, backBruiser.currentPosition() - 2),
            () -> Assertions.assertEquals(0, backBruiser.battlePersonageStats().normalDamageDealt())
        );

        backBruiser.move(back.context(), back.log(), 3);
        Assertions.assertEquals(1, attempts(back, backBruiser).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void guardianInterceptsOrdinaryDamageAndControlDoesNot(int level) {
        final var active = Step12V2CausalTestSupport.scenario(V2Matchup.BRUISER_V2, level);
        final var control = Step12V2CausalTestSupport.scenarioWithoutSkill(
            V2Matchup.BRUISER_V2,
            level,
            V2Build.GUARDIAN_BLUNT
        );
        final var activeAttacker = supportAt(active.teams().evaluatedTeam(), Position.FRONT);
        final var controlAttacker = supportAt(control.teams().evaluatedTeam(), Position.FRONT);
        final var activeWard = supportAt(active.teams().opponents(), Position.MID);
        final var controlWard = supportAt(control.teams().opponents(), Position.MID);
        final var activeGuardian = namedOpponent(active, V2Build.GUARDIAN_BLUNT);
        active.random().forceTarget(activeWard);
        control.random().forceTarget(controlWard);

        activeAttacker.move(active.context(), active.log(), 1);
        controlAttacker.move(control.context(), control.log(), 1);

        final var activeAttempt = attempts(active, activeAttacker).getFirst();
        final var controlAttempt = attempts(control, controlAttacker).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(activeWard.id(), activeAttempt.originalTargetId()),
            () -> Assertions.assertEquals(activeGuardian.id(), activeAttempt.finalTargetId()),
            () -> Assertions.assertTrue(activeAttempt.normalDamage() > 0),
            () -> Assertions.assertEquals(activeWard.maxHealth(), activeWard.health()),
            () -> Assertions.assertEquals(controlWard.id(), controlAttempt.originalTargetId()),
            () -> Assertions.assertEquals(controlWard.id(), controlAttempt.finalTargetId()),
            () -> Assertions.assertTrue(controlAttempt.normalDamage() > 0),
            () -> Assertions.assertTrue(controlWard.health() < controlWard.maxHealth()),
            () -> Assertions.assertEquals(1, Step12V2CausalTestSupport.events(
                active.log(),
                BattleEvent.AttackIntercepted.class
            ).size()),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                control.log(),
                BattleEvent.AttackIntercepted.class
            ).isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void guardianDoesNotInterceptDirectSkillDamage(int level) {
        final var scenario = Step12V2CausalTestSupport.guardianAgainstDirectSkill(level);
        final var ward = scenario.teams().evaluatedTeam().getFirst();
        final var guardian = scenario.teams().evaluatedTeam().get(1);
        final var thornsOwner = scenario.teams().opponents().getFirst();
        final int wardHealthBefore = ward.health();
        scenario.random().forceTarget(thornsOwner);

        ward.move(scenario.context(), scenario.log(), 1);

        final var skillDamage = Step12V2CausalTestSupport.events(
            scenario.log(),
            BattleEvent.ScalingSkillDamage.class
        ).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(ActiveEnum.THORNS, skillDamage.skill()),
            () -> Assertions.assertEquals(thornsOwner.id(), skillDamage.sourceId()),
            () -> Assertions.assertEquals(ward.id(), skillDamage.targetId()),
            () -> Assertions.assertFalse(skillDamage.periodic()),
            () -> Assertions.assertTrue(skillDamage.damageTaken() > 0),
            () -> Assertions.assertEquals(
                wardHealthBefore - skillDamage.damageTaken(),
                ward.health()
            ),
            () -> Assertions.assertEquals(guardian.maxHealth(), guardian.health()),
            () -> Assertions.assertTrue(scenario.context().teamSkillState(ward).guardReadyForAttempt()),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                scenario.log(),
                BattleEvent.AttackIntercepted.class
            ).isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void guardianTwoLinesFromPenetrationTargetDoesNotIntercept(int level) {
        final var scenario = Step12V2CausalTestSupport.scenario(
            List.of(new V2Placement(V2Build.ASSASSIN_PIERCE, Position.FRONT)),
            List.of(
                new V2Placement(V2Build.GUARDIAN_BLUNT, Position.FRONT),
                new V2Placement(V2Build.SUPPORT, Position.BACK)
            ),
            level
        );
        final var assassin = namedEvaluated(scenario, V2Build.ASSASSIN_PIERCE);
        final var guardian = namedOpponent(scenario, V2Build.GUARDIAN_BLUNT);
        final var ward = namedOpponent(scenario, V2Build.SUPPORT);
        scenario.context().moveBackward(ward, 1);
        Assertions.assertEquals(2, Math.abs(guardian.currentPosition() - ward.currentPosition()));
        scenario.random().forceTarget(ward);

        assassin.move(scenario.context(), scenario.log(), 1);

        final var attempt = attempts(scenario, assassin).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(ward.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(ward.id(), attempt.finalTargetId()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access()),
            () -> Assertions.assertTrue(attempt.distance() > attempt.ordinaryRange()),
            () -> Assertions.assertTrue(attempt.normalDamage() > 0),
            () -> Assertions.assertEquals(guardian.maxHealth(), guardian.health()),
            () -> Assertions.assertTrue(used(scenario, ActiveEnum.PENETRATION)),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                scenario.log(),
                BattleEvent.AttackIntercepted.class
            ).isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void hitAndRunRemovesExactlyTheBreakersNextAttemptAndCharge(int level) {
        final var active = Step12V2CausalTestSupport.scenario(V2Matchup.SKIRMISHER_V2, level);
        final var control = Step12V2CausalTestSupport.scenarioWithoutSkill(
            V2Matchup.SKIRMISHER_V2,
            level,
            V2Build.SKIRMISHER_SLASH
        );
        final var activeSkirmisher = namedEvaluated(active, V2Build.SKIRMISHER_SLASH);
        final var controlSkirmisher = namedEvaluated(control, V2Build.SKIRMISHER_SLASH);
        final var activeBreaker = namedOpponent(active, V2Build.BREAKER_CLOSE_SLASH);
        final var controlBreaker = namedOpponent(control, V2Build.BREAKER_CLOSE_SLASH);
        active.random().forceTarget(activeBreaker);
        control.random().forceTarget(controlBreaker);

        activeSkirmisher.move(active.context(), active.log(), 1);
        controlSkirmisher.move(control.context(), control.log(), 1);
        final var retreat = attempts(active, activeSkirmisher).getFirst();
        active.random().forceTarget(activeSkirmisher);
        control.random().forceTarget(controlSkirmisher);
        activeBreaker.move(active.context(), active.log(), 2);
        controlBreaker.move(control.context(), control.log(), 2);

        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, retreat.access()),
            () -> Assertions.assertEquals(retreat.lineBeforeRetreat() - 1, retreat.lineAfterRetreat()),
            () -> Assertions.assertEquals(0, attempts(active, activeBreaker).size()),
            () -> Assertions.assertEquals(0, activeBreaker.scalingSkills().accumulationCharges()),
            () -> Assertions.assertEquals(1, attempts(control, controlBreaker).size()),
            () -> Assertions.assertEquals(1, controlBreaker.scalingSkills().accumulationCharges())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void backBoundaryConsumesHitAndRunButDoesNotRemovePursuersAttempt(int level) {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.SKIRMISHER_V2, level);
        final var skirmisher = namedEvaluated(scenario, V2Build.SKIRMISHER_SLASH);
        final var breaker = namedOpponent(scenario, V2Build.BREAKER_CLOSE_SLASH);
        final var supports = new java.util.ArrayList<BattlePersonage>();
        supports.addAll(Step12V2CausalTestSupport.namedAll(
            scenario.teams().evaluatedTeam(),
            V2Build.CROSSBOW_SUPPORT
        ));
        supports.addAll(Step12V2CausalTestSupport.namedAll(
            scenario.teams().opponents(),
            V2Build.CROSSBOW_SUPPORT
        ));
        supports.forEach(Step12V2CausalTestSupport::defeat);
        scenario.context().moveBackward(skirmisher, 2);
        scenario.context().moveTowardEnemy(breaker);
        scenario.context().moveTowardEnemy(breaker);
        Assertions.assertEquals(1, skirmisher.distanceTo(breaker));
        scenario.random().forceTarget(breaker);

        skirmisher.move(scenario.context(), scenario.log(), 1);
        final var blockedRetreat = attempts(scenario, skirmisher).getFirst();
        scenario.random().forceTarget(skirmisher);
        breaker.move(scenario.context(), scenario.log(), 2);

        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, blockedRetreat.access()),
            () -> Assertions.assertEquals(0, blockedRetreat.lineBeforeRetreat()),
            () -> Assertions.assertEquals(0, blockedRetreat.lineAfterRetreat()),
            () -> Assertions.assertEquals(1, attempts(scenario, breaker).size()),
            () -> Assertions.assertEquals(1, breaker.scalingSkills().accumulationCharges())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void penetrationAloneReachesNamedRangerAtDistanceThree(int level) {
        final var active = Step12V2CausalTestSupport.scenario(V2Matchup.ASSASSIN_V2, level);
        final var control = Step12V2CausalTestSupport.scenarioWithoutSkill(
            V2Matchup.ASSASSIN_V2,
            level,
            V2Build.ASSASSIN_PIERCE
        );
        final var activeAssassin = namedEvaluated(active, V2Build.ASSASSIN_PIERCE);
        final var controlAssassin = namedEvaluated(control, V2Build.ASSASSIN_PIERCE);
        final var activeRanger = namedOpponent(active, V2Build.RANGER_PIERCE);
        final var controlRanger = namedOpponent(control, V2Build.RANGER_PIERCE);
        active.random().forceTarget(activeRanger);
        control.random().forceTarget(controlRanger);

        activeAssassin.move(active.context(), active.log(), 1);
        controlAssassin.move(control.context(), control.log(), 1);

        final var activeAttempt = attempts(active, activeAssassin).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(activeRanger.id(), activeAttempt.finalTargetId()),
            () -> Assertions.assertEquals(3, activeAttempt.distance()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, activeAttempt.access()),
            () -> Assertions.assertTrue(used(active, ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(
                controlRanger.maxHealth(),
                controlRanger.health(),
                "without Penetration the Ranger must receive no Assassin damage before the horizon"
            ),
            () -> Assertions.assertTrue(attempts(control, controlAssassin).stream().noneMatch(
                attempt -> attempt.finalTargetId().equals(controlRanger.id())
            ))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void penetrationDoesNotSpendWindowAtOrdinaryRangeAndDodgeSpendsFarWindow(int level) {
        final var near = Step12V2CausalTestSupport.scenario(V2Matchup.ASSASSIN_V2, level);
        final var dodged = Step12V2CausalTestSupport.scenario(V2Matchup.ASSASSIN_V2, level);
        final var nearAssassin = namedEvaluated(near, V2Build.ASSASSIN_PIERCE);
        final var dodgeAssassin = namedEvaluated(dodged, V2Build.ASSASSIN_PIERCE);
        final var nearRanger = namedOpponent(near, V2Build.RANGER_PIERCE);
        final var dodgeRanger = namedOpponent(dodged, V2Build.RANGER_PIERCE);
        near.context().moveTowardEnemy(nearRanger);
        Assertions.assertEquals(2, nearAssassin.distanceTo(nearRanger));
        near.random().forceTarget(nearRanger);
        dodged.random().forceTarget(dodgeRanger);
        dodged.random().forceDodgeOnAttempt(1);

        nearAssassin.move(near.context(), near.log(), 1);
        dodgeAssassin.move(dodged.context(), dodged.log(), 1);

        final var nearAttempt = attempts(near, nearAssassin).getFirst();
        final var dodgeAttempt = attempts(dodged, dodgeAssassin).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.NORMAL, nearAttempt.access()),
            () -> Assertions.assertFalse(used(near, ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, dodgeAttempt.access()),
            () -> Assertions.assertTrue(dodgeAttempt.dodged()),
            () -> Assertions.assertEquals(0, dodgeAttempt.normalDamage()),
            () -> Assertions.assertTrue(used(dodged, ActiveEnum.PENETRATION))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void rangerDistanceThreeToTwoReducesSavedBasisByExactlyOneThird(int level) {
        final var far = Step12V2CausalTestSupport.scenario(V2Matchup.RANGER_V2, level);
        final var near = Step12V2CausalTestSupport.scenario(V2Matchup.RANGER_V2, level);
        final var farRanger = namedEvaluated(far, V2Build.RANGER_MAGICAL);
        final var nearRanger = namedEvaluated(near, V2Build.RANGER_MAGICAL);
        final var farBruiser = namedOpponent(far, V2Build.BRUISER_MAGICAL_CLOTH);
        final var nearBruiser = namedOpponent(near, V2Build.BRUISER_MAGICAL_CLOTH);
        near.context().moveTowardEnemy(nearRanger);
        far.random().forceTarget(farBruiser);
        near.random().forceTarget(nearBruiser);

        farRanger.move(far.context(), far.log(), 1);
        nearRanger.move(near.context(), near.log(), 1);

        final var farAttempt = attempts(far, farRanger).getFirst();
        final var nearAttempt = attempts(near, nearRanger).getFirst();
        final int expectedFar = ItemProgression.valueAtLevel(480, level);
        final int expectedNear = ItemProgression.valueAtLevel(360, level);
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, farAttempt.distance()),
            () -> Assertions.assertEquals(2, nearAttempt.distance()),
            () -> Assertions.assertEquals(
                Map.of(AttackType.MAGICAL, expectedFar),
                farAttempt.savedBasis()
            ),
            () -> Assertions.assertEquals(
                Map.of(AttackType.MAGICAL, expectedNear),
                nearAttempt.savedBasis()
            ),
            () -> Assertions.assertEquals(3L * expectedFar, 4L * expectedNear),
            () -> Assertions.assertTrue(nearAttempt.normalDamage() > 0),
            () -> Assertions.assertTrue(farAttempt.normalDamage() > nearAttempt.normalDamage())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void accumulationAddsExactFourthAttemptDischargeAndControlDoesNot(int level) {
        final var active = breakerDuel(level, false, false);
        final var control = breakerDuel(level, true, false);
        final var activeBreaker = active.teams().evaluatedTeam().getFirst();
        final var controlBreaker = control.teams().evaluatedTeam().getFirst();
        final var activeTarget = active.teams().opponents().getFirst();
        final var controlTarget = control.teams().opponents().getFirst();
        active.random().forceTarget(activeTarget);
        control.random().forceTarget(controlTarget);

        for (int turn = 1; turn <= 3; turn++) {
            activeBreaker.move(active.context(), active.log(), turn);
            controlBreaker.move(control.context(), control.log(), turn);
        }
        Assertions.assertAll(
            () -> Assertions.assertTrue(activeTarget.isAlive()),
            () -> Assertions.assertTrue(controlTarget.isAlive())
        );
        activeBreaker.move(active.context(), active.log(), 4);
        controlBreaker.move(control.context(), control.log(), 4);

        final var activeAttempts = attempts(active, activeBreaker);
        final var controlAttempts = attempts(control, controlBreaker);
        final var discharge = activeAttempts.getLast();
        final int expectedBasis = ItemProgression.valueAtLevel(420, level);
        Assertions.assertAll(
            () -> Assertions.assertTrue(discharge.discharge()),
            () -> Assertions.assertEquals(
                Map.of(AttackType.MAGICAL, expectedBasis),
                discharge.dischargeBasis()
            ),
            () -> Assertions.assertEquals(18, discharge.dischargeCoefficientNumerator()),
            () -> Assertions.assertEquals(25, discharge.dischargeCoefficientDenominator()),
            () -> Assertions.assertEquals(0, discharge.accumulationChargesAfter()),
            () -> Assertions.assertTrue(totalDamage(activeAttempts) > totalDamage(controlAttempts)),
            () -> Assertions.assertTrue(controlAttempts.stream().noneMatch(
                BattleTraceEvent.NormalAttackAttempt::discharge
            ))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void fourthAttemptDodgeConsumesAccumulationWithoutDamage(int level) {
        final var scenario = breakerDuel(level, false, true);
        final var breaker = scenario.teams().evaluatedTeam().getFirst();
        final var target = scenario.teams().opponents().getFirst();
        scenario.random().forceTarget(target);
        scenario.random().forceDodgeOnAttempt(4);

        for (int turn = 1; turn <= 4; turn++) {
            breaker.move(scenario.context(), scenario.log(), turn);
        }

        final var discharge = attempts(scenario, breaker).getLast();
        Assertions.assertAll(
            () -> Assertions.assertTrue(discharge.discharge()),
            () -> Assertions.assertTrue(discharge.dodged()),
            () -> Assertions.assertEquals(3, discharge.accumulationChargesBefore()),
            () -> Assertions.assertEquals(0, discharge.accumulationChargesAfter()),
            () -> Assertions.assertEquals(0, discharge.normalDamage()),
            () -> Assertions.assertEquals(0, discharge.dischargeDamage())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void tempoBreakRemovesExactGaugeAndControlDoesNot(int level) {
        final var active = Step12V2CausalTestSupport.scenario(V2Matchup.TACTICIAN_V2, level);
        final var control = Step12V2CausalTestSupport.scenarioWithoutSkill(
            V2Matchup.TACTICIAN_V2,
            level,
            V2Build.TACTICIAN_MAGICAL
        );
        final var activeTactician = namedEvaluated(active, V2Build.TACTICIAN_MAGICAL);
        final var controlTactician = namedEvaluated(control, V2Build.TACTICIAN_MAGICAL);
        final var activeBruiser = namedOpponent(active, V2Build.BRUISER_MAGICAL_CLOTH);
        final var controlBruiser = namedOpponent(control, V2Build.BRUISER_MAGICAL_CLOTH);
        activeBruiser.setInitiativeGaugeForTest(500);
        controlBruiser.setInitiativeGaugeForTest(500);
        active.random().forceTarget(activeBruiser);
        control.random().forceTarget(controlBruiser);

        activeTactician.move(active.context(), active.log(), 1);
        controlTactician.move(control.context(), control.log(), 1);

        final var delay = Step12V2CausalTestSupport.events(
            active.log(),
            BattleEvent.InitiativeDelayed.class
        ).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(activeBruiser.id(), delay.targetId()),
            () -> Assertions.assertEquals(activeTactician.id(), delay.sourceId()),
            () -> Assertions.assertEquals(83, delay.amount()),
            () -> Assertions.assertEquals(417, delay.gaugeAfter()),
            () -> Assertions.assertEquals(417, activeBruiser.initiativeGauge()),
            () -> Assertions.assertEquals(500, controlBruiser.initiativeGauge()),
            () -> Assertions.assertEquals(
                2,
                activeTactician.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)
            ),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                control.log(),
                BattleEvent.InitiativeDelayed.class
            ).isEmpty()),
            () -> Assertions.assertFalse(used(control, ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertEquals(
                0,
                controlTactician.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("levelsAndInvalidTempoStates")
    void tempoBreakDoesNotRemoveGaugeOrStartRecoveryForCounterState(
        int level,
        InvalidTempoState state
    ) {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.TACTICIAN_V2, level);
        final var tactician = namedEvaluated(scenario, V2Build.TACTICIAN_MAGICAL);
        final var bruiser = namedOpponent(scenario, V2Build.BRUISER_MAGICAL_CLOTH);
        switch (state) {
            case READY -> {
                bruiser.setInitiativeGaugeForTest(900);
                Assertions.assertTrue(bruiser.tick(new BattleActionLog(), 0));
            }
            case ZERO_GAUGE -> bruiser.setInitiativeGaugeForTest(0);
            case IMMUNE -> {
                bruiser.setInitiativeGaugeForTest(500);
                bruiser.scalingSkills().setTempoBreakImmune(true);
            }
        }
        final int gaugeBefore = bruiser.initiativeGauge();
        scenario.random().forceTarget(bruiser);

        tactician.move(scenario.context(), scenario.log(), 1);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                scenario.log(),
                BattleEvent.InitiativeDelayed.class
            ).isEmpty()),
            () -> Assertions.assertFalse(used(scenario, ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertEquals(0, tactician.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertEquals(gaugeBefore, bruiser.initiativeGauge())
        );
    }

    private static Step12V2CausalTestSupport.Scenario bruiserThresholdScenario(
        int level,
        boolean withoutSkill,
        int healthAboveThreshold
    ) {
        final var scenario = Step12V2CausalTestSupport.duel(
            V2Build.BRUISER_BLUNT,
            Position.FRONT,
            V2Build.GUARDIAN_BLUNT,
            Position.FRONT,
            level,
            withoutSkill ? Optional.of(V2Build.BRUISER_BLUNT) : Optional.empty()
        );
        final var bruiser = scenario.teams().evaluatedTeam().getFirst();
        final var target = scenario.teams().opponents().getFirst();
        final int threshold = Math.floorDiv(Math.multiplyExact(3, bruiser.maxHealth()), 10);
        Step12V2CausalTestSupport.setHealth(bruiser, threshold + healthAboveThreshold);
        scenario.random().forceTarget(target);
        bruiser.move(scenario.context(), scenario.log(), 1);
        return scenario;
    }

    private static Step12V2CausalTestSupport.Scenario breakerDuel(
        int level,
        boolean withoutSkill,
        boolean fourthDodge
    ) {
        final var scenario = Step12V2CausalTestSupport.duel(
            V2Build.BREAKER_MAGICAL_LEATHER,
            Position.BACK,
            V2Build.BRUISER_MAGICAL_LEATHER,
            Position.FRONT,
            level,
            withoutSkill ? Optional.of(V2Build.BREAKER_MAGICAL_LEATHER) : Optional.empty()
        );
        if (fourthDodge) {
            scenario.random().forceDodgeOnAttempt(4);
        }
        return scenario;
    }

    private static Stream<Arguments> levelsAndInvalidTempoStates() {
        return Stream.of(0, 3, 6, 10, 20).flatMap(level ->
            Stream.of(InvalidTempoState.values()).map(state -> Arguments.of(level, state))
        );
    }

    private static BattlePersonage supportAt(List<BattlePersonage> team, Position position) {
        return Step12V2CausalTestSupport.namedAll(team, V2Build.SUPPORT).stream()
            .filter(personage -> personage.startPosition() == position)
            .findFirst()
            .orElseThrow();
    }

    private static int roundHalfUp(long numerator, int denominator) {
        final long doubledDenominator = Math.multiplyExact(2L, denominator);
        final long adjustedNumerator = Math.addExact(Math.multiplyExact(2L, numerator), denominator);
        return Math.toIntExact(Math.floorDiv(adjustedNumerator, doubledDenominator));
    }

    private static BattlePersonage namedEvaluated(
        Step12V2CausalTestSupport.Scenario scenario,
        V2Build build
    ) {
        return Step12V2CausalTestSupport.named(scenario.teams().evaluatedTeam(), build);
    }

    private static BattlePersonage namedOpponent(
        Step12V2CausalTestSupport.Scenario scenario,
        V2Build build
    ) {
        return Step12V2CausalTestSupport.named(scenario.teams().opponents(), build);
    }

    private static boolean used(
        Step12V2CausalTestSupport.Scenario scenario,
        ActiveEnum skill
    ) {
        return Step12V2CausalTestSupport.events(
            scenario.log(),
            BattleEvent.SkillWindowUsed.class
        ).stream().anyMatch(event -> event.skill() == skill);
    }

    private static BattleTraceEvent.NormalAttackAttempt onlyAttempt(
        Step12V2CausalTestSupport.Scenario scenario
    ) {
        return attempts(scenario, scenario.teams().evaluatedTeam().getFirst()).getFirst();
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attempts(
        Step12V2CausalTestSupport.Scenario scenario,
        BattlePersonage personage
    ) {
        return Step12V2CausalTestSupport.traces(
            scenario.log(),
            BattleTraceEvent.NormalAttackAttempt.class
        ).stream().filter(attempt -> attempt.attackerId().equals(personage.id())).toList();
    }

    private static long totalDamage(List<BattleTraceEvent.NormalAttackAttempt> attempts) {
        return attempts.stream()
            .mapToLong(attempt -> (long) attempt.normalDamage() + attempt.dischargeDamage())
            .sum();
    }

    private enum InvalidTempoState {
        READY,
        ZERO_GAUGE,
        IMMUNE,
    }
}
