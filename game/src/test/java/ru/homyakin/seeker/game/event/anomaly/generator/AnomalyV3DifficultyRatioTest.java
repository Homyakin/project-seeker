package ru.homyakin.seeker.game.event.anomaly.generator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
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

class AnomalyV3DifficultyRatioTest {
    private static final long GENERATION_SEED = 202611170193L;
    private final AnomalySafePveGenerator generator = new AnomalySafePveGenerator();

    @ParameterizedTest
    @MethodSource("inputs")
    void selectedDifficultyAppliesOnlyToCurrentVersion(
        SkillFormulaVersion version, AnomalyPveTemplate template, int count
    ) {
        final var players = IntStream.range(0, count).mapToObj(AnomalyV3DifficultyRatioTest::player).toList();
        final var generated = RandomUtils.withSeed(GENERATION_SEED,
            () -> generator.generateCalibrated(template, players, version));
        final var current = version == SkillFormulaVersion.SCALING_SKILLS_V3;
        final var ratio = current ? 1.10 : 0.92;
        final var target = players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * ratio;
        final var calibration = generated.calibration();
        Assertions.assertEquals(target, calibration.targetPower());
        Assertions.assertTrue(Double.isFinite(calibration.targetPower()));
        Assertions.assertTrue(Double.isFinite(calibration.achievedPower()));
        Assertions.assertTrue(Double.isFinite(calibration.multiplier()));
        Assertions.assertTrue(calibration.multiplier() >= 0);
        Assertions.assertTrue(calibration.multiplier() <= PveCharacteristicScaling.maxMultiplier(90, 100, 340));
        Assertions.assertTrue(calibration.evaluations() > 0);
        Assertions.assertEquals(
            generated.enemies().stream().mapToDouble(BattlePersonage::legacyPower).sum(), calibration.achievedPower()
        );
        Assertions.assertTrue(generated.diagnostics().isEmpty());
        final var expectedCount = switch (count) {
            case 1 -> 2;
            case 2 -> new Random(GENERATION_SEED).nextInt(2, 4);
            default -> count;
        };
        Assertions.assertEquals(expectedCount, generated.enemies().size());
        for (int i = 0; i < expectedCount; i++) {
            final var enemy = generated.enemies().get(i);
            final var position = expectedPosition(template, i, expectedCount);
            Assertions.assertEquals(position, enemy.startPosition());
            final var expectedRange = current ? switch (position) {
                case FRONT -> template.attackType() == AttackType.MAGICAL ? 2 : 1;
                case MID -> 2;
                case BACK -> 3;
            } : template.attackType() == AttackType.MAGICAL || position == Position.BACK ? 2 : 1;
            Assertions.assertEquals(expectedRange, enemy.range());
            Assertions.assertEquals(expectedRange, enemy.attacksByRange().size());
            final var attack = PveCharacteristicScaling.multiply(90, calibration.multiplier());
            for (int distance = 1; distance <= expectedRange; distance++) {
                final var vector = enemy.attacksByRange().get(distance - 1);
                Assertions.assertEquals(distance, vector.range());
                Assertions.assertEquals(Map.of(template.attackType(), attack), vector.attack());
            }
            Assertions.assertEquals(PveCharacteristicScaling.multiply(340, calibration.multiplier()), enemy.maxHealth());
            Assertions.assertEquals(
                Map.of(template.defenseType(), PveCharacteristicScaling.multiply(100, calibration.multiplier())),
                enemy.defenses()
            );
            Assertions.assertEquals(version, enemy.skillSnapshots().getFirst().formulaVersion());
        }
    }

    private static Stream<Arguments> inputs() {
        return Stream.of(SkillFormulaVersion.values()).flatMap(version ->
            Stream.of(AnomalyPveTemplate.values()).flatMap(template ->
                IntStream.rangeClosed(1, 5).mapToObj(count -> Arguments.of(version, template, count))));
    }

    private static Position expectedPosition(AnomalyPveTemplate template, int index, int count) {
        return switch (template) {
            case ASH_WIND -> index == 0 ? Position.FRONT : Position.MID;
            case SHADOW_NEEDLES -> index < (count + 1) / 2 ? Position.FRONT : Position.BACK;
            case IRON_PHALANX -> Position.FRONT;
            case CRYSTAL_STORM -> index == 0 ? Position.FRONT : Position.BACK;
        };
    }

    private static BattlePersonage player(int index) {
        return BattlePersonage.forCombat(List.of(new Item(new ItemObject(
            "anomaly-difficulty-player-" + index, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 100 + index * 10)),
            Optional.of(new ItemDefense(DefenseType.LEATHER, 100 + index * 5)),
            300 + index * 20, 5, 5, 0.4, 180, 20, null
        ), Optional.empty(), ItemRarity.COMMON)), Position.FRONT, PersonageEffects.EMPTY,
            Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3);
    }
}
