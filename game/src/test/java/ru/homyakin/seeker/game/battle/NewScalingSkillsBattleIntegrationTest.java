package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

class NewScalingSkillsBattleIntegrationTest {
    private static final int LARGE_HEALTH = 5_000;
    private static final int NORMAL_ATTACK = 100;
    private static final int DEFAULT_SPEED = 100;
    private static final int DEFAULT_THREAT = 10;

    @Test
    void guardInterceptsAndAlternatesSharedAttemptCooldown() {
        final var attacker = personage(Position.FRONT, NORMAL_ATTACK, 1);
        final var originalTarget = personage(Position.FRONT, 1, 1);
        final var guard = scalingPersonage(Position.FRONT, 1, 1, ActiveEnum.GUARD, 3);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(originalTarget, guard),
            NewScalingSkillsBattleIntegrationTest::centeredRoll
        );
        final var log = new BattleActionLog();

        for (int round = 1; round <= 7; round++) {
            attacker.move(context, log, round);
        }

        final var interceptions = events(log, BattleEvent.AttackIntercepted.class);
        Assertions.assertEquals(2, interceptions.size());
        Assertions.assertEquals(List.of(1, 7), interceptions.stream().map(BattleEvent::round).toList());
        for (final var interception : interceptions) {
            Assertions.assertEquals(attacker.id(), interception.attackerId());
            Assertions.assertEquals(originalTarget.id(), interception.originalTargetId());
            Assertions.assertEquals(guard.id(), interception.interceptorId());
        }
        Assertions.assertEquals(6, context.teamSkillState(originalTarget).guardAttemptsRemaining());
        Assertions.assertEquals(LARGE_HEALTH - 2 * NORMAL_ATTACK, guard.health());
        Assertions.assertEquals(LARGE_HEALTH - 5 * NORMAL_ATTACK, originalTarget.health());

        final var guardWindows = events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.GUARD)
            .toList();
        Assertions.assertEquals(List.of(1, 7), guardWindows.stream().map(BattleEvent::round).toList());
        Assertions.assertEquals(
            List.of(
                BattleEvent.TargetSelected.class,
                BattleEvent.AttackIntercepted.class,
                BattleEvent.SkillWindowUsed.class,
                BattleEvent.DamageReceived.class,
                BattleEvent.ThreatChanged.class
            ),
            log.events().stream()
                .filter(event -> event.round() == 1)
                .map(Object::getClass)
                .toList()
        );
    }

    @Test
    void guardOwnerCannotBeInterceptedButOwnerDirectedAttemptsAdvanceRecovery() {
        final var attacker = personage(Position.FRONT, NORMAL_ATTACK, 1);
        final var ward = personage(Position.FRONT, 1, 1);
        final var firstGuard = scalingPersonage(Position.FRONT, 1, 1, ActiveEnum.GUARD, 8);
        final var secondGuard = scalingPersonage(Position.FRONT, 1, 1, ActiveEnum.GUARD, 8);
        final var targetSelections = new AtomicInteger();
        final BattleRandom wardThenFirstGuard = (sequence, minimum, maximum) -> {
            if (sequence.startsWith("target-selection:")) {
                return targetSelections.incrementAndGet() == 1 ? minimum : minimum + DEFAULT_THREAT;
            }
            return centeredRoll(sequence, minimum, maximum);
        };
        final var context = new BattleContext(
            List.of(attacker),
            List.of(ward, firstGuard, secondGuard),
            wardThenFirstGuard
        );
        final var log = new BattleActionLog();

        for (int round = 1; round <= 5; round++) {
            attacker.move(context, log, round);
        }

        final var interceptions = events(log, BattleEvent.AttackIntercepted.class);
        Assertions.assertEquals(1, interceptions.size());
        Assertions.assertEquals(ward.id(), interceptions.getFirst().originalTargetId());
        Assertions.assertEquals(1, interceptions.getFirst().round());
        Assertions.assertEquals(0, context.teamSkillState(firstGuard).guardAttemptsRemaining());

        final var ownerSelections = events(log, BattleEvent.TargetSelected.class).stream()
            .filter(event -> event.round() >= 2)
            .toList();
        Assertions.assertEquals(4, ownerSelections.size());
        Assertions.assertTrue(ownerSelections.stream().allMatch(event ->
            event.originalTargetId().equals(firstGuard.id())
                && event.finalTargetId().equals(firstGuard.id())
        ));
        final var guardWindows = events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.GUARD)
            .toList();
        Assertions.assertEquals(1, guardWindows.size());
        Assertions.assertEquals(LARGE_HEALTH, ward.health());
    }

    @Test
    void penetrationReachesAnyLivingTargetAndAlternatesPersonalCooldown() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            NORMAL_ATTACK,
            1,
            ActiveEnum.PENETRATION,
            3
        );
        final var farTarget = personage(Position.BACK, 1, 3);
        final var frontTarget = personage(Position.FRONT, 1, 1);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(farTarget, frontTarget),
            NewScalingSkillsBattleIntegrationTest::centeredRoll
        );
        final var log = new BattleActionLog();

        for (int round = 1; round <= 7; round++) {
            attacker.move(context, log, round);
        }

        final var penetrationWindows = events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.PENETRATION)
            .toList();
        Assertions.assertEquals(List.of(1, 7), penetrationWindows.stream().map(BattleEvent::round).toList());
        Assertions.assertEquals(6, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION));
        Assertions.assertEquals(LARGE_HEALTH - 2 * NORMAL_ATTACK, farTarget.health());
        Assertions.assertEquals(LARGE_HEALTH - 5 * NORMAL_ATTACK, frontTarget.health());

        final var farSelections = events(log, BattleEvent.TargetSelected.class).stream()
            .filter(event -> event.finalTargetId().equals(farTarget.id()))
            .toList();
        Assertions.assertEquals(List.of(1, 7), farSelections.stream().map(BattleEvent::round).toList());
        Assertions.assertTrue(farSelections.stream().allMatch(event -> event.distance() == 3));

        final var firstHit = events(log, BattleEvent.DamageReceived.class).stream()
            .filter(event -> event.round() == 1)
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals(farTarget.id(), firstHit.targetId());
        Assertions.assertEquals(Map.of(AttackType.SLASH, NORMAL_ATTACK), firstHit.roll().attack());
    }

    @Test
    void accumulationDischargesExactPrimaryAttackFormulaOnFourthAttempt() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            NORMAL_ATTACK,
            1,
            ActiveEnum.ACCUMULATION,
            5
        );
        final var target = personage(Position.FRONT, 1, 1);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(target),
            NewScalingSkillsBattleIntegrationTest::centeredRoll
        );
        final var log = new BattleActionLog();

        for (int round = 1; round <= 4; round++) {
            attacker.move(context, log, round);
        }

        final var chargeEvents = events(log, BattleEvent.SkillChargeChanged.class);
        Assertions.assertEquals(List.of(1, 2, 3, 0), chargeEvents.stream()
            .map(BattleEvent.SkillChargeChanged::charges)
            .toList());
        Assertions.assertEquals(List.of(false, false, false, true), chargeEvents.stream()
            .map(BattleEvent.SkillChargeChanged::discharged)
            .toList());

        final var discharge = Assertions.assertInstanceOf(
            BattleEvent.ScalingSkillDamage.class,
            events(log, BattleEvent.ScalingSkillDamage.class).getFirst()
        );
        Assertions.assertEquals(ActiveEnum.ACCUMULATION, discharge.skill());
        Assertions.assertEquals(Map.of(AttackType.SLASH, NORMAL_ATTACK), discharge.basis());
        Assertions.assertEquals(21, discharge.coefficientNumerator());
        Assertions.assertEquals(25, discharge.coefficientDenominator());
        Assertions.assertEquals(84, discharge.damageTaken());
        Assertions.assertFalse(discharge.periodic());
        Assertions.assertEquals(4, discharge.round());
        Assertions.assertEquals(LARGE_HEALTH - 4 * NORMAL_ATTACK - 84, target.health());
        Assertions.assertEquals(0, attacker.scalingSkills().accumulationCharges());
    }

    @Test
    void accumulationConsumesPreparedDischargeWhenFourthAttemptIsDodged() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            NORMAL_ATTACK,
            1,
            ActiveEnum.ACCUMULATION,
            5
        );
        final var target = personage(Position.FRONT, 1, 1, 50, 0);
        final var dodgeRolls = new AtomicInteger();
        final BattleRandom fourthAttemptDodge = (sequence, minimum, maximum) -> {
            if (sequence.startsWith("normal-dodge:")) {
                return dodgeRolls.incrementAndGet() == 4 ? minimum : maximum;
            }
            return centeredRoll(sequence, minimum, maximum);
        };
        final var context = new BattleContext(List.of(attacker), List.of(target), fourthAttemptDodge);
        final var log = new BattleActionLog();

        for (int round = 1; round <= 4; round++) {
            attacker.move(context, log, round);
        }

        Assertions.assertEquals(3, events(log, BattleEvent.DamageReceived.class).size());
        final var dodge = events(log, BattleEvent.AttackDodged.class).getFirst();
        Assertions.assertEquals(4, dodge.round());
        Assertions.assertEquals(target.id(), dodge.targetId());
        Assertions.assertTrue(events(log, BattleEvent.ScalingSkillDamage.class).isEmpty());
        final var reset = events(log, BattleEvent.SkillChargeChanged.class).getLast();
        Assertions.assertEquals(0, reset.charges());
        Assertions.assertTrue(reset.discharged());
        Assertions.assertEquals(0, attacker.scalingSkills().accumulationCharges());
    }

    @Test
    void accumulationKeepsEquipmentPrimaryTypeAfterLegacyBerserkRounding() {
        final var attacker = BattlePersonage.withSkillVersions(
            List.of(item(
                List.of(
                    new ItemAttack(AttackType.SLASH, 1, 1, 1),
                    new ItemAttack(AttackType.SLASH, 1, 1, 1),
                    new ItemAttack(AttackType.BLUNT, 1, 1, 2)
                ),
                0,
                0
            )),
            Position.FRONT,
            Map.of(ActiveEnum.BERSERK, 8, ActiveEnum.ACCUMULATION, 5),
            Map.of(
                ActiveEnum.BERSERK, SkillFormulaVersion.LEGACY_SKILLS_V1,
                ActiveEnum.ACCUMULATION, SkillFormulaVersion.SCALING_SKILLS_V1
            )
        );
        final var target = personage(Position.FRONT, 1, 1);
        Assertions.assertEquals(
            4_000,
            attacker.applyVersionedSkillDamage(4_000, 4_000, target)
        );
        final var context = new BattleContext(
            List.of(attacker),
            List.of(target),
            NewScalingSkillsBattleIntegrationTest::centeredRoll
        );
        final var log = new BattleActionLog();

        for (int round = 1; round <= 4; round++) {
            attacker.move(context, log, round);
        }

        final var berserk = events(log, BattleEvent.PersonageAttackBuffed.class).getFirst();
        Assertions.assertEquals(ActiveEnum.BERSERK, berserk.skill());
        Assertions.assertEquals(30, berserk.attackBonusPercent());
        Assertions.assertTrue(events(log, BattleEvent.DamageReceived.class).stream().allMatch(event ->
            event.roll().attack().equals(Map.of(AttackType.SLASH, 3, AttackType.BLUNT, 3))
        ));
        final var discharge = events(log, BattleEvent.ScalingSkillDamage.class).getFirst();
        Assertions.assertEquals(ActiveEnum.ACCUMULATION, discharge.skill());
        Assertions.assertEquals(Map.of(AttackType.SLASH, 3), discharge.basis());
        Assertions.assertEquals(21, discharge.coefficientNumerator());
        Assertions.assertEquals(25, discharge.coefficientDenominator());
    }

    @Test
    void tempoBreakUsesImpactFormulaCooldownAndTargetActionImmunity() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            NORMAL_ATTACK,
            1,
            0,
            20,
            ActiveEnum.TEMPO_BREAK,
            5
        );
        final var target = personage(Position.FRONT, 1, 1);
        target.setInitiativeGaugeForTest(300);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(target),
            NewScalingSkillsBattleIntegrationTest::centeredRoll
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        Assertions.assertEquals(195, target.initiativeGauge());
        Assertions.assertEquals(2, attacker.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
        Assertions.assertTrue(target.scalingSkills().tempoBreakImmune());

        attacker.move(context, log, 2);
        Assertions.assertEquals(1, attacker.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
        Assertions.assertEquals(1, events(log, BattleEvent.InitiativeDelayed.class).size());

        target.move(context, log, 2);
        Assertions.assertFalse(target.scalingSkills().tempoBreakImmune());
        attacker.move(context, log, 3);
        Assertions.assertEquals(0, attacker.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
        Assertions.assertEquals(1, events(log, BattleEvent.InitiativeDelayed.class).size());

        attacker.move(context, log, 4);

        final var delays = events(log, BattleEvent.InitiativeDelayed.class);
        Assertions.assertEquals(List.of(1, 4), delays.stream().map(BattleEvent::round).toList());
        Assertions.assertEquals(List.of(105, 105), delays.stream()
            .map(BattleEvent.InitiativeDelayed::amount)
            .toList());
        Assertions.assertEquals(List.of(195, 90), delays.stream()
            .map(BattleEvent.InitiativeDelayed::gaugeAfter)
            .toList());
        Assertions.assertTrue(delays.stream().allMatch(event -> event.skill() == ActiveEnum.TEMPO_BREAK));
        Assertions.assertEquals(2, events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.TEMPO_BREAK)
            .count());
        Assertions.assertEquals(2, attacker.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK));
        Assertions.assertTrue(target.scalingSkills().tempoBreakImmune());
    }

    private static BattlePersonage personage(Position position, int attack, int maxRange) {
        return personage(position, attack, maxRange, 0, 0);
    }

    private static BattlePersonage personage(
        Position position,
        int attack,
        int maxRange,
        int dodgeChance,
        int impact
    ) {
        return new BattlePersonage(
            List.of(item(attack, maxRange, dodgeChance, impact)),
            position
        );
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        int attack,
        int maxRange,
        ActiveEnum skill,
        int points
    ) {
        return scalingPersonage(position, attack, maxRange, 0, 0, skill, points);
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        int attack,
        int maxRange,
        int dodgeChance,
        int impact,
        ActiveEnum skill,
        int points
    ) {
        return BattlePersonage.forScalingSkills(
            List.of(item(attack, maxRange, dodgeChance, impact)),
            position,
            Map.of(skill, points)
        );
    }

    private static Item item(int attack, int maxRange, int dodgeChance, int impact) {
        return item(
            List.of(new ItemAttack(AttackType.SLASH, 1, maxRange, attack)),
            dodgeChance,
            impact
        );
    }

    private static Item item(List<ItemAttack> attacks, int dodgeChance, int impact) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                attacks,
                Optional.empty(),
                LARGE_HEALTH,
                0,
                dodgeChance,
                0,
                DEFAULT_SPEED,
                DEFAULT_THREAT,
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

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }
}
