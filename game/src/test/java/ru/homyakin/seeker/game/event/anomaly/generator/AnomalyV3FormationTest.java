package ru.homyakin.seeker.game.event.anomaly.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

class AnomalyV3FormationTest {
    private final AnomalySafePveGenerator generator = new AnomalySafePveGenerator();

    @ParameterizedTest
    @MethodSource("formations")
    void currentFactoryPreservesEveryDeclaredLineAndCompleteAttackVector(
        AnomalyPveTemplate template, int count, int expectedEnemies, long seed
    ) {
        final var players = IntStream.range(0, count).mapToObj(_ -> player()).toList();
        final var generated = RandomUtils.withSeed(seed, () -> generator.generateCalibrated(
            template, players, SkillFormulaVersion.SCALING_SKILLS_V3
        ));
        final var enemies = generated.enemies();
        Assertions.assertEquals(expectedEnemies, enemies.size());
        Assertions.assertEquals(
            players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * 1.10,
            generated.calibration().targetPower()
        );
        Assertions.assertEquals(
            enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum(),
            generated.calibration().achievedPower()
        );
        final var expectedPositions = positions(template, expectedEnemies);
        Assertions.assertEquals(expectedPositions, enemies.stream().map(BattlePersonage::startPosition).toList());
        for (int i = 0; i < enemies.size(); i++) {
            final var enemy = enemies.get(i);
            final var position = expectedPositions.get(i);
            final var range = switch (position) {
                case FRONT -> template.attackType() == AttackType.MAGICAL ? 2 : 1;
                case MID -> 2;
                case BACK -> 3;
            };
            final var attack = PveCharacteristicScaling.multiply(90, generated.calibration().multiplier());
            Assertions.assertEquals(range, enemy.range());
            Assertions.assertEquals(range, enemy.attacksByRange().size());
            for (int distance = 1; distance <= range; distance++) {
                final var vector = enemy.attacksByRange().get(distance - 1);
                Assertions.assertEquals(distance, vector.range());
                Assertions.assertEquals(java.util.Map.of(template.attackType(), attack), vector.attack());
            }
            Assertions.assertEquals(
                PveCharacteristicScaling.multiply(340, generated.calibration().multiplier()), enemy.maxHealth()
            );
            Assertions.assertEquals(
                java.util.Map.of(template.defenseType(),
                    PveCharacteristicScaling.multiply(100, generated.calibration().multiplier())),
                enemy.defenses()
            );
            Assertions.assertEquals(180, enemy.initiative());
            Assertions.assertEquals(5, enemy.critChance());
            Assertions.assertEquals(4, enemy.dodgeChance());
            Assertions.assertEquals(1, enemy.skillSnapshots().size());
            Assertions.assertEquals(expectedSkill(template, position), enemy.skillSnapshots().getFirst().code());
            Assertions.assertEquals(4, enemy.skillSnapshots().getFirst().points());
            Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V3, enemy.skillSnapshots().getFirst().formulaVersion()
            );
        }
        final var result = RandomUtils.withSeed(seed + 100_000, () -> new Battle(seed).process(
            enemies, players, 1000, BattleTempoVersion.ACCELERATED_V1
        ));
        for (int i = 0; i < enemies.size(); i++) {
            final var id = enemies.get(i).id();
            final var snapshot = result.initState().personagesById().get(id);
            final var actualLine = result.initState().lines().get(snapshot.lineIndex());
            Assertions.assertTrue(actualLine.firstTeam());
            Assertions.assertTrue(actualLine.personageIds().contains(id));
            Assertions.assertEquals(expectedPositions.get(i), actualLine.position());
        }
    }

    @ParameterizedTest
    @MethodSource("historicalInputs")
    void historicalVersionsKeepOriginalRangesAndSizeNormalization(
        SkillFormulaVersion version, AnomalyPveTemplate template, int count
    ) {
        final var players = IntStream.range(0, count).mapToObj(_ -> player()).toList();
        final var enemies = RandomUtils.withSeed(202611150019L, () -> generator.generate(template, players, version));
        Assertions.assertEquals(count == 0 ? 2 : 5, enemies.size());
        for (final var enemy : enemies) {
            Assertions.assertEquals(
                template.attackType() == AttackType.MAGICAL || enemy.startPosition() == Position.BACK ? 2 : 1,
                enemy.range()
            );
            Assertions.assertEquals(version, enemy.skillSnapshots().getFirst().formulaVersion());
        }
    }

    @Test
    void currentVersionRejectsInvalidInputsBeforeConsumingRandomness() {
        final var valid = List.of(player());
        final var withNull = Arrays.asList(player(), null);
        final var oversized = IntStream.range(0, 6).mapToObj(_ -> player()).toList();
        final List<Runnable> invalid = List.of(
            () -> generator.generate(null, valid, SkillFormulaVersion.SCALING_SKILLS_V3),
            () -> generator.generate(AnomalyPveTemplate.ASH_WIND, null, SkillFormulaVersion.SCALING_SKILLS_V3),
            () -> generator.generate(AnomalyPveTemplate.ASH_WIND, List.of(), SkillFormulaVersion.SCALING_SKILLS_V3),
            () -> generator.generate(AnomalyPveTemplate.ASH_WIND, oversized, SkillFormulaVersion.SCALING_SKILLS_V3),
            () -> generator.generate(AnomalyPveTemplate.ASH_WIND, withNull, SkillFormulaVersion.SCALING_SKILLS_V3),
            () -> generator.generate(AnomalyPveTemplate.ASH_WIND, valid, null)
        );
        for (final var action : invalid) {
            final var next = RandomUtils.withSeed(202611150019L, () -> {
                Assertions.assertThrows(IllegalArgumentException.class, action::run);
                return RandomUtils.getInInterval(0, Integer.MAX_VALUE - 1);
            });
            Assertions.assertEquals(
                RandomUtils.withSeed(202611150019L, () -> RandomUtils.getInInterval(0, Integer.MAX_VALUE - 1)), next
            );
        }
    }

    private static Stream<Arguments> formations() {
        return Stream.of(AnomalyPveTemplate.values()).flatMap(template -> Stream.of(
            Arguments.of(template, 1, 2, 202611150019L),
            Arguments.of(template, 2, 2, seedForCount(2)),
            Arguments.of(template, 2, 3, seedForCount(3)),
            Arguments.of(template, 3, 3, 202611150019L),
            Arguments.of(template, 4, 4, 202611150019L),
            Arguments.of(template, 5, 5, 202611150019L)
        ));
    }

    private static long seedForCount(int expected) {
        return IntStream.range(0, 100_000).filter(seed -> new Random(seed).nextInt(2, 4) == expected)
            .findFirst().orElseThrow();
    }

    private static Stream<Arguments> historicalInputs() {
        return Stream.of(SkillFormulaVersion.LEGACY_SKILLS_V1,
            SkillFormulaVersion.SCALING_SKILLS_V1, SkillFormulaVersion.SCALING_SKILLS_V2)
            .flatMap(version -> Stream.of(AnomalyPveTemplate.values()).flatMap(template -> Stream.of(
                Arguments.of(version, template, 0), Arguments.of(version, template, 6)
            )));
    }

    private static List<Position> positions(AnomalyPveTemplate template, int count) {
        final var positions = new ArrayList<Position>();
        for (int i = 0; i < count; i++) {
            positions.add(switch (template) {
                case ASH_WIND -> i == 0 ? Position.FRONT : Position.MID;
                case SHADOW_NEEDLES -> i < (count + 1) / 2 ? Position.FRONT : Position.BACK;
                case IRON_PHALANX -> Position.FRONT;
                case CRYSTAL_STORM -> i == 0 ? Position.FRONT : Position.BACK;
            });
        }
        return positions;
    }

    private static ActiveEnum expectedSkill(AnomalyPveTemplate template, Position position) {
        return switch (template) {
            case ASH_WIND -> ActiveEnum.HIT_AND_RUN;
            case SHADOW_NEEDLES -> position == Position.FRONT ? ActiveEnum.THORNS : ActiveEnum.BLEEDING;
            case IRON_PHALANX -> ActiveEnum.BERSERK;
            case CRYSTAL_STORM -> position == Position.FRONT ? ActiveEnum.KNOCKBACK : ActiveEnum.PRECISE_STRIKE;
        };
    }

    private static BattlePersonage player() {
        return BattlePersonage.forCombat(List.of(new Item(new ItemObject(
            "anomaly-formation-player", Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 100)),
            Optional.of(new ItemDefense(DefenseType.LEATHER, 100)), 300, 5, 5, 0.4, 180, 20, null
        ), Optional.empty(), ItemRarity.COMMON)), Position.FRONT, PersonageEffects.EMPTY,
            Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3);
    }
}
