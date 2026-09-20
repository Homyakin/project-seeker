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

class PenetrationTargetLockBattleTest {
    private static final int ATTACK = 100;
    private static final int LARGE_HEALTH = 5_000;
    private static final int DEFAULT_SPEED = 100;
    private static final int DEFAULT_THREAT = 10;

    @Test
    void openingLocksActualTargetForExactlyTwoFollowUpsWithoutRepeatingWindowOrBonus() {
        final var attacker = penetrationAttacker(3);
        final var farTarget = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(farTarget, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        Assertions.assertAll(
            () -> Assertions.assertEquals(Optional.of(farTarget.id()),
                attacker.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(2, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(5, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION))
        );

        attacker.move(context, log, 2);
        Assertions.assertAll(
            () -> Assertions.assertEquals(Optional.of(farTarget.id()),
                attacker.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(1, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(4, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION))
        );

        attacker.move(context, log, 3);

        Assertions.assertAll(
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(3, attacker.scalingSkills().cooldown(ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(
                List.of(farTarget.id(), farTarget.id(), farTarget.id()),
                events(log, BattleEvent.TargetSelected.class).stream()
                    .map(BattleEvent.TargetSelected::originalTargetId)
                    .toList()
            ),
            () -> Assertions.assertEquals(
                List.of(AttackAccess.PENETRATION, AttackAccess.PENETRATION, AttackAccess.PENETRATION),
                attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList()
            ),
            () -> Assertions.assertEquals(List.of(1), skillWindows(log, ActiveEnum.PENETRATION).stream()
                .map(BattleEvent::round)
                .toList()),
            () -> Assertions.assertEquals(List.of(1), skillDamage(log, ActiveEnum.PENETRATION).stream()
                .map(BattleEvent::round)
                .toList()),
            () -> Assertions.assertEquals(LARGE_HEALTH - 425, farTarget.health()),
            () -> Assertions.assertEquals(LARGE_HEALTH, closeTarget.health())
        );
        final var openingBonus = skillDamage(log, ActiveEnum.PENETRATION).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(15, openingBonus.coefficientNumerator()),
            () -> Assertions.assertEquals(12, openingBonus.coefficientDenominator()),
            () -> Assertions.assertEquals(125, openingBonus.damageTaken())
        );
    }

    @Test
    void missedOpeningDoesNotLockTarget() {
        final var attacker = penetrationAttacker(8);
        final var farTarget = target(Position.BACK, LARGE_HEALTH, 3, 100);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(farTarget, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        attacker.move(context, log, 2);

        Assertions.assertAll(
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(
                List.of(farTarget.id(), closeTarget.id()),
                events(log, BattleEvent.TargetSelected.class).stream()
                    .map(BattleEvent.TargetSelected::originalTargetId)
                    .toList()
            ),
            () -> Assertions.assertEquals(
                List.of(AttackAccess.PENETRATION, AttackAccess.NORMAL),
                attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList()
            ),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertTrue(skillDamage(log, ActiveEnum.PENETRATION).isEmpty())
        );
    }

    @Test
    void targetKilledByOpeningDoesNotBecomeLocked() {
        final var attacker = penetrationAttacker(8);
        final var farTarget = target(Position.BACK, ATTACK, 3, 0);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(farTarget, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        attacker.move(context, log, 2);

        Assertions.assertAll(
            () -> Assertions.assertFalse(farTarget.isAlive()),
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(
                List.of(farTarget.id(), closeTarget.id()),
                events(log, BattleEvent.TargetSelected.class).stream()
                    .map(BattleEvent.TargetSelected::originalTargetId)
                    .toList()
            ),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertTrue(skillDamage(log, ActiveEnum.PENETRATION).isEmpty())
        );
    }

    @Test
    void lockedTargetDeathClearsUnusedFollowUp() {
        final var attacker = penetrationAttacker(8);
        final var farTarget = target(Position.BACK, 400, 3, 0);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(farTarget, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        attacker.move(context, log, 2);
        attacker.move(context, log, 3);

        Assertions.assertAll(
            () -> Assertions.assertFalse(farTarget.isAlive()),
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(
                List.of(farTarget.id(), farTarget.id(), closeTarget.id()),
                events(log, BattleEvent.TargetSelected.class).stream()
                    .map(BattleEvent.TargetSelected::originalTargetId)
                    .toList()
            ),
            () -> Assertions.assertEquals(
                List.of(AttackAccess.PENETRATION, AttackAccess.PENETRATION, AttackAccess.NORMAL),
                attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList()
            ),
            () -> Assertions.assertEquals(1, skillDamage(log, ActiveEnum.PENETRATION).size())
        );
    }

    @Test
    void missingLockedTargetClearsFollowUpsBeforeOrdinarySelection() {
        final var attacker = penetrationAttacker(8);
        final var farTarget = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(farTarget, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        context.enemyAliveTeam(attacker).remove(farTarget.id());
        attacker.move(context, log, 2);

        Assertions.assertAll(
            () -> Assertions.assertTrue(farTarget.isAlive()),
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(
                List.of(farTarget.id(), closeTarget.id()),
                events(log, BattleEvent.TargetSelected.class).stream()
                    .map(BattleEvent.TargetSelected::originalTargetId)
                    .toList()
            ),
            () -> Assertions.assertEquals(
                List.of(AttackAccess.PENETRATION, AttackAccess.NORMAL),
                attempts(log).stream().map(BattleTraceEvent.NormalAttackAttempt::access).toList()
            )
        );
    }

    @Test
    void anotherCombatantDefeatingLockedTargetClearsFollowUpsBeforeOwnersNextTurn() {
        final var attacker = penetrationAttacker(8);
        final var ally = new BattlePersonage(
            List.of(item(10_000, 3, LARGE_HEALTH, 0)),
            Position.FRONT
        );
        final var farTarget = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = new BattleContext(
            List.of(attacker, ally),
            List.of(farTarget, closeTarget),
            PenetrationTargetLockBattleTest::deterministicRoll
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        Assertions.assertEquals(
            Optional.of(farTarget.id()),
            attacker.scalingSkills().penetrationTargetId()
        );

        ally.move(context, log, 2);
        context.pruneDefeated();

        Assertions.assertAll(
            () -> Assertions.assertFalse(farTarget.isAlive()),
            () -> Assertions.assertFalse(context.enemyAliveTeam(attacker).containsKey(farTarget.id())),
            () -> Assertions.assertTrue(attacker.scalingSkills().penetrationTargetId().isEmpty()),
            () -> Assertions.assertEquals(0, attacker.scalingSkills().penetrationFollowUpsRemaining())
        );
    }

    @Test
    void distantGuardCannotInterceptLockedContinuationWithoutOrdinaryAccess() {
        final var attacker = penetrationAndHitAndRunAttacker();
        final var ward = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var guard = guard(Position.BACK, LARGE_HEALTH, 3);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(ward, guard, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        context.moveTowardEnemy(guard);
        attacker.move(context, log, 2);

        final var selections = events(log, BattleEvent.TargetSelected.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ward.id(), selections.getFirst().originalTargetId()),
            () -> Assertions.assertEquals(ward.id(), selections.getFirst().finalTargetId()),
            () -> Assertions.assertEquals(ward.id(), selections.getLast().originalTargetId()),
            () -> Assertions.assertEquals(ward.id(), selections.getLast().finalTargetId()),
            () -> Assertions.assertTrue(events(log, BattleEvent.AttackIntercepted.class).isEmpty()),
            () -> Assertions.assertTrue(skillWindows(log, ActiveEnum.GUARD).isEmpty()),
            () -> Assertions.assertEquals(Optional.of(ward.id()),
                attacker.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(1, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(1, skillDamage(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertTrue(skillWindows(log, ActiveEnum.HIT_AND_RUN).isEmpty())
        );
    }

    @Test
    void ordinaryAccessibleGuardCanInterceptLockedContinuation() {
        final var attacker = penetrationAttacker(8, 2);
        final var ward = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var guard = guard(Position.BACK, LARGE_HEALTH, 3);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(ward, guard, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        context.moveTowardEnemy(guard);
        attacker.move(context, log, 2);

        final var selections = events(log, BattleEvent.TargetSelected.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ward.id(), selections.getFirst().originalTargetId()),
            () -> Assertions.assertEquals(ward.id(), selections.getFirst().finalTargetId()),
            () -> Assertions.assertEquals(ward.id(), selections.getLast().originalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selections.getLast().finalTargetId()),
            () -> Assertions.assertEquals(guard.id(),
                events(log, BattleEvent.AttackIntercepted.class).getFirst().interceptorId()),
            () -> Assertions.assertEquals(Optional.of(ward.id()),
                attacker.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(1, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(1, skillDamage(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.GUARD).size())
        );
    }

    @Test
    void interceptedOpeningLocksTheSurvivingGuardAsActualTarget() {
        final var attacker = penetrationAttacker(8);
        final var ward = target(Position.BACK, LARGE_HEALTH, 3, 0);
        final var guard = guard(Position.MID, LARGE_HEALTH, 2);
        final var closeTarget = target(Position.FRONT, LARGE_HEALTH, 1, 0);
        final var context = context(attacker, List.of(ward, guard, closeTarget));
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        attacker.move(context, log, 2);

        final var selections = events(log, BattleEvent.TargetSelected.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ward.id(), selections.getFirst().originalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selections.getFirst().finalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selections.getLast().originalTargetId()),
            () -> Assertions.assertEquals(guard.id(), selections.getLast().finalTargetId()),
            () -> Assertions.assertEquals(Optional.of(guard.id()),
                attacker.scalingSkills().penetrationTargetId()),
            () -> Assertions.assertEquals(1, attacker.scalingSkills().penetrationFollowUpsRemaining()),
            () -> Assertions.assertEquals(1, skillDamage(log, ActiveEnum.PENETRATION).size()),
            () -> Assertions.assertEquals(1, skillWindows(log, ActiveEnum.PENETRATION).size())
        );
    }

    private static BattleContext context(BattlePersonage attacker, List<BattlePersonage> targets) {
        return new BattleContext(List.of(attacker), targets, PenetrationTargetLockBattleTest::deterministicRoll);
    }

    private static BattlePersonage penetrationAttacker(int points) {
        return penetrationAttacker(points, 1);
    }

    private static BattlePersonage penetrationAttacker(int points, int maxRange) {
        return scalingAttacker(maxRange, Map.of(ActiveEnum.PENETRATION, points));
    }

    private static BattlePersonage penetrationAndHitAndRunAttacker() {
        return scalingAttacker(1, Map.of(
            ActiveEnum.PENETRATION, 8,
            ActiveEnum.HIT_AND_RUN, 8
        ));
    }

    private static BattlePersonage scalingAttacker(int maxRange, Map<ActiveEnum, Integer> skills) {
        return BattlePersonage.forScalingSkills(
            List.of(item(ATTACK, maxRange, LARGE_HEALTH, 0)),
            Position.FRONT,
            skills,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static BattlePersonage target(Position position, int health, int maxRange, int dodgeChance) {
        return new BattlePersonage(List.of(item(1, maxRange, health, dodgeChance)), position);
    }

    private static BattlePersonage guard(Position position, int health, int maxRange) {
        return BattlePersonage.forScalingSkills(
            List.of(item(1, maxRange, health, 0)),
            position,
            Map.of(ActiveEnum.GUARD, 8),
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static Item item(int attack, int maxRange, int health, int dodgeChance) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                List.of(new ItemAttack(AttackType.SLASH, 1, maxRange, attack)),
                Optional.empty(),
                health,
                0,
                dodgeChance,
                0,
                DEFAULT_SPEED,
                DEFAULT_THREAT,
                0,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static int deterministicRoll(String sequence, int minimum, int maximum) {
        if (sequence.startsWith("target-selection:")) {
            return minimum;
        }
        return minimum + (maximum - minimum) / 2;
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attempts(BattleActionLog log) {
        return log.traceEvents().stream()
            .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
            .toList();
    }

    private static List<BattleEvent.SkillWindowUsed> skillWindows(BattleActionLog log, ActiveEnum skill) {
        return events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == skill)
            .toList();
    }

    private static List<BattleEvent.ScalingSkillDamage> skillDamage(BattleActionLog log, ActiveEnum skill) {
        return events(log, BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == skill)
            .toList();
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }
}
