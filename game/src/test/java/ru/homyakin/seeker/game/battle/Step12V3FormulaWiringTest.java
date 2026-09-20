package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

/** Exact revision-three formula wiring at every preregistered control level. */
class Step12V3FormulaWiringTest {
    private static final List<Integer> LEVELS = List.of(0, 3, 6, 10, 20);

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void rangerUsesCurrentFarBasisAndSecondScalingFormulaVersion(int level) {
        final var scenario = scenario(
            V3Matchup.RANGER_V3,
            V3Build.RANGER_MAGICAL,
            V3Build.BRUISER_MAGICAL_CLOTH,
            level
        );

        scenario.source().move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario);
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, attempt.distance()),
            () -> Assertions.assertEquals(
                Map.of(AttackType.MAGICAL, ItemProgression.valueAtLevel(360, level)),
                attempt.savedBasis()
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                scenario.source().scalingSkills().version(ActiveEnum.DOUBLE_ATTACK)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void rangerEmitsCurrentDoubleAttackDamageAtEveryLevel(int level) {
        final var scenario = scenario(
            V3Matchup.RANGER_V3,
            V3Build.RANGER_MAGICAL,
            V3Build.BRUISER_MAGICAL_CLOTH,
            level
        );

        scenario.source().move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario);
        final var doubleAttack = events(scenario.log(), BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == ActiveEnum.DOUBLE_ATTACK)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(scenario.source().id(), doubleAttack.sourceId()),
            () -> Assertions.assertEquals(scenario.target().id(), doubleAttack.targetId()),
            () -> Assertions.assertEquals(attempt.savedBasis(), doubleAttack.basis()),
            () -> Assertions.assertEquals(18, doubleAttack.coefficientNumerator()),
            () -> Assertions.assertEquals(100, doubleAttack.coefficientDenominator())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void skirmisherUsesCurrentHitAndRunCooldownAndActuallyRetreats(int level) {
        final var scenario = scenario(
            V3Matchup.SKIRMISHER_V3,
            V3Build.SKIRMISHER_SLASH,
            V3Build.BREAKER_CLOSE_SLASH,
            level
        );

        scenario.source().move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario);
        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, attempt.access()),
            () -> Assertions.assertEquals(
                attempt.lineBeforeRetreat() - scenario.source().advanceDirection().indexDelta(),
                attempt.lineAfterRetreat()
            ),
            () -> Assertions.assertNotEquals(attempt.lineBeforeRetreat(), attempt.lineAfterRetreat()),
            () -> Assertions.assertEquals(
                attempt.lineAfterRetreat(),
                scenario.source().currentPosition()
            ),
            () -> Assertions.assertEquals(
                8,
                scenario.source().scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                scenario.source().scalingSkills().version(ActiveEnum.HIT_AND_RUN)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void breakerUsesCurrentAccumulationBasisAndDenominator(int level) {
        final var scenario = scenario(
            V3Matchup.BREAKER_V3,
            V3Build.BREAKER_MAGICAL_LEATHER,
            V3Build.BRUISER_MAGICAL_LEATHER,
            level
        );

        for (int turn = 1; turn <= 4; turn++) {
            scenario.source().move(scenario.context(), scenario.log(), turn);
        }

        final var discharge = attempts(scenario).getLast();
        Assertions.assertAll(
            () -> Assertions.assertTrue(discharge.discharge()),
            () -> Assertions.assertEquals(
                Map.of(AttackType.MAGICAL, ItemProgression.valueAtLevel(360, level)),
                discharge.dischargeBasis()
            ),
            () -> Assertions.assertEquals(18, discharge.dischargeCoefficientNumerator()),
            () -> Assertions.assertEquals(28, discharge.dischargeCoefficientDenominator()),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                scenario.source().scalingSkills().version(ActiveEnum.ACCUMULATION)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void tacticianUsesCurrentTempoFormulaAndCooldown(int level) {
        final var scenario = scenario(
            V3Matchup.TACTICIAN_V3,
            V3Build.TACTICIAN_MAGICAL,
            V3Build.BRUISER_MAGICAL_CLOTH,
            level
        );
        scenario.target().setInitiativeGaugeForTest(500);

        scenario.source().move(scenario.context(), scenario.log(), 1);

        final var delay = events(scenario.log(), BattleEvent.InitiativeDelayed.class).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(331, delay.amount()),
            () -> Assertions.assertEquals(169, delay.gaugeAfter()),
            () -> Assertions.assertEquals(
                1,
                scenario.source().scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                scenario.source().scalingSkills().version(ActiveEnum.TEMPO_BREAK)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 10, 20})
    void assassinUsesCurrentPenetrationPackageAndTargetLock(int level) {
        final var scenario = scenario(
            V3Matchup.ASSASSIN_V3,
            V3Build.ASSASSIN_PIERCE,
            V3Build.RANGER_PIERCE,
            level
        );

        scenario.source().move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario);
        final var penetration = events(scenario.log(), BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == ActiveEnum.PENETRATION)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access()),
            () -> Assertions.assertEquals(attempt.savedBasis(), penetration.basis()),
            () -> Assertions.assertEquals(18, penetration.coefficientNumerator()),
            () -> Assertions.assertEquals(12, penetration.coefficientDenominator()),
            () -> Assertions.assertEquals(
                java.util.Optional.of(scenario.target().id()),
                scenario.source().scalingSkills().penetrationTargetId()
            ),
            () -> Assertions.assertEquals(2, scenario.source().scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                scenario.source().scalingSkills().version(ActiveEnum.PENETRATION)
            )
        );
    }

    @org.junit.jupiter.api.Test
    void declaredLevelsStayAlignedWithTheSharedStep12Fixture() {
        Assertions.assertEquals(LEVELS, Step12SimulationFixtures.CONTROL_LEVELS);
    }

    private static Scenario scenario(
        V3Matchup matchup,
        V3Build sourceBuild,
        V3Build targetBuild,
        int level
    ) {
        final var teams = Step12SimulationFixtures.v3Teams(matchup, level);
        final var source = named(teams.evaluatedTeam(), sourceBuild);
        final var target = named(teams.opponents(), targetBuild);
        final var random = new ForcedTargetRandom(target.id());
        final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
        return new Scenario(source, target, context, new BattleActionLog());
    }

    private static BattlePersonage named(List<BattlePersonage> team, V3Build build) {
        return team.stream()
            .filter(personage -> personage.name().orElseThrow().equals(build.displayName()))
            .findFirst()
            .orElseThrow();
    }

    private static BattleTraceEvent.NormalAttackAttempt onlyAttempt(Scenario scenario) {
        return attempts(scenario).getFirst();
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attempts(Scenario scenario) {
        return scenario.log().traceEvents().stream()
            .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
            .filter(attempt -> attempt.attackerId().equals(scenario.source().id()))
            .toList();
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    private record Scenario(
        BattlePersonage source,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log
    ) {
    }

    private static final class ForcedTargetRandom implements BattleRandom {
        private final UUID targetId;

        private ForcedTargetRandom(UUID targetId) {
            this.targetId = targetId;
        }

        @Override
        public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
            if (sequence.startsWith("normal-critical:") || sequence.startsWith("normal-dodge:")) {
                return maximumInclusive;
            }
            if (sequence.startsWith("skill-chance:")) {
                return minimumInclusive;
            }
            return minimumInclusive + (maximumInclusive - minimumInclusive) / 2;
        }

        @Override
        public <T> T pickWeighted(String sequence, Map<T, Integer> weights) {
            for (final var candidate : weights.keySet()) {
                if (candidate instanceof BattlePersonage personage && personage.id().equals(targetId)) {
                    return candidate;
                }
            }
            return weights.keySet().iterator().next();
        }

        @Override
        public <T> List<T> shuffle(String sequence, List<T> values) {
            return List.copyOf(values);
        }
    }
}
