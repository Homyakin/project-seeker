package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class ScalingSkillFormulaVersionBattleTest {
    private static final int ATTACK = 100;
    private static final int HEALTH = 10_000;

    @Test
    void v1AndV2DoubleAttackRunTogetherWithTheirOwnDenominators() {
        final var v1 = scalingPersonage(
            Position.FRONT,
            ActiveEnum.DOUBLE_ATTACK,
            3,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );
        final var v2 = scalingPersonage(
            Position.FRONT,
            ActiveEnum.DOUBLE_ATTACK,
            3,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
        final var target = ordinaryPersonage(Position.FRONT);
        final var context = new BattleContext(List.of(v1, v2), List.of(target),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        final var log = new BattleActionLog();

        v1.move(context, log, 1);
        v2.move(context, log, 2);

        final var damage = skillDamage(log, ActiveEnum.DOUBLE_ATTACK);
        Assertions.assertAll(
            () -> Assertions.assertEquals(2, damage.size()),
            () -> Assertions.assertEquals(v1.id(), damage.getFirst().sourceId()),
            () -> Assertions.assertEquals(15, damage.getFirst().coefficientNumerator()),
            () -> Assertions.assertEquals(50, damage.getFirst().coefficientDenominator()),
            () -> Assertions.assertEquals(v2.id(), damage.getLast().sourceId()),
            () -> Assertions.assertEquals(15, damage.getLast().coefficientNumerator()),
            () -> Assertions.assertEquals(100, damage.getLast().coefficientDenominator()),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                v1.scalingSkills().version(ActiveEnum.DOUBLE_ATTACK)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                v2.scalingSkills().version(ActiveEnum.DOUBLE_ATTACK)
            )
        );
    }

    @Test
    void accumulationTraceKeepsVersionedDenominator() {
        final var v1 = scalingPersonage(
            Position.FRONT,
            ActiveEnum.ACCUMULATION,
            4,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );
        final var v2 = scalingPersonage(
            Position.FRONT,
            ActiveEnum.ACCUMULATION,
            4,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
        final var target = ordinaryPersonage(Position.FRONT);
        final var context = new BattleContext(List.of(v1, v2), List.of(target),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        final var log = new BattleActionLog();

        for (int turn = 1; turn <= 4; turn++) {
            v1.move(context, log, turn);
            v2.move(context, log, turn);
        }

        final var v1Discharge = attemptsBy(log, v1).getLast();
        final var v2Discharge = attemptsBy(log, v2).getLast();
        Assertions.assertAll(
            () -> Assertions.assertTrue(v1Discharge.discharge()),
            () -> Assertions.assertEquals(18, v1Discharge.dischargeCoefficientNumerator()),
            () -> Assertions.assertEquals(25, v1Discharge.dischargeCoefficientDenominator()),
            () -> Assertions.assertTrue(v2Discharge.discharge()),
            () -> Assertions.assertEquals(18, v2Discharge.dischargeCoefficientNumerator()),
            () -> Assertions.assertEquals(28, v2Discharge.dischargeCoefficientDenominator())
        );
    }

    @Test
    void tempoBreakKeepsV1AndV2StrengthAndCooldown() {
        final var v1 = tempoScenario(SkillFormulaVersion.SCALING_SKILLS_V1);
        final var v2 = tempoScenario(SkillFormulaVersion.SCALING_SKILLS_V2);

        v1.source().move(v1.context(), v1.log(), 1);
        v2.source().move(v2.context(), v2.log(), 1);

        final var v1Delay = events(v1.log(), BattleEvent.InitiativeDelayed.class).getFirst();
        final var v2Delay = events(v2.log(), BattleEvent.InitiativeDelayed.class).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(105, v1Delay.amount()),
            () -> Assertions.assertEquals(795, v1Delay.gaugeAfter()),
            () -> Assertions.assertEquals(2, v1.source().scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertEquals(420, v2Delay.amount()),
            () -> Assertions.assertEquals(480, v2Delay.gaugeAfter()),
            () -> Assertions.assertEquals(1, v2.source().scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK))
        );
    }

    @Test
    void v1PenetrationKeepsOldSelectionConsumptionAndNoDamagePackageOrLock() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            ActiveEnum.PENETRATION,
            3,
            SkillFormulaVersion.SCALING_SKILLS_V1,
            0,
            1
        );
        final var close = ordinaryPersonage(Position.FRONT);
        final var far = ordinaryPersonage(Position.BACK);
        final var context = new BattleContext(List.of(attacker), List.of(close, far),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var attempt = attemptsBy(log, attacker).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(close.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access()),
            () -> Assertions.assertEquals(5, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION)),
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertTrue(skillDamage(log, ActiveEnum.PENETRATION).isEmpty()),
            () -> Assertions.assertEquals(HEALTH - ATTACK, close.health()),
            () -> Assertions.assertEquals(HEALTH, far.health())
        );
    }

    @Test
    void v1PenetrationPreservesEnemyInsertionOrderAcrossFarAndCloseTargets() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            ActiveEnum.PENETRATION,
            3,
            SkillFormulaVersion.SCALING_SKILLS_V1,
            0,
            1
        );
        final var far = ordinaryPersonage(Position.BACK);
        final var close = ordinaryPersonage(Position.FRONT);
        final var context = new BattleContext(List.of(attacker), List.of(far, close),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var attempt = attemptsBy(log, attacker).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(far.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(3, attempt.distance()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access())
        );
    }

    @Test
    void v2PenetrationTakesOrdinaryRangePlusOneBeforeHitAndRun() {
        final var attacker = BattlePersonage.withSkillVersions(
            List.of(item(0, 2)),
            Position.FRONT,
            Map.of(ActiveEnum.PENETRATION, 3, ActiveEnum.HIT_AND_RUN, 3),
            Map.of(
                ActiveEnum.PENETRATION, SkillFormulaVersion.SCALING_SKILLS_V2,
                ActiveEnum.HIT_AND_RUN, SkillFormulaVersion.SCALING_SKILLS_V2
            )
        );
        final var closeAnchor = ordinaryPersonage(Position.FRONT);
        final var rangePlusOneTarget = ordinaryPersonage(Position.BACK);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(closeAnchor, rangePlusOneTarget),
            ScalingSkillFormulaVersionBattleTest::centeredRoll
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var attempt = attemptsBy(log, attacker).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, attempt.distance()),
            () -> Assertions.assertEquals(2, attempt.ordinaryRange()),
            () -> Assertions.assertEquals(rangePlusOneTarget.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access()),
            () -> Assertions.assertEquals(5, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN)),
            () -> Assertions.assertEquals(1, skillDamage(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertEquals(0, skillWindows(log, ActiveEnum.HIT_AND_RUN).size()),
            () -> Assertions.assertEquals(
                Optional.of(rangePlusOneTarget.id()),
                attacker.scalingSkills().penetrationTargetId()
            )
        );
    }

    @Test
    void guardGeometryIsVersioned() {
        final var v1 = guardScenario(SkillFormulaVersion.SCALING_SKILLS_V1);
        final var v2 = guardScenario(SkillFormulaVersion.SCALING_SKILLS_V2);

        v1.attacker().move(v1.context(), v1.log(), 1);
        v2.attacker().move(v2.context(), v2.log(), 1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                v1.guard().id(),
                events(v1.log(), BattleEvent.TargetSelected.class).getFirst().finalTargetId()
            ),
            () -> Assertions.assertEquals(1, events(v1.log(), BattleEvent.AttackIntercepted.class).size()),
            () -> Assertions.assertEquals(
                v2.ward().id(),
                events(v2.log(), BattleEvent.TargetSelected.class).getFirst().finalTargetId()
            ),
            () -> Assertions.assertTrue(events(v2.log(), BattleEvent.AttackIntercepted.class).isEmpty())
        );
    }

    private static TempoScenario tempoScenario(SkillFormulaVersion version) {
        final var source = scalingPersonage(Position.FRONT, ActiveEnum.TEMPO_BREAK, 5, version, 20);
        final var target = ordinaryPersonage(Position.FRONT);
        target.setInitiativeGaugeForTest(900);
        final var context = new BattleContext(List.of(source), List.of(target),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        return new TempoScenario(source, context, new BattleActionLog());
    }

    private static GuardScenario guardScenario(SkillFormulaVersion version) {
        final var attacker = ordinaryPersonage(Position.MID);
        final var ward = ordinaryPersonage(Position.MID);
        final var guard = scalingPersonage(Position.MID, ActiveEnum.GUARD, 8, version);
        final var context = new BattleContext(List.of(attacker), List.of(ward, guard),
            ScalingSkillFormulaVersionBattleTest::centeredRoll);
        return new GuardScenario(attacker, ward, guard, context, new BattleActionLog());
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        ActiveEnum skill,
        int points,
        SkillFormulaVersion version
    ) {
        return scalingPersonage(position, skill, points, version, 0);
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        ActiveEnum skill,
        int points,
        SkillFormulaVersion version,
        int impact
    ) {
        return scalingPersonage(position, skill, points, version, impact, 3);
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        ActiveEnum skill,
        int points,
        SkillFormulaVersion version,
        int impact,
        int maxRange
    ) {
        return BattlePersonage.forScalingSkills(
            List.of(item(impact, maxRange)),
            position,
            Map.of(skill, points),
            version
        );
    }

    private static BattlePersonage ordinaryPersonage(Position position) {
        return new BattlePersonage(List.of(item(0)), position);
    }

    private static Item item(int impact) {
        return item(impact, 3);
    }

    private static Item item(int impact, int maxRange) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                List.of(new ItemAttack(AttackType.SLASH, 1, maxRange, ATTACK)),
                Optional.empty(),
                HEALTH,
                0,
                0,
                0,
                100,
                10,
                impact,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static int centeredRoll(String sequence, int minimum, int maximum) {
        if (sequence.startsWith("target-selection:")) {
            return minimum;
        }
        return minimum + (maximum - minimum) / 2;
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attemptsBy(
        BattleActionLog log,
        BattlePersonage source
    ) {
        return log.traceEvents().stream()
            .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
            .filter(attempt -> attempt.attackerId().equals(source.id()))
            .toList();
    }

    private static List<BattleEvent.ScalingSkillDamage> skillDamage(
        BattleActionLog log,
        ActiveEnum skill
    ) {
        return events(log, BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == skill)
            .toList();
    }

    private static List<BattleEvent.SkillWindowUsed> skillWindows(
        BattleActionLog log,
        ActiveEnum skill
    ) {
        return events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == skill)
            .toList();
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    private record TempoScenario(
        BattlePersonage source,
        BattleContext context,
        BattleActionLog log
    ) {
    }

    private record GuardScenario(
        BattlePersonage attacker,
        BattlePersonage ward,
        BattlePersonage guard,
        BattleContext context,
        BattleActionLog log
    ) {
    }
}
