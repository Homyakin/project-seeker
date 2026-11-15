package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

class MyconidColonyAdaptationTest {
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final BattleRandom SUCCESSFUL_RANDOM = (sequence, minimum, maximum) -> minimum;

    @ParameterizedTest
    @MethodSource("groupSizes")
    void everyGroupKeepsItsCountFormationFourPointSkillsAndMagicalArcaneProfile(int size) {
        final var generated = generate(players(size), V3);
        final var colony = generated.enemies();
        final int count = Math.max(2, size);
        final int frontCount = count / 2;
        Assertions.assertEquals(count, colony.size());
        for (int index = 0; index < count; index++) {
            final var enemy = colony.get(index);
            final boolean guardian = index < frontCount;
            Assertions.assertEquals(guardian ? Position.FRONT : Position.BACK, enemy.startPosition());
            Assertions.assertEquals(guardian ? 1 : 3, enemy.range());
            Assertions.assertEquals(List.of(new BattleSkillInitSnapshot(
                guardian ? ActiveEnum.KNOCKBACK : ActiveEnum.PRECISE_STRIKE, 4, V3
            )), enemy.skillSnapshots());
            Assertions.assertEquals(Set.of(DefenseType.ARCANE), enemy.defenses().keySet());
            Assertions.assertEquals(guardian ? 1 : 3, enemy.attacksByRange().size());
            for (final var attack : enemy.attacksByRange()) {
                Assertions.assertEquals(Set.of(AttackType.MAGICAL), attack.attack().keySet());
                Assertions.assertTrue(attack.attack().get(AttackType.MAGICAL) > 0);
            }
        }
        final var finalPower = equipmentPowerSum(colony);
        Assertions.assertEquals(generated.calibration().achievedPower(), finalPower, finalPower * 1e-12);
    }

    @Test
    void groupTargetUsesTheEquipmentPowerSumWithoutTheWithdrawnTeamSizeCorrection() {
        var onePlayerTarget = 0.0;
        for (int size = 1; size <= 30; size++) {
            final var players = players(size);
            final var generated = generate(players, V3);
            final var playerPower = equipmentPowerSum(players);
            final var normalizedTarget = generated.calibration().targetPower() / playerPower;
            final var count = Math.max(2, size);
            final var groupCorrection = 1 + MathUtils.log(4, count) * 0.05;
            Assertions.assertEquals(1.14 * groupCorrection * 0.82,
                normalizedTarget, 1e-12);
            if (size == 1) {
                onePlayerTarget = normalizedTarget;
            } else if (size == 2) {
                Assertions.assertEquals(onePlayerTarget, normalizedTarget, 1e-12);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true, 1", "true, 2", "true, 3", "false, 1", "false, 2", "false, 3"})
    void aBackSlingerAttacksAtEverySupportedDistanceWithoutAFalseApproach(boolean firstTeam, int distance) {
        final var fixture = fixture(firstTeam);
        final int startLine = fixture.slinger().currentPosition();
        Assertions.assertEquals(firstTeam ? 0 : 5, startLine);
        Assertions.assertEquals(3, fixture.slinger().distanceTo(fixture.opponent()));
        for (int step = 3; step > distance; step--) {
            fixture.context().moveTowardEnemy(fixture.opponent());
        }
        Assertions.assertEquals(distance, fixture.slinger().distanceTo(fixture.opponent()));
        final var initialHealth = fixture.opponent().health();
        final var log = new BattleActionLog();

        fixture.slinger().move(fixture.context(), log, 1);

        Assertions.assertEquals(startLine, fixture.slinger().currentPosition());
        Assertions.assertTrue(log.events().stream().noneMatch(BattleEvent.MovedTowardEnemy.class::isInstance));
        final var attempts = attempts(log);
        Assertions.assertEquals(1, attempts.size());
        Assertions.assertEquals(distance, attempts.getFirst().distance());
        Assertions.assertEquals(fixture.opponent().id(), attempts.getFirst().finalTargetId());
        Assertions.assertTrue(attempts.getFirst().savedBasis().get(AttackType.MAGICAL) > 0);
        Assertions.assertTrue(fixture.opponent().health() < initialHealth);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aSlingerCannotDealDamageBeyondThreeLinesAndApproachesInstead(boolean firstTeam) {
        final var fixture = fixture(firstTeam);
        fixture.context().moveBackward(fixture.opponent(), 1);
        Assertions.assertEquals(4, fixture.slinger().distanceTo(fixture.opponent()));
        final var initialHealth = fixture.opponent().health();
        final var startLine = fixture.slinger().currentPosition();
        final var log = new BattleActionLog();

        fixture.slinger().move(fixture.context(), log, 1);

        Assertions.assertTrue(attempts(log).isEmpty());
        Assertions.assertEquals(initialHealth, fixture.opponent().health());
        Assertions.assertEquals(startLine + (firstTeam ? 1 : -1), fixture.slinger().currentPosition());
        Assertions.assertEquals(List.of(new BattleEvent.MovedTowardEnemy(
            fixture.slinger().id(), fixture.slinger().currentPosition(), 1
        )), log.events());
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"LEGACY_SKILLS_V1", "SCALING_SKILLS_V1", "SCALING_SKILLS_V2"})
    void olderFormulaSelectionsRetainTheirOriginalRangeAndGroupTarget(SkillFormulaVersion version) {
        for (final int size : new int[]{1, 2, 7, 8, 30}) {
            final var players = players(size);
            final var generated = generate(players, version);
            final int count = Math.max(2, size);
            final var groupCorrection = 1.0 + MathUtils.log(count <= 7 ? 2.2 : 4, count) * 0.05;
            final var expected = players.stream().mapToDouble(BattlePersonage::legacyPower).sum()
                * 1.14 * groupCorrection * 0.77;
            Assertions.assertEquals(expected, generated.calibration().targetPower());
            Assertions.assertTrue(generated.enemies().stream().allMatch(enemy -> enemy.range() == 1));
            Assertions.assertTrue(generated.enemies().stream().allMatch(enemy ->
                enemy.skillSnapshots().getFirst().formulaVersion() == version));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void separateGenerationAndBattleSeedsReplayTheCompleteBattle(boolean reverse) {
        final var first = simulate(2026110815000L, 3026110815000L, reverse);
        final var repeated = simulate(2026110815000L, 3026110815000L, reverse);
        Assertions.assertEquals(first.initState(), repeated.initState());
        Assertions.assertEquals(first.actionLog().events(), repeated.actionLog().events());
        Assertions.assertEquals(first.actionLog().traceEvents(), repeated.actionLog().traceEvents());
        Assertions.assertEquals(first.rounds(), repeated.rounds());
        Assertions.assertEquals(first.firstWin(), repeated.firstWin());
        Assertions.assertEquals(first.personageStats(), repeated.personageStats());
    }

    @Test
    void trialCountDoesNotChangeFinalIdentifiersInitiativeOrTheOuterRandomStream() {
        final var minimum = generationRandomness(0);
        final var calibrated = generationRandomness(2.04);

        Assertions.assertNotEquals(minimum.evaluations(), calibrated.evaluations());
        Assertions.assertEquals(minimum.identifiers(), calibrated.identifiers());
        Assertions.assertEquals(minimum.initiative(), calibrated.initiative());
        Assertions.assertEquals(minimum.nextDraw(), calibrated.nextDraw());
    }

    @Test
    void reorderingDifferentParticipantsDoesNotChangeCalibrationDiagnosticsOrFinalEnemies() {
        final var participants = Step123ReferenceTeams.mainTeam(Step123ReferenceTeams.Tier.MID, 3).personages();
        final var first = generate(participants, V3);
        final var reordered = generate(participants.reversed(), V3);

        Assertions.assertEquals(first.calibration(), reordered.calibration());
        Assertions.assertEquals(first.diagnostics(), reordered.diagnostics());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::id).toList(),
            reordered.enemies().stream().map(BattlePersonage::id).toList());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
            reordered.enemies().stream().map(BattlePersonage::initiativeGauge).toList());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::battlePersonageStats).toList(),
            reordered.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void theExistingLongbowCanUseItsFarProfileAgainstTheActualBackRow(boolean reverse) {
        final var cell = new PveAdaptationStudy.Cell(PveAdaptationStudy.Mode.RAID, "MYCONID_COLONY", "MAIN",
            Step123ReferenceTeams.Tier.BEGINNER, 3, 10, false);
        final var prepared = RandomUtils.withSeed(2026110815L, () -> PveAdaptationStudy.prepare(cell));
        final var colony = prepared.generated().enemies();
        // Tactics change weights, not certainty: select the last eligible target for this branch.
        final BattleRandom farTarget = (sequence, minimum, maximum) ->
            sequence.startsWith("target-selection:") ? maximum : minimum;
        final var context = new BattleContext(reverse ? prepared.players() : colony,
            reverse ? colony : prepared.players(), farTarget, BattleTempoVersion.ACCELERATED_V1);
        final var archer = prepared.players().get(1);
        Assertions.assertEquals(2, archer.distanceTo(colony.getFirst()));
        Assertions.assertEquals(4, archer.distanceTo(colony.getLast()));
        final var profile = archer.attacksByRange();
        final var nearAttack = profile.stream().filter(attack -> attack.range() == 2).findFirst().orElseThrow();
        final var farAttack = profile.stream().filter(attack -> attack.range() == 4).findFirst().orElseThrow();
        Assertions.assertEquals(240, nearAttack.attack().get(AttackType.PIERCE));
        Assertions.assertEquals(360, farAttack.attack().get(AttackType.PIERCE));

        final var log = new BattleActionLog();
        archer.move(context, log, 1);

        Assertions.assertFalse(attempts(log).isEmpty());
        final var attempt = attempts(log).getFirst();
        Assertions.assertEquals(4, attempt.distance());
        Assertions.assertTrue(colony.subList(1, colony.size()).stream()
            .anyMatch(enemy -> enemy.id().equals(attempt.finalTargetId())));
    }

    private static IntStream groupSizes() {
        return IntStream.rangeClosed(1, 30);
    }

    private static double equipmentPowerSum(List<BattlePersonage> personages) {
        return personages.stream().mapToDouble(BattlePersonage::equipmentPowerUnscaled)
            .sorted().reduce(0.0, Double::sum);
    }

    private static GeneratedPveEnemies generate(List<BattlePersonage> players, SkillFormulaVersion version) {
        return RandomUtils.withSeed(2026110815L, () ->
            new MyconidColonyGenerator().generateCalibrated(players, 1.14, version));
    }

    private static List<BattlePersonage> players(int size) {
        return IntStream.range(0, size).mapToObj(ignored -> player()).toList();
    }

    private static BattlePersonage player() {
        return BattlePersonage.forCombat(
            List.of(Item.fromObject(new ItemObject(
                null,
                Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(AttackType.PIERCE, 1, 500)),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 300)),
                1_000_000, 0, 0, 0, 200, 10, null
            ))),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            V3
        );
    }

    private static Fixture fixture(boolean firstTeam) {
        return RandomUtils.withSeed(2026110815L, () -> {
            final var opponent = player();
            final var colony = new MyconidColonyGenerator().generate(List.of(opponent), 1.14, V3);
            final var slinger = colony.getLast();
            final var context = new BattleContext(
                firstTeam ? colony : List.of(opponent),
                firstTeam ? List.of(opponent) : colony,
                SUCCESSFUL_RANDOM,
                BattleTempoVersion.ACCELERATED_V1
            );
            return new Fixture(context, slinger, opponent);
        });
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attempts(BattleActionLog log) {
        return log.traceEvents().stream()
            .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
            .toList();
    }

    private static BattleResult simulate(long generationSeed, long battleSeed, boolean reverse) {
        final var prepared = RandomUtils.withSeed(generationSeed, () -> {
            final var players = Step123ReferenceTeams.mainTeam(Step123ReferenceTeams.Tier.MID, 3).personages();
            final var colony = new MyconidColonyGenerator().generate(players, 1.14, V3);
            return List.of(players, colony);
        });
        return RandomUtils.withSeed(battleSeed, () -> new Battle(battleSeed).process(
            prepared.get(reverse ? 1 : 0), prepared.get(reverse ? 0 : 1),
            10_000, BattleTempoVersion.ACCELERATED_V1
        ));
    }

    private static GenerationRandomness generationRandomness(double powerBonus) {
        return RandomUtils.withSeed(20261108152L, () -> {
            final var generated = new MyconidColonyGenerator().generateCalibrated(players(3), powerBonus, V3);
            return new GenerationRandomness(
                generated.enemies().stream().map(BattlePersonage::id).toList(),
                generated.enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
                RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1), generated.calibration().evaluations()
            );
        });
    }

    private record GenerationRandomness(
        List<java.util.UUID> identifiers,
        List<Integer> initiative,
        long nextDraw,
        int evaluations
    ) {
    }

    private record Fixture(BattleContext context, BattlePersonage slinger, BattlePersonage opponent) {
    }
}
