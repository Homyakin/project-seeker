package ru.homyakin.seeker.game.battle;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.utils.RandomUtils;

class PveAdaptationStudyTest {
    @ParameterizedTest
    @EnumSource(RaidType.class)
    void realRaidGenerationAndBattleReplayExactly(RaidType type) throws Exception {
        assertReplay(new Cell(Mode.RAID, type.name(), "MAIN", Tier.MID, 3, 10, true));
    }

    @ParameterizedTest
    @EnumSource(AnomalyPveTemplate.class)
    void realAnomalyGenerationAndBattleReplayExactly(AnomalyPveTemplate template) throws Exception {
        assertReplay(new Cell(Mode.ANOMALY_SAFE, template.name(), "MAIN", Tier.STRONG, 2, 0, true));
    }

    private void assertReplay(Cell cell) throws Exception {
        final var first = PveAdaptationStudy.run(cell, 104, 207, false);
        final var replay = PveAdaptationStudy.run(cell, 104, 207, false);
        Assertions.assertEquals(PveAdaptationStudy.JSON.valueToTree(first),
            PveAdaptationStudy.JSON.valueToTree(replay));
        Assertions.assertNotEquals("TIMEOUT", first.get("outcome"));
        Assertions.assertTrue((long) first.get("normalAttempts") > 0);
        final var mirrored = PveAdaptationStudy.run(cell, 104, 207, true);
        Assertions.assertEquals(PveAdaptationStudy.JSON.valueToTree(first.get("enemyBasis")),
            PveAdaptationStudy.JSON.valueToTree(mirrored.get("enemyBasis")));
        Assertions.assertEquals(first.get("calibration"), mirrored.get("calibration"));
    }

    @Test
    void raidsRetainEnhancementAndApplyEffectsButBothAnomalyInputsStripOnlyCopies() {
        final var raid = new Cell(Mode.RAID, RaidType.WOLFPACK.name(), "MAIN", Tier.STRONG, 3, 10, true);
        final var withoutEffects = new Cell(Mode.RAID, raid.encounter(), "MAIN", Tier.STRONG, 3, 10, false);
        final var preparedRaid = RandomUtils.withSeed(77, () -> PveAdaptationStudy.prepare(raid));
        final var unbuffedRaid = RandomUtils.withSeed(77, () -> PveAdaptationStudy.prepare(withoutEffects));
        for (int index = 0; index < 3; index++) {
            final var member = preparedRaid.members().get(index);
            Assertions.assertEquals(member.sourceItems(), member.combatItems());
            Assertions.assertTrue(member.sourceItems().stream().allMatch(item -> item.enhanceLevel() == 6));
            Assertions.assertTrue(preparedRaid.players().get(index).maxHealth()
                > unbuffedRaid.players().get(index).maxHealth());
            Assertions.assertTrue(preparedRaid.players().get(index).battlePersonageStats().attack()
                > unbuffedRaid.players().get(index).battlePersonageStats().attack());
        }
        for (final var mode : List.of(Mode.ANOMALY_SAFE, Mode.ANOMALY_FALLBACK)) {
            final var anomaly = new Cell(mode, AnomalyPveTemplate.values()[0].name(), "MAIN", Tier.STRONG, 3, 0, true);
            final var noEffects = new Cell(mode, anomaly.encounter(), "MAIN", Tier.STRONG, 3, 0, false);
            final var prepared = RandomUtils.withSeed(77, () -> PveAdaptationStudy.prepare(anomaly));
            final var plain = RandomUtils.withSeed(77, () -> PveAdaptationStudy.prepare(noEffects));
            Assertions.assertEquals(PveAdaptationStudy.JSON.valueToTree(prepared.players().stream()
                    .map(BattlePersonage::battlePersonageStats).toList()),
                PveAdaptationStudy.JSON.valueToTree(plain.players().stream()
                    .map(BattlePersonage::battlePersonageStats).toList()));
            for (final var member : prepared.members()) {
                for (int index = 0; index < member.sourceItems().size(); index++) {
                    final var original = member.sourceItems().get(index);
                    final var combat = member.combatItems().get(index);
                    Assertions.assertEquals(6, original.enhanceLevel());
                    Assertions.assertEquals(0, combat.enhanceLevel());
                    Assertions.assertSame(original.object(), combat.object());
                    Assertions.assertEquals(original.modifier(), combat.modifier());
                    Assertions.assertEquals(original.rarity(), combat.rarity());
                }
            }
        }
    }

    @Test
    void matrixIncludesAllModesAndNumericalBoundariesWithoutDuplicateCases() {
        final var cases = PveAdaptationStudy.cases(true);
        Assertions.assertEquals(cases.size(), cases.stream().distinct().count());
        for (final var type : RaidType.values()) {
            for (final int size : List.of(1, 2, 7, 8, 10, 11, 13, 14, 30)) {
                Assertions.assertTrue(cases.stream().anyMatch(cell -> cell.mode() == Mode.RAID
                    && cell.encounter().equals(type.name()) && cell.size() == size));
            }
        }
        for (final var mode : List.of(Mode.ANOMALY_SAFE, Mode.ANOMALY_FALLBACK)) {
            for (final var template : AnomalyPveTemplate.values()) {
                for (int size = 1; size <= 5; size++) {
                    final int requiredSize = size;
                    Assertions.assertTrue(cases.stream().anyMatch(cell -> cell.mode() == mode
                        && cell.encounter().equals(template.name()) && cell.size() == requiredSize));
                }
            }
        }
    }
}
