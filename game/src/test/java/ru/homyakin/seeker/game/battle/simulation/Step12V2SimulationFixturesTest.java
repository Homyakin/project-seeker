package ru.homyakin.seeker.game.battle.simulation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

class Step12V2SimulationFixturesTest {
    private static final Map<V2Build, ActiveEnum> EXPECTED_SKILLS = Map.ofEntries(
        Map.entry(V2Build.BRUISER_BLUNT, ActiveEnum.BERSERK),
        Map.entry(V2Build.GUARDIAN_BLUNT, ActiveEnum.GUARD),
        Map.entry(V2Build.SKIRMISHER_SLASH, ActiveEnum.HIT_AND_RUN),
        Map.entry(V2Build.BREAKER_CLOSE_SLASH, ActiveEnum.ACCUMULATION),
        Map.entry(V2Build.ASSASSIN_PIERCE, ActiveEnum.PENETRATION),
        Map.entry(V2Build.RANGER_PIERCE, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(V2Build.RANGER_MAGICAL, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(V2Build.BRUISER_MAGICAL_CLOTH, ActiveEnum.BERSERK),
        Map.entry(V2Build.BRUISER_MAGICAL_LEATHER, ActiveEnum.BERSERK),
        Map.entry(V2Build.BREAKER_MAGICAL_LEATHER, ActiveEnum.ACCUMULATION),
        Map.entry(V2Build.TACTICIAN_MAGICAL, ActiveEnum.TEMPO_BREAK)
    );
    private static final Map<V3Build, ActiveEnum> EXPECTED_V3_SKILLS = Map.ofEntries(
        Map.entry(V3Build.BRUISER_BLUNT, ActiveEnum.BERSERK),
        Map.entry(V3Build.GUARDIAN_BLUNT, ActiveEnum.GUARD),
        Map.entry(V3Build.SKIRMISHER_SLASH, ActiveEnum.HIT_AND_RUN),
        Map.entry(V3Build.BREAKER_CLOSE_SLASH, ActiveEnum.ACCUMULATION),
        Map.entry(V3Build.ASSASSIN_PIERCE, ActiveEnum.PENETRATION),
        Map.entry(V3Build.RANGER_PIERCE, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(V3Build.RANGER_MAGICAL, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(V3Build.BRUISER_MAGICAL_CLOTH, ActiveEnum.BERSERK),
        Map.entry(V3Build.BRUISER_MAGICAL_LEATHER, ActiveEnum.BERSERK),
        Map.entry(V3Build.BREAKER_MAGICAL_LEATHER, ActiveEnum.ACCUMULATION),
        Map.entry(V3Build.TACTICIAN_MAGICAL, ActiveEnum.TEMPO_BREAK)
    );

    @Test
    void v2BuildsUseExactCatalogItemsAndPreregisteredLevelZeroCharacteristics() {
        final Map<V2Build, ExpectedStats> expected = new EnumMap<>(V2Build.class);
        expected.put(V2Build.SUPPORT, stats(
            2_220,
            Map.of(DefenseType.LEATHER, 480, DefenseType.ARCANE, 120),
            AttackType.MAGICAL,
            240, 360, 0, 0,
            10, 13, 1.44, 140, 2, 92
        ));
        expected.put(V2Build.CROSSBOW_SUPPORT, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.PIERCE,
            420, 420, 420, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V2Build.BRUISER_BLUNT, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.BLUNT,
            480, 0, 0, 0,
            7, 10, 1.36, 156, 18, 85
        ));
        expected.put(V2Build.GUARDIAN_BLUNT, stats(
            1_800,
            Map.of(DefenseType.CLOTH, 360, DefenseType.PLATE, 120),
            AttackType.BLUNT,
            360, 0, 0, 0,
            6, 10, 1.34, 156, 20, 85
        ));
        expected.put(V2Build.SKIRMISHER_SLASH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.SLASH,
            480, 0, 0, 0,
            7, 12, 1.37, 162, 11, 85
        ));
        expected.put(V2Build.BREAKER_CLOSE_SLASH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.SLASH,
            480, 0, 0, 0,
            7, 12, 1.37, 162, 11, 85
        ));
        expected.put(V2Build.ASSASSIN_PIERCE, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.PIERCE,
            420, 300, 0, 0,
            14, 13, 1.49, 170, 4, 87
        ));
        expected.put(V2Build.RANGER_PIERCE, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.PIERCE,
            360, 360, 480, 480,
            8, 11, 1.39, 156, 4, 87
        ));
        expected.put(V2Build.RANGER_MAGICAL, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            360, 360, 480, 480,
            8, 11, 1.39, 156, 4, 87
        ));
        expected.put(V2Build.BRUISER_MAGICAL_CLOTH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            360, 360, 0, 0,
            9, 11, 1.44, 160, 3, 92
        ));
        expected.put(V2Build.BRUISER_MAGICAL_LEATHER, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.MAGICAL,
            360, 360, 0, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V2Build.BREAKER_MAGICAL_LEATHER, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.MAGICAL,
            420, 420, 420, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V2Build.TACTICIAN_MAGICAL, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            360, 360, 0, 0,
            9, 11, 1.44, 160, 3, 92
        ));

        Assertions.assertEquals(13, V2Build.values().length);
        Assertions.assertEquals(11, EXPECTED_SKILLS.size());
        Assertions.assertEquals(expected.keySet(), SetHelper.enumSet(V2Build.values()));
        for (final var entry : expected.entrySet()) {
            final var build = entry.getKey();
            final var personage = Step12SimulationFixtures.v2Personage(build, 0);
            final var items = Step12SimulationFixtures.v2Items(build, 0);
            final var modified = items.stream().filter(item -> item.modifier().isPresent()).toList();

            assertStats(build, personage, entry.getValue());
            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertTrue(items.stream().allMatch(item -> item.enhanceLevel() == 0)),
                () -> Assertions.assertTrue(items.stream().allMatch(item ->
                    item.object().progressionVersion() == ItemProgressionVersion.V1
                )),
                () -> Assertions.assertEquals(build.position(), personage.startPosition()),
                () -> Assertions.assertEquals(build.targetingTactic(), personage.targetingTactic()),
                () -> Assertions.assertEquals(build.displayName(), personage.name().orElseThrow())
            );
            assertSkill(build, personage, modified.size());
        }
    }

    @Test
    void v3CandidateBuildsUseExactCatalogItemsAndLevelZeroCharacteristics() {
        final Map<V3Build, ExpectedStats> expected = new EnumMap<>(V3Build.class);
        expected.put(V3Build.SUPPORT, stats(
            2_220,
            Map.of(DefenseType.LEATHER, 480, DefenseType.ARCANE, 120),
            AttackType.MAGICAL,
            240, 360, 0, 0,
            10, 13, 1.44, 140, 2, 92
        ));
        expected.put(V3Build.CROSSBOW_SUPPORT, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.PIERCE,
            360, 360, 360, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V3Build.BRUISER_BLUNT, stats(
            1_680,
            Map.of(DefenseType.CLOTH, 120, DefenseType.LEATHER, 360),
            AttackType.BLUNT,
            480, 0, 0, 0,
            8, 12, 1.38, 140, 18, 85
        ));
        expected.put(V3Build.GUARDIAN_BLUNT, stats(
            1_800,
            Map.of(DefenseType.CLOTH, 360, DefenseType.PLATE, 120),
            AttackType.BLUNT,
            360, 0, 0, 0,
            6, 10, 1.34, 156, 20, 85
        ));
        expected.put(V3Build.SKIRMISHER_SLASH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.SLASH,
            420, 0, 0, 0,
            7, 12, 1.37, 168, 11, 85
        ));
        expected.put(V3Build.BREAKER_CLOSE_SLASH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.SLASH,
            480, 0, 0, 0,
            7, 12, 1.37, 162, 11, 85
        ));
        expected.put(V3Build.ASSASSIN_PIERCE, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.PIERCE,
            480, 360, 0, 0,
            14, 13, 1.49, 170, 4, 87
        ));
        expected.put(V3Build.RANGER_PIERCE, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.PIERCE,
            240, 240, 360, 360,
            8, 11, 1.39, 156, 4, 87
        ));
        expected.put(V3Build.RANGER_MAGICAL, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            240, 240, 360, 360,
            8, 11, 1.39, 156, 4, 87
        ));
        expected.put(V3Build.BRUISER_MAGICAL_CLOTH, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            360, 360, 0, 0,
            9, 11, 1.44, 160, 3, 92
        ));
        expected.put(V3Build.BRUISER_MAGICAL_LEATHER, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.MAGICAL,
            360, 360, 0, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V3Build.BREAKER_MAGICAL_LEATHER, stats(
            1_920, Map.of(DefenseType.LEATHER, 480), AttackType.MAGICAL,
            360, 360, 360, 0,
            10, 13, 1.46, 134, 3, 92
        ));
        expected.put(V3Build.TACTICIAN_MAGICAL, stats(
            1_380, Map.of(DefenseType.CLOTH, 360), AttackType.MAGICAL,
            480, 480, 0, 0,
            9, 11, 1.44, 156, 3, 92
        ));
        final Map<V3Build, List<String>> changedItems = Map.of(
            V3Build.BRUISER_BLUNT,
            List.of("sledgehammer", "breastplate", "leather_chausses", "boots", "hood", "cloth_gloves"),
            V3Build.SKIRMISHER_SLASH,
            List.of("sword", "shortsword", "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"),
            V3Build.TACTICIAN_MAGICAL,
            List.of("command_staff", "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves")
        );

        Assertions.assertEquals(13, V3Build.values().length);
        Assertions.assertEquals(11, EXPECTED_V3_SKILLS.size());
        Assertions.assertEquals(expected.keySet(), SetHelper.enumSet(V3Build.values()));
        for (final var entry : expected.entrySet()) {
            final var build = entry.getKey();
            final var personage = Step12SimulationFixtures.v3Personage(build, 0);
            final var items = Step12SimulationFixtures.v3Items(build, 0);
            final var modified = items.stream().filter(item -> item.modifier().isPresent()).toList();

            assertStats(build, personage, entry.getValue());
            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertTrue(items.stream().allMatch(item -> item.enhanceLevel() == 0)),
                () -> Assertions.assertTrue(items.stream().allMatch(item ->
                    item.object().progressionVersion() == ItemProgressionVersion.V1
                )),
                () -> Assertions.assertEquals(build.position(), personage.startPosition()),
                () -> Assertions.assertEquals(build.targetingTactic(), personage.targetingTactic()),
                () -> Assertions.assertEquals(build.displayName(), personage.name().orElseThrow())
            );
            if (changedItems.containsKey(build)) {
                Assertions.assertEquals(
                    changedItems.get(build),
                    items.stream().map(item -> item.object().code()).toList(),
                    build.name()
                );
            }
            assertV3Skill(build, personage, modified.size());
        }
    }

    @Test
    void v3CandidateBuildsFollowApprovedProgressionAtEveryControlLevel() {
        for (final var build : V3Build.values()) {
            final var baseline = Step12SimulationFixtures.v3Personage(build, 0);
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var actual = Step12SimulationFixtures.v3Personage(build, level);
                Assertions.assertAll(
                    build.name() + "/+" + level,
                    () -> Assertions.assertEquals(
                        ItemProgression.valueAtLevel(baseline.maxHealth(), level),
                        actual.maxHealth()
                    ),
                    () -> Assertions.assertEquals(
                        scaleDefenses(baseline.defenses(), level),
                        actual.defenses()
                    ),
                    () -> Assertions.assertEquals(baseline.range(), actual.range()),
                    () -> Assertions.assertEquals(baseline.critChance(), actual.critChance()),
                    () -> Assertions.assertEquals(baseline.dodgeChance(), actual.dodgeChance()),
                    () -> Assertions.assertEquals(baseline.critMultiplier(), actual.critMultiplier()),
                    () -> Assertions.assertEquals(baseline.initiative(), actual.initiative()),
                    () -> Assertions.assertEquals(baseline.totalThreat(), actual.totalThreat()),
                    () -> Assertions.assertEquals(baseline.impactStrength(), actual.impactStrength()),
                    () -> Assertions.assertTrue(actual.skillSnapshots().stream().allMatch(snapshot ->
                        snapshot.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V2
                    ))
                );
                for (int distance = 1; distance <= baseline.range(); distance++) {
                    Assertions.assertEquals(
                        scaleAttacks(baseline.attackAtRange(distance), level),
                        actual.attackAtRange(distance),
                        build.name() + "/+" + level + "/distance=" + distance
                    );
                }
            }
        }
    }

    @Test
    void v2BuildWithoutModifierKeepsEveryItemAndConstantCharacteristic() {
        for (final var build : EXPECTED_SKILLS.keySet()) {
            final int level = 10;
            final var enabledItems = Step12SimulationFixtures.v2Items(build, level);
            final var disabledItems = Step12SimulationFixtures.v2ItemsWithoutModifier(build, level);
            final var enabled = Step12SimulationFixtures.v2Personage(build, level);
            final var disabled = Step12SimulationFixtures.v2PersonageWithoutModifier(build, level);

            Assertions.assertEquals(enabledItems.size(), disabledItems.size());
            for (int index = 0; index < enabledItems.size(); index++) {
                final var enabledItem = enabledItems.get(index);
                final var disabledItem = disabledItems.get(index);
                Assertions.assertAll(
                    build.name() + " item " + index,
                    () -> Assertions.assertSame(enabledItem.object(), disabledItem.object()),
                    () -> Assertions.assertEquals(enabledItem.rarity(), disabledItem.rarity()),
                    () -> Assertions.assertEquals(enabledItem.enhanceLevel(), disabledItem.enhanceLevel())
                );
            }
            Assertions.assertEquals(1, enabledItems.stream().filter(item -> item.modifier().isPresent()).count());
            Assertions.assertTrue(disabledItems.stream().allMatch(item -> item.modifier().isEmpty()));
            Assertions.assertTrue(disabled.skillSnapshots().isEmpty());
            Assertions.assertEquals(enabled.maxHealth(), disabled.maxHealth());
            Assertions.assertEquals(enabled.defenses(), disabled.defenses());
            Assertions.assertEquals(enabled.attacksByRange(), disabled.attacksByRange());
            Assertions.assertEquals(enabled.critChance(), disabled.critChance());
            Assertions.assertEquals(enabled.dodgeChance(), disabled.dodgeChance());
            Assertions.assertEquals(enabled.critMultiplier(), disabled.critMultiplier());
            Assertions.assertEquals(enabled.initiative(), disabled.initiative());
            Assertions.assertEquals(enabled.totalThreat(), disabled.totalThreat());
            Assertions.assertEquals(enabled.impactStrength(), disabled.impactStrength());
        }
    }

    @Test
    void v2MatchupCanDisableBothRoleModifiersWithoutChangingConstantCharacteristics() {
        final var active = Step12SimulationFixtures.v2Teams(V2Matchup.TACTICIAN_V2, 10);
        final var disabled = Step12SimulationFixtures.v2Teams(
            V2Matchup.TACTICIAN_V2,
            10,
            Set.of(V2Build.TACTICIAN_MAGICAL, V2Build.BRUISER_MAGICAL_CLOTH)
        );

        assertOnlySkillsChanged(active.evaluatedTeam(), disabled.evaluatedTeam());
        assertOnlySkillsChanged(active.opponents(), disabled.opponents());
    }

    @Test
    void v2MatchupsHaveExactCompositionsAndNoAutomaticInitialApproach() {
        final Map<V2Matchup, ExpectedComposition> expected = new EnumMap<>(V2Matchup.class);
        expected.put(V2Matchup.BRUISER_V2, composition(
            "ОПОРА@FRONT|Громила, дробящий@FRONT|ОПОРА@MID",
            "ОПОРА@FRONT|Страж, дробящий@FRONT|ОПОРА@MID"
        ));
        expected.put(V2Matchup.SKIRMISHER_V2, composition(
            "Застрельщик, рубящий@FRONT|ОПОРА-АРБАЛЕТ@MID|ОПОРА-АРБАЛЕТ@MID",
            "Разрушитель, ближний рубящий@FRONT|ОПОРА-АРБАЛЕТ@MID|ОПОРА-АРБАЛЕТ@MID"
        ));
        expected.put(V2Matchup.ASSASSIN_V2, composition(
            "ОПОРА@FRONT|Убийца, колющий@FRONT|ОПОРА@MID",
            "ОПОРА@FRONT|ОПОРА@MID|Дальнобоец, колющий@BACK"
        ));
        expected.put(V2Matchup.RANGER_V2, composition(
            "ОПОРА@FRONT|ОПОРА@MID|Дальнобоец, магический@BACK",
            "ОПОРА@FRONT|Громила, магический в ткани@FRONT|ОПОРА@MID"
        ));
        expected.put(V2Matchup.BREAKER_V2, composition(
            "ОПОРА@FRONT|ОПОРА@MID|Разрушитель, магический в коже@BACK",
            "ОПОРА@FRONT|Громила, магический в коже@FRONT|ОПОРА@MID"
        ));
        expected.put(V2Matchup.TACTICIAN_V2, composition(
            "Тактик, магический@FRONT|ОПОРА-АРБАЛЕТ@BACK|ОПОРА-АРБАЛЕТ@BACK",
            "Громила, магический в ткани@FRONT|ОПОРА-АРБАЛЕТ@BACK|ОПОРА-АРБАЛЕТ@BACK"
        ));

        Assertions.assertEquals(6, V2Matchup.values().length);
        Assertions.assertEquals(expected.keySet(), SetHelper.enumSet(V2Matchup.values()));
        for (final var entry : expected.entrySet()) {
            final var matchup = entry.getKey();
            final var teams = Step12SimulationFixtures.v2Teams(matchup, 0);
            final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents());

            Assertions.assertAll(
                matchup.code(),
                () -> Assertions.assertTrue(matchup.code().endsWith("_V2")),
                () -> Assertions.assertEquals(3, teams.evaluatedTeam().size()),
                () -> Assertions.assertEquals(3, teams.opponents().size()),
                () -> Assertions.assertEquals(entry.getValue().evaluated(), labels(teams.evaluatedTeam())),
                () -> Assertions.assertEquals(entry.getValue().opponents(), labels(teams.opponents())),
                () -> Assertions.assertEquals(6, context.lines().size())
            );
            assertActualLines(context, teams.evaluatedTeam(), matchup.evaluatedTeam());
            assertActualLines(context, teams.opponents(), matchup.opponents());
        }
    }

    @Test
    void v2GeometryCreatesTheThreeDeclaredRoleInteractions() {
        assertSkirmisherRetreatGeometry();
        assertAssassinUniqueFarTarget();
        assertTacticianUniqueNearbyRoleTarget();
    }

    private static void assertSkirmisherRetreatGeometry() {
        final var teams = Step12SimulationFixtures.v2Teams(V2Matchup.SKIRMISHER_V2, 0);
        final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents());
        final var skirmisher = named(teams.evaluatedTeam(), "Застрельщик, рубящий");
        final var breaker = named(teams.opponents(), "Разрушитель, ближний рубящий");

        context.moveBackward(skirmisher, 1);

        Assertions.assertEquals(Position.MID, context.lines().get(skirmisher.currentPosition()).position());
        Assertions.assertEquals(1, breaker.range());
        Assertions.assertTrue(teams.evaluatedTeam().stream().allMatch(target -> distance(breaker, target) == 2));
    }

    private static void assertAssassinUniqueFarTarget() {
        final var teams = Step12SimulationFixtures.v2Teams(V2Matchup.ASSASSIN_V2, 0);
        final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents());
        final var assassin = named(teams.evaluatedTeam(), "Убийца, колющий");
        final var ranger = named(teams.opponents(), "Дальнобоец, колющий");
        final var outsideOrdinaryRange = teams.opponents().stream()
            .filter(target -> distance(assassin, target) > assassin.range())
            .toList();

        Assertions.assertEquals(6, context.lines().size());
        Assertions.assertEquals(2, assassin.range());
        Assertions.assertEquals(3, distance(assassin, ranger));
        Assertions.assertEquals(List.of(ranger), outsideOrdinaryRange);
    }

    private static void assertTacticianUniqueNearbyRoleTarget() {
        final var teams = Step12SimulationFixtures.v2Teams(V2Matchup.TACTICIAN_V2, 0);
        final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents());
        final var tactician = named(teams.evaluatedTeam(), "Тактик, магический");
        final var bruiser = named(teams.opponents(), "Громила, магический в ткани");

        Assertions.assertEquals(6, context.lines().size());
        Assertions.assertEquals(1, distance(tactician, bruiser));
        Assertions.assertTrue(teams.opponents().stream()
            .filter(target -> target != bruiser)
            .allMatch(target -> distance(tactician, target) > tactician.range()));
        Assertions.assertTrue(teams.evaluatedTeam().stream()
            .filter(target -> target != tactician)
            .allMatch(target -> distance(bruiser, target) > bruiser.range()));
    }

    private static void assertSkill(V2Build build, BattlePersonage personage, int modifiedItems) {
        final var expectedSkill = EXPECTED_SKILLS.get(build);
        if (expectedSkill == null) {
            Assertions.assertEquals(0, modifiedItems, build.name());
            Assertions.assertTrue(personage.skillSnapshots().isEmpty(), build.name());
            return;
        }
        Assertions.assertEquals(1, modifiedItems, build.name());
        Assertions.assertEquals(1, personage.skillSnapshots().size(), build.name());
        final var snapshot = personage.skillSnapshots().getFirst();
        Assertions.assertEquals(expectedSkill, snapshot.code(), build.name());
        Assertions.assertEquals(4, snapshot.points(), build.name());
        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V1, snapshot.formulaVersion(), build.name());
    }

    private static void assertV3Skill(V3Build build, BattlePersonage personage, int modifiedItems) {
        final var expectedSkill = EXPECTED_V3_SKILLS.get(build);
        if (expectedSkill == null) {
            Assertions.assertEquals(0, modifiedItems, build.name());
            Assertions.assertTrue(personage.skillSnapshots().isEmpty(), build.name());
            return;
        }
        Assertions.assertEquals(1, modifiedItems, build.name());
        Assertions.assertEquals(1, personage.skillSnapshots().size(), build.name());
        final var snapshot = personage.skillSnapshots().getFirst();
        Assertions.assertEquals(expectedSkill, snapshot.code(), build.name());
        Assertions.assertEquals(4, snapshot.points(), build.name());
        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V2, snapshot.formulaVersion(), build.name());
    }

    private static void assertOnlySkillsChanged(
        List<BattlePersonage> active,
        List<BattlePersonage> disabled
    ) {
        Assertions.assertEquals(active.size(), disabled.size());
        for (int index = 0; index < active.size(); index++) {
            final var enabledPersonage = active.get(index);
            final var disabledPersonage = disabled.get(index);
            Assertions.assertAll(
                enabledPersonage.name().orElseThrow(),
                () -> Assertions.assertEquals(enabledPersonage.maxHealth(), disabledPersonage.maxHealth()),
                () -> Assertions.assertEquals(enabledPersonage.defenses(), disabledPersonage.defenses()),
                () -> Assertions.assertEquals(enabledPersonage.attacksByRange(), disabledPersonage.attacksByRange()),
                () -> Assertions.assertEquals(enabledPersonage.critChance(), disabledPersonage.critChance()),
                () -> Assertions.assertEquals(enabledPersonage.dodgeChance(), disabledPersonage.dodgeChance()),
                () -> Assertions.assertEquals(enabledPersonage.critMultiplier(), disabledPersonage.critMultiplier()),
                () -> Assertions.assertEquals(enabledPersonage.initiative(), disabledPersonage.initiative()),
                () -> Assertions.assertEquals(enabledPersonage.totalThreat(), disabledPersonage.totalThreat()),
                () -> Assertions.assertEquals(enabledPersonage.impactStrength(), disabledPersonage.impactStrength())
            );
        }
        Assertions.assertTrue(active.stream().mapToInt(personage -> personage.skillSnapshots().size()).sum() > 0);
        Assertions.assertTrue(disabled.stream().allMatch(personage -> personage.skillSnapshots().isEmpty()));
    }

    private static void assertStats(V2Build build, BattlePersonage personage, ExpectedStats expected) {
        assertStats(build.name(), personage, expected);
    }

    private static void assertStats(V3Build build, BattlePersonage personage, ExpectedStats expected) {
        assertStats(build.name(), personage, expected);
    }

    private static void assertStats(String label, BattlePersonage personage, ExpectedStats expected) {
        Assertions.assertAll(
            label,
            () -> Assertions.assertEquals(expected.health(), personage.maxHealth()),
            () -> Assertions.assertEquals(expected.defenses(), personage.defenses()),
            () -> Assertions.assertEquals(
                expected.attackByRange(),
                attackByRange(personage, expected.attackType())
            ),
            () -> Assertions.assertEquals(expected.critChance(), personage.critChance()),
            () -> Assertions.assertEquals(expected.dodgeChance(), personage.dodgeChance()),
            () -> Assertions.assertEquals(expected.critMultiplier(), personage.critMultiplier(), 1e-9),
            () -> Assertions.assertEquals(expected.speed(), personage.initiative()),
            () -> Assertions.assertEquals(expected.threat(), personage.totalThreat()),
            () -> Assertions.assertEquals(expected.impact(), personage.impactStrength())
        );
    }

    private static void assertActualLines(
        BattleContext context,
        List<BattlePersonage> team,
        List<Step12SimulationFixtures.V2Placement> placements
    ) {
        for (int index = 0; index < team.size(); index++) {
            final var personage = team.get(index);
            Assertions.assertEquals(
                placements.get(index).position(),
                context.lines().get(personage.currentPosition()).position(),
                placements.get(index).build().displayName()
            );
        }
    }

    private static List<Integer> attackByRange(BattlePersonage personage, AttackType attackType) {
        return List.of(1, 2, 3, 4).stream()
            .map(distance -> distance <= personage.range()
                ? personage.attackAtRange(distance).getOrDefault(attackType, 0)
                : 0)
            .toList();
    }

    private static Map<DefenseType, Integer> scaleDefenses(
        Map<DefenseType, Integer> baseline,
        int level
    ) {
        final var result = new EnumMap<DefenseType, Integer>(DefenseType.class);
        baseline.forEach((type, value) -> result.put(type, ItemProgression.valueAtLevel(value, level)));
        return Map.copyOf(result);
    }

    private static Map<AttackType, Integer> scaleAttacks(
        Map<AttackType, Integer> baseline,
        int level
    ) {
        final var result = new EnumMap<AttackType, Integer>(AttackType.class);
        baseline.forEach((type, value) -> result.put(type, ItemProgression.valueAtLevel(value, level)));
        return Map.copyOf(result);
    }

    private static ExpectedStats stats(
        int health,
        Map<DefenseType, Integer> defenses,
        AttackType attackType,
        int rangeOne,
        int rangeTwo,
        int rangeThree,
        int rangeFour,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int threat,
        int impact
    ) {
        return new ExpectedStats(
            health,
            defenses,
            attackType,
            List.of(rangeOne, rangeTwo, rangeThree, rangeFour),
            critChance,
            dodgeChance,
            critMultiplier,
            speed,
            threat,
            impact
        );
    }

    private static ExpectedComposition composition(String evaluated, String opponents) {
        return new ExpectedComposition(List.of(evaluated.split("\\|")), List.of(opponents.split("\\|")));
    }

    private static List<String> labels(List<BattlePersonage> team) {
        return team.stream()
            .map(personage -> "%s@%s".formatted(
                personage.name().orElseThrow(),
                personage.startPosition()
            ))
            .toList();
    }

    private static BattlePersonage named(List<BattlePersonage> team, String name) {
        return team.stream().filter(personage -> personage.name().orElseThrow().equals(name)).findFirst().orElseThrow();
    }

    private static int distance(BattlePersonage first, BattlePersonage second) {
        return Math.abs(first.currentPosition() - second.currentPosition());
    }

    private record ExpectedStats(
        int health,
        Map<DefenseType, Integer> defenses,
        AttackType attackType,
        List<Integer> attackByRange,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int threat,
        int impact
    ) {
    }

    private record ExpectedComposition(List<String> evaluated, List<String> opponents) {
    }

    private static final class SetHelper {
        private SetHelper() {
        }

        private static <E extends Enum<E>> java.util.Set<E> enumSet(E[] values) {
            return java.util.Set.of(values);
        }
    }
}
