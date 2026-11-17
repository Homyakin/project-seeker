package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step192AnomalyFormationStudy.Sample;
import ru.homyakin.seeker.game.battle.Step193AnomalyDifficultyStudy.Phase;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Preregistered inputs and seeds only: these tests never run a battle or choose a coefficient. */
class Step193AnomalyDifficultyStudyTest {
    @Test
    void matrixContainsOnlyTheApprovedThreeToFivePlayerMainDirectionAndDefaultCells() {
        for (final var phase : Phase.values()) {
            final var cases = Step193AnomalyDifficultyStudy.cases(phase);
            Assertions.assertEquals(72, cases.size());
            Assertions.assertEquals(Map.of("A_MAIN", 36L, "C_DIRECTIONS", 24L, "D_DEFAULTS", 12L),
                cases.stream().collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
            for (int index = 0; index < cases.size(); index++) {
                final var sample = cases.get(index);
                Assertions.assertEquals(index + 1, sample.caseId());
                Assertions.assertTrue(List.of(3, 4, 5).contains(sample.cell().size()));
                Assertions.assertEquals(PveAdaptationStudy.Mode.ANOMALY_SAFE, sample.cell().mode());
                Assertions.assertEquals(0, sample.cell().raidLevel());
                Assertions.assertFalse(sample.cell().effects());
                Assertions.assertEquals(phase == Phase.SCREEN ? 20 : 80, sample.pairs());
            }
            for (final var template : AnomalyPveTemplate.values()) {
                for (final int size : List.of(3, 4, 5)) {
                    final var cells = cases.stream().filter(sample -> sample.cell().encounter().equals(template.name())
                        && sample.cell().size() == size).toList();
                    Assertions.assertEquals(6, cells.size());
                    Assertions.assertEquals(3, cells.stream().filter(sample -> sample.family().equals("A_MAIN")).count());
                    Assertions.assertEquals(2, cells.stream().filter(sample -> sample.family().equals("C_DIRECTIONS")).count());
                    Assertions.assertEquals(1, cells.stream().filter(sample -> sample.family().equals("D_DEFAULTS")).count());
                }
            }
        }
    }

    @Test
    void candidateSetAndBattleCountsCannotChangeAfterInspectingResults() {
        Assertions.assertEquals(List.of("0.92", "1.10", "1.30", "1.50", "1.75", "2.00", "2.40"),
            Step193AnomalyDifficultyStudy.ratios(Phase.SCREEN, Map.of()));
        Assertions.assertEquals(List.of("0.92", "1.50"),
            Step193AnomalyDifficultyStudy.ratios(Phase.CONFIRM, Map.of("selectedRatio", "1.50")));
        Assertions.assertEquals(20160, Step193AnomalyDifficultyStudy.battleCount(Phase.SCREEN, 7));
        Assertions.assertEquals(23040, Step193AnomalyDifficultyStudy.battleCount(Phase.CONFIRM, 2));
        for (final var invalid : List.of("0.92", "1.11", "NaN", "", "3.00")) {
            Assertions.assertThrows(IllegalStateException.class, () ->
                Step193AnomalyDifficultyStudy.ratios(Phase.CONFIRM, Map.of("selectedRatio", invalid)));
        }
        Assertions.assertThrows(IllegalStateException.class,
            () -> Step193AnomalyDifficultyStudy.ratios(Phase.CONFIRM, Map.of()));
        Assertions.assertThrows(IllegalStateException.class,
            () -> Step193AnomalyDifficultyStudy.ratios(Phase.SCREEN, Map.of("selectedRatio", "1.50")));
    }

    @Test
    void everyInputUsesRealV5ObjectsAndModifiersAndFillsEverySlotExactlyOnce() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5);
        final Map<String, ItemObject> objects = java.util.stream.Stream.concat(catalog.itemObjects().stream(),
            catalog.defaultItems().stream()).collect(Collectors.toMap(ItemObject::code, Function.identity(),
                (first, second) -> first));
        final var modifiers = catalog.modifiers().stream().collect(Collectors.toMap(
            ru.homyakin.seeker.game.item.models.Modifier::activeEnum, Function.identity()));
        for (final var sample : Step193AnomalyDifficultyStudy.cases(Phase.SCREEN)) {
            for (final var member : sample.members()) {
                final var occupied = java.util.EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    Assertions.assertEquals(objects.get(item.object().code()), item.object());
                    item.modifier().ifPresent(modifier -> Assertions.assertEquals(modifiers.get(modifier.activeEnum()), modifier));
                    for (final var slot : item.object().slots()) {
                        Assertions.assertTrue(occupied.add(slot));
                    }
                }
                Assertions.assertEquals(java.util.EnumSet.allOf(PersonageSlot.class), occupied);
            }
        }
    }

    @Test
    void combatCopiesDropEnhancementAndEffectsWithoutMutatingPermanentInputs() {
        for (final var sample : Step193AnomalyDifficultyStudy.cases(Phase.SCREEN)) {
            final var original = Step192AnomalyFormationStudy.canonicalTree(sample.members());
            final var prepared = RandomUtils.withSeed(71_000L + sample.caseId(), () -> Step192AnomalyFormationStudy.prepare(
                sample, Step192AnomalyFormationStudy.currentGenerator(), PveAdaptationStudy.FORMULA));
            Assertions.assertEquals(original, Step192AnomalyFormationStudy.canonicalTree(sample.members()));
            for (int index = 0; index < sample.members().size(); index++) {
                final var member = prepared.members().get(index);
                Assertions.assertEquals(sample.members().get(index).items(), member.sourceItems());
                Assertions.assertEquals(member.sourceItems().stream().map(Item::withoutStormEnhance).toList(), member.combatItems());
                Assertions.assertTrue(member.combatItems().stream().allMatch(item -> item.enhanceLevel() == 0));
                final var actor = prepared.players().get(index);
                final var expected = BattlePersonage.forCombat(member.combatItems(), member.position(), PersonageEffects.EMPTY,
                    java.util.Optional.of(member.build()), PveAdaptationStudy.FORMULA);
                Assertions.assertEquals(expected.health(), actor.health());
                Assertions.assertEquals(expected.attackAtRange(1), actor.attackAtRange(1));
                Assertions.assertEquals(expected.defenses(), actor.defenses());
                Assertions.assertEquals(expected.legacyPower(), actor.legacyPower());
            }
        }
    }

    @Test
    void screenAndConfirmationRootsAreDistinctFromOneAnotherAndFromFrozenFormationStudy() {
        Assertions.assertEquals(20_261_203_000_000L, Phase.SCREEN.generationRoot);
        Assertions.assertEquals(30_261_203_000_000L, Phase.SCREEN.battleRoot);
        Assertions.assertEquals(20_261_204_000_000L, Phase.CONFIRM.generationRoot);
        Assertions.assertEquals(30_261_204_000_000L, Phase.CONFIRM.battleRoot);
        final var seenGeneration = new java.util.HashSet<Long>();
        final var seenBattle = new java.util.HashSet<Long>();
        for (final var phase : Phase.values()) {
            final var samples = Step193AnomalyDifficultyStudy.cases(phase);
            final var plans = Step193AnomalyDifficultyStudy.plans(phase, samples);
            Assertions.assertEquals(72, plans.size());
            for (final var sample : samples) {
                final var seeds = plans.get(sample.caseId());
                Assertions.assertEquals(sample.pairs(), seeds.size());
                for (int pair = 0; pair < seeds.size(); pair++) {
                    final var seed = seeds.get(pair);
                    Assertions.assertEquals(pair, seed.pair());
                    Assertions.assertEquals(phase.generationRoot + sample.caseId() * 1000L + pair, seed.generationSeed());
                    Assertions.assertEquals(phase.battleRoot + sample.caseId() * 1000L + pair, seed.battleSeed());
                    Assertions.assertEquals(sample.cell().size(), seed.expectedEnemyCount());
                    Assertions.assertTrue(seenGeneration.add(seed.generationSeed()));
                    Assertions.assertTrue(seenBattle.add(seed.battleSeed()));
                    Assertions.assertTrue(seed.generationSeed() > Step192AnomalyFormationStudy.GENERATION_ROOT + 1_000_000L);
                }
            }
            Assertions.assertEquals(plans, Step193AnomalyDifficultyStudy.plans(phase, samples));
        }
        Assertions.assertEquals(7200, seenGeneration.size());
        Assertions.assertEquals(7200, seenBattle.size());
    }

    @Test
    void replaySelectionIsPredeclaredAndDoesNotAddObservationsToEitherSample() {
        for (final var phase : Phase.values()) {
            int selected = 0;
            for (final var sample : Step193AnomalyDifficultyStudy.cases(phase)) {
                for (int pair = 0; pair < sample.pairs(); pair++) {
                    final boolean expected = pair == 0 && (sample.family().equals("A_MAIN") && sample.cell().tier() == Tier.MID
                        || phase == Phase.CONFIRM && (sample.cell().style().equals("COMMAND_CUIRASS")
                            || sample.family().equals("D_DEFAULTS")));
                    Assertions.assertEquals(expected, Step193AnomalyDifficultyStudy.replaySelected(phase, sample, pair));
                    if (expected) {
                        ++selected;
                    }
                }
            }
            Assertions.assertEquals(phase == Phase.SCREEN ? 12 : 36, selected);
        }
        Assertions.assertEquals(168, Step193AnomalyDifficultyStudy.replayCount(Phase.SCREEN, 7));
        Assertions.assertEquals(144, Step193AnomalyDifficultyStudy.replayCount(Phase.CONFIRM, 2));
    }

    @Test
    void screenAndConfirmationKeepExactlyTheSamePermanentBuildsAndOnlyChangePairCounts() {
        final var screen = Step193AnomalyDifficultyStudy.cases(Phase.SCREEN);
        final var confirm = Step193AnomalyDifficultyStudy.cases(Phase.CONFIRM);
        for (int index = 0; index < screen.size(); index++) {
            Assertions.assertEquals(screen.get(index).caseId(), confirm.get(index).caseId());
            Assertions.assertEquals(screen.get(index).cell(), confirm.get(index).cell());
            Assertions.assertEquals(Step192AnomalyFormationStudy.canonicalTree(screen.get(index).members()),
                Step192AnomalyFormationStudy.canonicalTree(confirm.get(index).members()));
        }
    }
}
