package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.RandomUtils;

class VersionedExistingSkillsBattleTest {
    private static final int LARGE_HEALTH = 5_000;
    private static final BattleRandom SUCCESSFUL_RANDOM = VersionedExistingSkillsBattleTest::successfulRoll;

    @Test
    void hitBranchUsesSavedAttackAndRunsReactionsInTheApprovedOrder() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            100,
            0,
            Map.of(
                ActiveEnum.COUNTER_ATTACK, 4,
                ActiveEnum.DOUBLE_ATTACK, 4,
                ActiveEnum.BLEEDING, 4,
                ActiveEnum.KNOCKBACK, 4
            )
        );
        final var defender = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 80)),
            0,
            0,
            Map.of(
                ActiveEnum.COUNTER_ATTACK, 4,
                ActiveEnum.THORNS, 4,
                ActiveEnum.RETREAT, 4
            )
        );
        final var context = new BattleContext(List.of(attacker), List.of(defender), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        final var skillDamage = events(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertEquals(
            List.of(ActiveEnum.COUNTER_ATTACK, ActiveEnum.THORNS, ActiveEnum.DOUBLE_ATTACK),
            skillDamage.stream().map(BattleEvent.ScalingSkillDamage::skill).toList()
        );
        Assertions.assertEquals(List.of(21, 12, 36), skillDamage.stream()
            .map(BattleEvent.ScalingSkillDamage::damageTaken)
            .toList());
        Assertions.assertEquals(Map.of(AttackType.SLASH, 100), skillDamage.get(2).basis());

        final var movements = events(log, BattleEvent.PersonageForcedMove.class);
        Assertions.assertEquals(
            List.of(ActiveEnum.RETREAT, ActiveEnum.KNOCKBACK),
            movements.stream().map(BattleEvent.PersonageForcedMove::skill).toList()
        );
        Assertions.assertEquals(List.of(4, 5), movements.stream()
            .map(BattleEvent.PersonageForcedMove::newLineIndex)
            .toList());
        Assertions.assertEquals(4, attacker.scalingSkills().cooldown(ActiveEnum.BLEEDING));

        defender.move(context, log, 2);

        final var bleedingTick = events(log, BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == ActiveEnum.BLEEDING)
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertTrue(bleedingTick.periodic()),
            () -> Assertions.assertEquals(Map.of(AttackType.SLASH, 100), bleedingTick.basis()),
            () -> Assertions.assertEquals(18, bleedingTick.coefficientNumerator()),
            () -> Assertions.assertEquals(80, bleedingTick.coefficientDenominator()),
            () -> Assertions.assertEquals(22, bleedingTick.damageTaken())
        );
    }

    @Test
    void dodgeBranchRunsFeintBeforePreciseStrikeWithoutAReactionChain() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            0,
            0,
            Map.of(
                ActiveEnum.PRECISE_STRIKE, 4,
                ActiveEnum.COUNTER_ATTACK, 4
            )
        );
        final var defender = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 80)),
            0,
            100,
            Map.of(ActiveEnum.FEINT, 4)
        );
        final var context = new BattleContext(List.of(attacker), List.of(defender), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        Assertions.assertEquals(1, events(log, BattleEvent.AttackDodged.class).size());
        Assertions.assertTrue(events(log, BattleEvent.DamageReceived.class).isEmpty());
        final var skillDamage = events(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertEquals(
            List.of(ActiveEnum.FEINT, ActiveEnum.PRECISE_STRIKE),
            skillDamage.stream().map(BattleEvent.ScalingSkillDamage::skill).toList()
        );
        Assertions.assertEquals(List.of(36, 54), skillDamage.stream()
            .map(BattleEvent.ScalingSkillDamage::damageTaken)
            .toList());
    }

    @Test
    void berserkActivatesOnceAndAddsTheSummedBaseBonusAfterTemporaryChanges() {
        final var owner = scalingPersonage(
            Position.FRONT,
            1_000,
            List.of(
                attack(AttackType.SLASH, 1, 1, 2),
                attack(AttackType.SLASH, 1, 1, 2)
            ),
            0,
            0,
            Map.of(ActiveEnum.BERSERK, 3)
        );
        final var opponent = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        owner.applyVersionedSkillDamage(700, 700, opponent);
        owner.increaseAttack(100);
        final var context = new BattleContext(List.of(owner), List.of(opponent), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        owner.move(context, log, 1);
        owner.move(context, log, 2);

        Assertions.assertEquals(1, events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.BERSERK)
            .count());
        Assertions.assertEquals(Map.of(AttackType.SLASH, 9), owner.scalingAttackAt(1, AttackAccess.NORMAL));
        Assertions.assertEquals(List.of(9, 9), events(log, BattleEvent.DamageReceived.class).stream()
            .map(event -> event.roll().amount())
            .toList());
    }

    @Test
    void berserkDoesNotActivateWithoutAPositivePermanentAttack() {
        final var owner = scalingPersonage(
            Position.FRONT,
            1_000,
            List.of(),
            0,
            0,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var opponent = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        owner.applyVersionedSkillDamage(700, 700, opponent);
        final var context = new BattleContext(List.of(owner), List.of(opponent), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        owner.move(context, log, 1);

        Assertions.assertFalse(owner.scalingSkills().berserkActivated());
        Assertions.assertTrue(events(log, BattleEvent.SkillWindowUsed.class).stream()
            .noneMatch(event -> event.skill() == ActiveEnum.BERSERK));
    }

    @Test
    void hitAndRunStartsReadyThenAClosingTurnOnlyMovesAndCoolsDown() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            0,
            0,
            Map.of(ActiveEnum.HIT_AND_RUN, 3)
        );
        final var farTarget = personage(
            Position.MID,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 2, 1)),
            0,
            0
        );
        final var deadFrontAnchor = personage(
            Position.FRONT,
            1,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        deadFrontAnchor.applyVersionedSkillDamage(1, 1, attacker);
        final var context = new BattleContext(
            List.of(attacker),
            List.of(farTarget, deadFrontAnchor),
            SUCCESSFUL_RANDOM
        );
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        Assertions.assertEquals(1, events(log, BattleEvent.DamageReceived.class).size());
        Assertions.assertEquals(1, attacker.currentPosition());
        Assertions.assertEquals(4, attacker.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(1, events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.HIT_AND_RUN)
            .count());

        attacker.move(context, log, 2);

        Assertions.assertEquals(1, events(log, BattleEvent.DamageReceived.class).size());
        Assertions.assertEquals(2, attacker.currentPosition());
        Assertions.assertEquals(3, attacker.scalingSkills().cooldown(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(1, events(log, BattleEvent.MovedTowardEnemy.class).size());
    }

    @Test
    void selfHealUsesMaximumHealthAndDoesNotShortenANewCooldown() {
        final var owner = scalingPersonage(
            Position.FRONT,
            1_000,
            List.of(attack(AttackType.SLASH, 1, 1, 1)),
            0,
            0,
            Map.of(ActiveEnum.SELF_HEAL, 4)
        );
        final var opponent = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        owner.applyVersionedSkillDamage(200, 200, opponent);
        final var context = new BattleContext(List.of(owner), List.of(opponent), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        owner.move(context, log, 1);
        Assertions.assertEquals(2, owner.scalingSkills().cooldown(ActiveEnum.SELF_HEAL));
        owner.move(context, log, 2);
        owner.move(context, log, 3);
        owner.move(context, log, 4);

        final var healing = events(log, BattleEvent.PersonageHealed.class);
        Assertions.assertEquals(List.of(1, 4), healing.stream().map(BattleEvent::round).toList());
        Assertions.assertEquals(List.of(45, 45), healing.stream()
            .map(BattleEvent.PersonageHealed::amount)
            .toList());
        Assertions.assertEquals(890, owner.health());
        Assertions.assertEquals(2, owner.scalingSkills().cooldown(ActiveEnum.SELF_HEAL));
        Assertions.assertEquals(2, events(log, BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.skill() == ActiveEnum.SELF_HEAL)
            .count());
    }

    @Test
    void positiveSelfHealBasisStillHealsOneAfterRounding() {
        final var owner = scalingPersonage(
            Position.FRONT,
            2,
            List.of(attack(AttackType.SLASH, 1, 1, 1)),
            0,
            0,
            Map.of(ActiveEnum.SELF_HEAL, 1)
        );
        final var opponent = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        owner.applyVersionedSkillDamage(1, 1, opponent);
        final var context = new BattleContext(List.of(owner), List.of(opponent), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        owner.move(context, log, 1);

        Assertions.assertEquals(2, owner.health());
        Assertions.assertEquals(1, events(log, BattleEvent.PersonageHealed.class).getFirst().amount());
    }

    @Test
    void legacyRetreatStillFinishesAfterScalingCounterKillsTheAttacker() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            20,
            List.of(attack(AttackType.SLASH, 1, 1, 1)),
            100,
            0,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var defender = BattlePersonage.withSkillVersions(
            List.of(item(
                LARGE_HEALTH,
                List.of(attack(AttackType.BLUNT, 1, 1, 100)),
                0,
                0
            )),
            Position.FRONT,
            Map.of(
                ActiveEnum.COUNTER_ATTACK, 8,
                ActiveEnum.RETREAT, 8
            ),
            Map.of(
                ActiveEnum.COUNTER_ATTACK, SkillFormulaVersion.SCALING_SKILLS_V1,
                ActiveEnum.RETREAT, SkillFormulaVersion.LEGACY_SKILLS_V1
            )
        );
        final var backAnchor = personage(
            Position.BACK,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            0,
            0
        );
        final var context = new BattleContext(
            List.of(attacker),
            List.of(defender, backAnchor),
            SUCCESSFUL_RANDOM
        );
        final int defenderPositionBefore = defender.currentPosition();
        final var log = new BattleActionLog();

        RandomUtils.withSeed(2, () -> {
            attacker.move(context, log, 1);
            return null;
        });

        Assertions.assertFalse(attacker.isAlive());
        Assertions.assertEquals(defenderPositionBefore + 1, defender.currentPosition());
        Assertions.assertEquals(List.of(ActiveEnum.RETREAT), events(log, BattleEvent.PersonageForcedMove.class).stream()
            .map(BattleEvent.PersonageForcedMove::skill)
            .toList());
    }

    @Test
    void bleedingTicksFourTimesAndThenExpires() {
        final var source = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            0,
            0,
            Map.of(ActiveEnum.BLEEDING, 1)
        );
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        final var context = new BattleContext(List.of(source), List.of(target), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        source.move(context, log, 1);
        for (int round = 2; round <= 5; round++) {
            target.move(context, log, round);
        }

        final var ticks = events(log, BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.skill() == ActiveEnum.BLEEDING)
            .toList();
        Assertions.assertEquals(4, ticks.size());
        Assertions.assertTrue(ticks.stream().allMatch(BattleEvent.ScalingSkillDamage::periodic));
        Assertions.assertTrue(ticks.stream().allMatch(event -> event.damageTaken() == 10));
        Assertions.assertTrue(target.scalingBleedings().isEmpty());
        Assertions.assertEquals(4, source.scalingSkills().cooldown(ActiveEnum.BLEEDING));
    }

    @Test
    void replacingBleedingUsesTheLatestBasisAndDeathStopsLaterEffects() {
        final var target = personage(
            Position.FRONT,
            10,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        final var firstSource = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 1)),
            0,
            0,
            Map.of(ActiveEnum.BLEEDING, 8)
        );
        final var secondSource = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            0,
            0,
            Map.of(ActiveEnum.BLEEDING, 8)
        );
        final var lowerIdSource = firstSource.id().compareTo(secondSource.id()) < 0 ? firstSource : secondSource;
        final var higherIdSource = lowerIdSource == firstSource ? secondSource : firstSource;
        target.addOrReplaceScalingBleeding(bleeding(lowerIdSource, 1));
        target.addOrReplaceScalingBleeding(bleeding(lowerIdSource, 100));
        target.addOrReplaceScalingBleeding(bleeding(higherIdSource, 100));
        final var context = new BattleContext(
            List.of(target),
            List.of(firstSource, secondSource),
            SUCCESSFUL_RANDOM
        );
        final var log = new BattleActionLog();

        target.move(context, log, 1);

        final var ticks = events(log, BattleEvent.ScalingSkillDamage.class);
        Assertions.assertEquals(1, ticks.size());
        Assertions.assertEquals(lowerIdSource.id(), ticks.getFirst().sourceId());
        Assertions.assertEquals(Map.of(AttackType.SLASH, 100), ticks.getFirst().basis());
        Assertions.assertEquals(10, ticks.getFirst().damageTaken());
        Assertions.assertFalse(target.isAlive());
        Assertions.assertEquals(1, events(log, BattleEvent.PersonageDefeated.class).size());
    }

    @Test
    void legacyPeriodicDamageLogsThreatLossBeforeDefeatInACommonTurn() {
        final var source = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 1)),
            0,
            0,
            Map.of(ActiveEnum.BLEEDING, 1)
        );
        final var target = personage(
            Position.FRONT,
            10,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        target.changeBonusThreat(20);
        target.addPeriodicDamageEffect(new PeriodicDamageEffect(
            AttackType.SLASH,
            100,
            1,
            1,
            source.id(),
            ActiveEnum.BLEEDING
        ));
        final var context = new BattleContext(List.of(target), List.of(source), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        target.move(context, log, 1);

        Assertions.assertEquals(
            List.of(
                BattleEvent.EffectDamage.class,
                BattleEvent.ThreatChanged.class,
                BattleEvent.PersonageDefeated.class
            ),
            log.events().stream().map(Object::getClass).toList()
        );
        final var threat = events(log, BattleEvent.ThreatChanged.class).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(-8, threat.delta()),
            () -> Assertions.assertEquals(BattleEvent.ThreatReason.DAMAGE_TAKEN, threat.reason()),
            () -> Assertions.assertFalse(target.isAlive())
        );
    }

    @Test
    void zeroActualDamageDoesNotReduceThreat() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(),
            0,
            0,
            Map.of(ActiveEnum.ACCUMULATION, 1)
        );
        final var target = personage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 1)),
            0,
            0
        );
        target.changeBonusThreat(20);
        final int threatBefore = target.totalThreat();
        final var context = new BattleContext(List.of(attacker), List.of(target), SUCCESSFUL_RANDOM);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);

        Assertions.assertEquals(threatBefore, target.totalThreat());
        Assertions.assertEquals(0, events(log, BattleEvent.DamageReceived.class).getFirst().damageTaken());
        Assertions.assertTrue(events(log, BattleEvent.ThreatChanged.class).stream().noneMatch(event ->
            event.personageId().equals(target.id())
                && event.reason() == BattleEvent.ThreatReason.DAMAGE_TAKEN
        ));
    }

    @Test
    void anotherParticipantsRollsDoNotShiftTheSameAttackersNormalSequence() {
        final var baseline = RandomUtils.withSeed(91, VersionedExistingSkillsBattleTest::randomScenario);
        final var interleaved = RandomUtils.withSeed(91, VersionedExistingSkillsBattleTest::randomScenario);

        for (int round = 1; round <= 20; round++) {
            baseline.attacker().move(baseline.context(), baseline.log(), round);
            interleaved.otherAttacker().move(interleaved.context(), interleaved.log(), round);
            interleaved.attacker().move(interleaved.context(), interleaved.log(), round);
        }

        Assertions.assertEquals(baseline.attacker().id(), interleaved.attacker().id());
        Assertions.assertEquals(baseline.target().id(), interleaved.target().id());
        Assertions.assertEquals(
            normalOutcomes(baseline.log(), baseline.attacker()),
            normalOutcomes(interleaved.log(), interleaved.attacker())
        );
    }

    private static RandomScenario randomScenario() {
        final var attacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.SLASH, 1, 1, 100)),
            25,
            0,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var otherAttacker = scalingPersonage(
            Position.FRONT,
            LARGE_HEALTH,
            List.of(attack(AttackType.BLUNT, 1, 1, 100)),
            25,
            0,
            Map.of(ActiveEnum.BERSERK, 1)
        );
        final var target = personage(
            Position.FRONT,
            1_000_000,
            List.of(attack(AttackType.PIERCE, 1, 1, 1)),
            0,
            25
        );
        return new RandomScenario(
            attacker,
            otherAttacker,
            target,
            new BattleContext(
                List.of(attacker, otherAttacker),
                List.of(target),
                new SeededBattleRandom(20260913)
            ),
            new BattleActionLog()
        );
    }

    private static List<String> normalOutcomes(BattleActionLog log, BattlePersonage attacker) {
        return log.events().stream()
            .filter(event -> event instanceof BattleEvent.DamageReceived damage
                    && damage.attackerId().equals(attacker.id())
                || event instanceof BattleEvent.AttackDodged dodge
                    && dodge.attackerId().equals(attacker.id()))
            .map(event -> {
                if (event instanceof BattleEvent.DamageReceived damage) {
                    return "hit:" + damage.roll().crit() + ":" + damage.damageTaken();
                }
                return "dodge";
            })
            .toList();
    }

    private static ScalingBleedingEffect bleeding(BattlePersonage source, int basis) {
        return new ScalingBleedingEffect(
            source,
            Map.of(AttackType.SLASH, basis),
            30,
            80,
            4
        );
    }

    private static BattlePersonage scalingPersonage(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance,
        Map<ActiveEnum, Integer> skills
    ) {
        return BattlePersonage.forScalingSkills(
            List.of(item(health, attacks, criticalChance, dodgeChance)),
            position,
            skills
        );
    }

    private static BattlePersonage personage(
        Position position,
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance
    ) {
        return new BattlePersonage(
            List.of(item(health, attacks, criticalChance, dodgeChance)),
            position
        );
    }

    private static Item item(
        int health,
        List<ItemAttack> attacks,
        int criticalChance,
        int dodgeChance
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                attacks,
                Optional.empty(),
                health,
                criticalChance,
                dodgeChance,
                0,
                100,
                10,
                0,
                ItemProgressionVersion.V1,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static ItemAttack attack(AttackType type, int minimumRange, int maximumRange, int amount) {
        return new ItemAttack(type, minimumRange, maximumRange, amount);
    }

    private static int successfulRoll(String sequence, int minimum, int maximum) {
        if (sequence.startsWith("normal-damage:")) {
            return minimum + (maximum - minimum) / 2;
        }
        if (sequence.startsWith("skill-damage-")) {
            return 0;
        }
        return minimum;
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    private record RandomScenario(
        BattlePersonage attacker,
        BattlePersonage otherAttacker,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log
    ) {
    }
}
