package ru.homyakin.seeker.game.battle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step192AnomalyFormationStudy.Sample;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Matrix, input provenance and pre-battle seed strata only; no simulated battles or application startup. */
class Step192AnomalyFormationStudyTest {
    private static final List<Sample> CASES = Step192AnomalyFormationStudy.cases();

    @TempDir
    private Path isolatedClasses;

    @Test
    void isolatedGeneratorAndItsSyntheticSwitchShareOneLoaderWhileBattleModelsStayShared() throws Exception {
        copyCurrentClass(Step192AnomalyFormationStudy.GENERATOR);
        copyCurrentClass(Step192AnomalyFormationStudy.GENERATOR + "$1");
        try (var loader = new Step192AnomalyFormationStudy.BaselineLoader(isolatedClasses.toUri().toURL())) {
            Assertions.assertSame(loader, loader.loadClass(Step192AnomalyFormationStudy.GENERATOR).getClassLoader());
            Assertions.assertSame(loader, loader.loadClass(Step192AnomalyFormationStudy.GENERATOR + "$1").getClassLoader());
            Assertions.assertSame(BattlePersonage.class, loader.loadClass(BattlePersonage.class.getName()));
            Assertions.assertSame(RandomUtils.class, loader.loadClass(RandomUtils.class.getName()));
            final var generator = Step192AnomalyFormationStudy.baselineGenerator(loader);
            for (final var template : AnomalyPveTemplate.values()) {
                final var sample = CASES.stream().filter(candidate -> candidate.family().equals("A_MAIN")
                    && candidate.cell().encounter().equals(template.name()) && candidate.cell().size() == 3
                    && candidate.cell().tier() == Tier.MID).findFirst().orElseThrow();
                final var generated = RandomUtils.withSeed(15_000L + sample.caseId(), () ->
                    Step192AnomalyFormationStudy.prepare(sample, generator, PveAdaptationStudy.FORMULA));
                Assertions.assertEquals(3, generated.generated().enemies().size());
            }
        }
    }

    @Test
    void missingBaselineSyntheticClassMustFailInsteadOfFallingBackToCurrentCode() throws Exception {
        copyCurrentClass(Step192AnomalyFormationStudy.GENERATOR);
        try (var loader = new Step192AnomalyFormationStudy.BaselineLoader(isolatedClasses.toUri().toURL())) {
            Assertions.assertSame(loader, loader.loadClass(Step192AnomalyFormationStudy.GENERATOR).getClassLoader());
            Assertions.assertThrows(ClassNotFoundException.class,
                () -> loader.loadClass(Step192AnomalyFormationStudy.GENERATOR + "$1"));
            Assertions.assertThrows(ClassNotFoundException.class, () -> Step192AnomalyFormationStudy.baselineGenerator(loader));
        }
    }

    private void copyCurrentClass(String binaryName) throws Exception {
        final var relative = binaryName.replace('.', '/') + ".class";
        final var output = isolatedClasses.resolve(relative);
        Files.createDirectories(output.getParent());
        try (var input = Step192AnomalyFormationStudyTest.class.getClassLoader().getResourceAsStream(relative)) {
            Assertions.assertNotNull(input);
            Files.copy(input, output);
        }
    }

    @Test
    void exactApprovedMatrixUsesOneSharedAnomalyModeNotTwoFakeServiceSamples() {
        Assertions.assertEquals(136, CASES.size());
        Assertions.assertEquals(7840, Step192AnomalyFormationStudy.battleCount(CASES));
        Assertions.assertEquals(Map.of("A_MAIN", 60L, "B_SOLO", 40L, "C_DIRECTIONS", 24L,
            "D_DEFAULTS", 12L), CASES.stream().collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        Assertions.assertEquals(4, CASES.stream().map(sample -> sample.cell().encounter()).distinct().count());
        for (int index = 0; index < CASES.size(); index++) {
            final var sample = CASES.get(index);
            Assertions.assertEquals(index + 1, sample.caseId());
            Assertions.assertEquals(PveAdaptationStudy.Mode.ANOMALY_SAFE, sample.cell().mode());
            Assertions.assertEquals(0, sample.cell().raidLevel());
            Assertions.assertFalse(sample.cell().effects());
            Assertions.assertEquals(sample.cell().size(), sample.members().size());
            Assertions.assertEquals(sample.family().equals("A_MAIN") ? 20 : 10, sample.pairs());
        }
    }

    @Test
    void everyInputReallyUsesTheFifthCatalogNotTheOldBuildHelperCatalog() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5);
        final Map<String, ItemObject> objects = java.util.stream.Stream.concat(catalog.itemObjects().stream(),
            catalog.defaultItems().stream()).collect(Collectors.toMap(ItemObject::code, Function.identity(),
                (first, second) -> first));
        final var modifiers = catalog.modifiers().stream().collect(Collectors.toMap(
            ru.homyakin.seeker.game.item.models.Modifier::activeEnum, Function.identity()));
        for (final var sample : CASES) {
            for (final var member : sample.members()) {
                for (final var item : member.items()) {
                    Assertions.assertEquals(objects.get(item.object().code()), item.object(),
                        sample.family() + ":" + item.object().code());
                    item.modifier().ifPresent(modifier -> Assertions.assertEquals(modifiers.get(modifier.activeEnum()), modifier));
                }
            }
        }
    }

    @Test
    void allTenSoloRolesPreserveTheirOriginalModifierAssignmentsAndUseUnenhancedV5Objects() {
        for (final var template : AnomalyPveTemplate.values()) {
            final var samples = CASES.stream().filter(sample -> sample.family().equals("B_SOLO")
                && sample.cell().encounter().equals(template.name())).toList();
            Assertions.assertEquals(10, samples.size());
            for (int index = 0; index < RoleBuild.values().length; index++) {
                final var role = RoleBuild.values()[index];
                final var build = Step12RepresentativeBuilds.role(role);
                final var sample = samples.get(index);
                final var member = sample.members().getFirst();
                Assertions.assertEquals(role.name(), sample.cell().style());
                Assertions.assertEquals(Tier.MID, sample.cell().tier());
                Assertions.assertEquals(build.code(), member.build());
                Assertions.assertEquals(build.position(), member.position());
                Assertions.assertEquals(build.tactic(), member.tactic());
                Assertions.assertEquals(new Step123BattleRunner.Gear(build, EquipmentCatalogVersion.SCALING_V5)
                    .items(0), member.items());
                Assertions.assertEquals(build.items(0).stream().map(Item::rarity).toList(),
                    member.items().stream().map(Item::rarity).toList());
                Assertions.assertEquals(build.items(0).stream().map(item -> item.modifier().map(modifier -> modifier.activeEnum()))
                    .toList(), member.items().stream().map(item -> item.modifier().map(modifier -> modifier.activeEnum())).toList());
                Assertions.assertTrue(member.items().stream().allMatch(item -> item.enhanceLevel() == 0));
            }
        }
    }

    @Test
    void mainProfilesKeepRealModifierDifferencesButEveryCombatCopyDropsEnhancementAndEffects() {
        for (final var sample : CASES.stream().filter(candidate -> candidate.family().equals("A_MAIN")).toList()) {
            final var originals = Step192AnomalyFormationStudy.canonicalTree(sample.members());
            final var prepared = RandomUtils.withSeed(12_000L + sample.caseId(), () ->
                Step192AnomalyFormationStudy.prepare(sample, Step192AnomalyFormationStudy.currentGenerator(),
                    PveAdaptationStudy.FORMULA));
            Assertions.assertEquals(originals, Step192AnomalyFormationStudy.canonicalTree(sample.members()));
            for (int index = 0; index < sample.members().size(); index++) {
                final var member = prepared.members().get(index);
                Assertions.assertEquals(sample.members().get(index).items(), member.sourceItems());
                Assertions.assertEquals(member.sourceItems().stream().map(Item::withoutStormEnhance).toList(), member.combatItems());
                Assertions.assertTrue(member.combatItems().stream().allMatch(item -> item.enhanceLevel() == 0));
                final var actor = prepared.players().get(index);
                final var expected = BattlePersonage.forCombat(member.combatItems(), member.position(),
                    PersonageEffects.EMPTY, java.util.Optional.of(member.build()), PveAdaptationStudy.FORMULA);
                Assertions.assertEquals(expected.health(), actor.health());
                Assertions.assertEquals(expected.attackAtRange(1), actor.attackAtRange(1));
                Assertions.assertEquals(expected.defenses(), actor.defenses());
                Assertions.assertEquals(expected.legacyPower(), actor.legacyPower());
            }
        }
        final var totals = CASES.stream().filter(sample -> sample.family().equals("A_MAIN") && sample.cell().size() == 3
            && sample.cell().encounter().equals(AnomalyPveTemplate.ASH_WIND.name())).collect(Collectors.toMap(
                sample -> sample.cell().tier(), sample -> sample.members().stream().flatMap(member -> member.items().stream())
                    .mapToInt(Item::skillPoints).sum()));
        Assertions.assertTrue(totals.get(Tier.BEGINNER) < totals.get(Tier.MID));
        Assertions.assertTrue(totals.get(Tier.MID) < totals.get(Tier.STRONG));
    }

    @Test
    void n2StrataAreSelectedThroughActualGenerationBeforeAnyOutcomesAndKeepDistinctSharedRoots() {
        final var samples = CASES.stream().filter(sample -> sample.family().equals("A_MAIN")
            && sample.cell().size() == 2).toList();
        Assertions.assertEquals(12, samples.size());
        final var plans = Step192AnomalyFormationStudy.plans(samples, Step192AnomalyFormationStudy.currentGenerator());
        for (final var sample : samples) {
            final var seeds = plans.get(sample.caseId());
            Assertions.assertEquals(20, seeds.size());
            Assertions.assertEquals(20, seeds.stream().map(Step192AnomalyFormationStudy.Seed::generationSeed).distinct().count());
            for (int pair = 0; pair < seeds.size(); pair++) {
                final var seed = seeds.get(pair);
                Assertions.assertEquals(pair, seed.pair());
                Assertions.assertEquals(pair < 10 ? 2 : 3, seed.expectedEnemyCount());
                Assertions.assertEquals(30_261_129_000_000L + sample.caseId() * 1000L + pair, seed.battleSeed());
                Assertions.assertTrue(seed.generationSeed() >= 20_261_129_000_000L + sample.caseId() * 1000L);
                final var generated = RandomUtils.withSeed(seed.generationSeed(), () -> Step192AnomalyFormationStudy.prepare(
                    sample, Step192AnomalyFormationStudy.currentGenerator(), PveAdaptationStudy.FORMULA));
                Assertions.assertEquals(seed.expectedEnemyCount(), generated.generated().enemies().size());
            }
        }
        Assertions.assertEquals(plans, Step192AnomalyFormationStudy.plans(samples, Step192AnomalyFormationStudy.currentGenerator()));
    }

    @Test
    void replayFixesEveryTemplateAndSizeAndBothN2BranchesWithoutIncreasingSampleSize() {
        int pairs = 0;
        for (final var template : AnomalyPveTemplate.values()) {
            for (int size = 1; size <= 5; size++) {
                final int expectedSize = size;
                final var sample = CASES.stream().filter(candidate -> candidate.family().equals("A_MAIN")
                    && candidate.cell().tier() == Tier.MID && candidate.cell().size() == expectedSize
                    && candidate.cell().encounter().equals(template.name())).findFirst().orElseThrow();
                for (int pair = 0; pair < sample.pairs(); pair++) {
                    Assertions.assertEquals(pair == 0 || size == 2 && pair == 10,
                        Step192AnomalyFormationStudy.replaySelected(sample, pair));
                    if (Step192AnomalyFormationStudy.replaySelected(sample, pair)) {
                        ++pairs;
                    }
                }
            }
        }
        Assertions.assertEquals(24, pairs);
        Assertions.assertEquals(96, pairs * 4);
        Assertions.assertEquals(7840, Step192AnomalyFormationStudy.battleCount(CASES));
    }

    @Test
    void durationDistributionUsesNearestRankAndSeparatesNoCompletedObservations() {
        Assertions.assertEquals(Map.of("count", 0), Step192AnomalyFormationStudy.distribution(List.of()));
        final var distribution = Step192AnomalyFormationStudy.distribution(List.of(4.0, 1.0, 2.0, 3.0));
        Assertions.assertEquals(4, distribution.get("count"));
        Assertions.assertEquals(2.5, distribution.get("mean"));
        Assertions.assertEquals(2.0, distribution.get("p50"));
        Assertions.assertEquals(4.0, distribution.get("p95"));
    }
}
