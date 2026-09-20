package ru.homyakin.seeker.game.battle.simulation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

class Step12MixedBuildFixturesTest {
    private static final Map<MixedBuild, ExpectedBuild> EXPECTED_BUILDS = Map.of(
        MixedBuild.RAM,
        build(
            "Таран", Position.FRONT, TargetingTactic.WOUNDED_HUNTER,
            "mace", "tower_shield",
            "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
        ),
        MixedBuild.KEEPER,
        build(
            "Хранитель", Position.FRONT, TargetingTactic.INITIATIVE_INTERCEPTION,
            "spear", "tower_shield",
            "cuirass", "greaves", "sabatons", "hood", "cloth_gloves"
        ),
        MixedBuild.DUELIST,
        build(
            "Дуэлянт", Position.MID, TargetingTactic.CHALLENGE_THE_AGILE,
            "sword", "dirk",
            "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
        ),
        MixedBuild.RAIDER,
        build(
            "Налётчик", Position.FRONT, TargetingTactic.EXECUTIONER,
            "rapier", "dirk",
            "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
        ),
        MixedBuild.FORMATION_BREAKER,
        build(
            "Ломатель строя", Position.MID, TargetingTactic.INITIATIVE_INTERCEPTION,
            "spear", "club",
            "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
        ),
        MixedBuild.ARTILLERIST,
        build(
            "Артиллерист", Position.BACK, TargetingTactic.EXECUTIONER,
            "bow",
            "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
        ),
        MixedBuild.BLOCKER,
        build(
            "Заградитель", Position.BACK, TargetingTactic.EXPLOIT_WEAKNESS,
            "bow",
            "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
        )
    );
    private static final Map<MixedBuild, List<ExpectedModifier>> EXPECTED_MODIFIERS = Map.of(
        MixedBuild.RAM,
        List.of(modifier("mace", "furious", ActiveEnum.BERSERK, 2),
            modifier("tower_shield", "guarding", ActiveEnum.GUARD, 2)),
        MixedBuild.KEEPER,
        List.of(modifier("spear", "disrupting", ActiveEnum.TEMPO_BREAK, 2),
            modifier("tower_shield", "guarding", ActiveEnum.GUARD, 2)),
        MixedBuild.DUELIST,
        List.of(modifier("sword", "furious", ActiveEnum.BERSERK, 2),
            modifier("boots", "swift", ActiveEnum.HIT_AND_RUN, 2)),
        MixedBuild.RAIDER,
        List.of(modifier("rapier", "penetrating", ActiveEnum.PENETRATION, 2),
            modifier("hood", "swift", ActiveEnum.HIT_AND_RUN, 2)),
        MixedBuild.FORMATION_BREAKER,
        List.of(modifier("spear", "disrupting", ActiveEnum.TEMPO_BREAK, 2),
            modifier("club", "furious", ActiveEnum.BERSERK, 2)),
        MixedBuild.ARTILLERIST,
        List.of(modifier("bow", "charging", ActiveEnum.ACCUMULATION, 4)),
        MixedBuild.BLOCKER,
        List.of(modifier("bow", "knocking", ActiveEnum.KNOCKBACK, 4))
    );
    private static final Map<MixedBuild, AttackType> EXPECTED_PRIMARY_ATTACK_TYPES = Map.of(
        MixedBuild.RAM, AttackType.BLUNT,
        MixedBuild.KEEPER, AttackType.PIERCE,
        MixedBuild.DUELIST, AttackType.SLASH,
        MixedBuild.RAIDER, AttackType.PIERCE,
        MixedBuild.FORMATION_BREAKER, AttackType.PIERCE,
        MixedBuild.ARTILLERIST, AttackType.PIERCE,
        MixedBuild.BLOCKER, AttackType.PIERCE
    );

    @Test
    void mixedBuildsUseTheExactDeclaredCatalogItemsPositionsAndTactics() {
        Assertions.assertEquals(7, MixedBuild.values().length);
        Assertions.assertEquals(EXPECTED_BUILDS.keySet(), Set.of(MixedBuild.values()));

        for (final var entry : EXPECTED_BUILDS.entrySet()) {
            final var build = entry.getKey();
            final var expected = entry.getValue();
            final var items = Step12SimulationFixtures.items(build, 6);
            final var personage = Step12SimulationFixtures.personage(build, 6);

            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertEquals(expected.itemCodes(), itemCodes(items)),
                () -> Assertions.assertTrue(items.stream().allMatch(item -> item.enhanceLevel() == 6)),
                () -> Assertions.assertTrue(items.stream().allMatch(item ->
                    item.object().progressionVersion() == ItemProgressionVersion.V1
                )),
                () -> Assertions.assertEquals(expected.displayName(), personage.name().orElseThrow()),
                () -> Assertions.assertEquals(expected.position(), personage.startPosition()),
                () -> Assertions.assertEquals(expected.tactic(), personage.targetingTactic())
            );
        }
    }

    @Test
    void modifierCarriersProduceTheExactFourPointSkillSets() {
        for (final var entry : EXPECTED_MODIFIERS.entrySet()) {
            final var build = entry.getKey();
            final var items = Step12SimulationFixtures.items(build, 0);
            final var personage = Step12SimulationFixtures.personage(build, 0);
            final var modifiedItems = items.stream().filter(item -> item.modifier().isPresent()).toList();
            final var expectedSkills = entry.getValue().stream().collect(Collectors.toMap(
                ExpectedModifier::skill,
                ExpectedModifier::points
            ));

            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertEquals(entry.getValue(), modifiers(modifiedItems)),
                () -> Assertions.assertEquals(4, modifiedItems.stream().mapToInt(Item::skillPoints).sum()),
                () -> Assertions.assertEquals(expectedSkills, skills(personage)),
                () -> Assertions.assertTrue(personage.skillSnapshots().stream().allMatch(snapshot ->
                    snapshot.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V2
                ))
            );
        }
    }

    @Test
    void equipmentDerivesTheDeclaredPrimaryAttackTypeWithoutAStoredRoleLabel() {
        for (final var entry : EXPECTED_PRIMARY_ATTACK_TYPES.entrySet()) {
            final var items = Step12SimulationFixtures.items(entry.getKey(), 0);
            Assertions.assertEquals(entry.getValue(), primaryAttackType(items), entry.getKey().name());
        }
    }

    @Test
    void splitMechanicsKeepTheirDeterministicCombinationPrice() {
        final var guardian = Step12SimulationFixtures.personage(RoleBuild.GUARDIAN, 0);
        final var bruiser = Step12SimulationFixtures.personage(RoleBuild.BRUISER, 0);
        final var skirmisher = Step12SimulationFixtures.personage(RoleBuild.SKIRMISHER, 0);
        final var assassin = Step12SimulationFixtures.personage(RoleBuild.ASSASSIN, 0);
        final var ranger = Step12SimulationFixtures.personage(RoleBuild.RANGER_PHYSICAL, 0);
        final var tactician = Step12SimulationFixtures.personage(RoleBuild.TACTICIAN_PHYSICAL, 0);
        final var ram = Step12SimulationFixtures.personage(MixedBuild.RAM, 0);
        final var keeper = Step12SimulationFixtures.personage(MixedBuild.KEEPER, 0);
        final var duelist = Step12SimulationFixtures.personage(MixedBuild.DUELIST, 0);
        final var raider = Step12SimulationFixtures.personage(MixedBuild.RAIDER, 0);
        final var formationBreaker = Step12SimulationFixtures.personage(MixedBuild.FORMATION_BREAKER, 0);
        final var artillerist = Step12SimulationFixtures.personage(MixedBuild.ARTILLERIST, 0);
        final var blocker = Step12SimulationFixtures.personage(MixedBuild.BLOCKER, 0);

        Assertions.assertAll(
            "Таран",
            () -> Assertions.assertTrue(ram.maxHealth() < guardian.maxHealth()),
            () -> Assertions.assertTrue(points(ram, ActiveEnum.GUARD) < points(guardian, ActiveEnum.GUARD)),
            () -> Assertions.assertTrue(attackAtDistance(MixedBuild.RAM, 1) < attackAtDistance(RoleBuild.BRUISER, 1))
        );
        Assertions.assertAll(
            "Хранитель",
            () -> Assertions.assertTrue(keeper.maxHealth() < guardian.maxHealth()),
            () -> Assertions.assertTrue(keeper.totalThreat() < guardian.totalThreat()),
            () -> Assertions.assertTrue(points(keeper, ActiveEnum.TEMPO_BREAK)
                < points(tactician, ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertTrue(attackAtDistance(MixedBuild.KEEPER, 1)
                < attackAtDistance(RoleBuild.BRUISER, 1))
        );
        Assertions.assertAll(
            "Дуэлянт",
            () -> Assertions.assertTrue(points(duelist, ActiveEnum.BERSERK) < points(bruiser, ActiveEnum.BERSERK)),
            () -> Assertions.assertTrue(points(duelist, ActiveEnum.HIT_AND_RUN)
                < points(skirmisher, ActiveEnum.HIT_AND_RUN))
        );
        Assertions.assertAll(
            "Налётчик",
            () -> Assertions.assertTrue(points(raider, ActiveEnum.PENETRATION)
                < points(assassin, ActiveEnum.PENETRATION)),
            () -> Assertions.assertTrue(points(raider, ActiveEnum.HIT_AND_RUN)
                < points(skirmisher, ActiveEnum.HIT_AND_RUN)),
            () -> Assertions.assertTrue(raider.maxHealth() < assassin.maxHealth())
        );
        Assertions.assertAll(
            "Ломатель строя",
            () -> Assertions.assertTrue(attackAtDistance(MixedBuild.FORMATION_BREAKER, 1)
                < attackAtDistance(RoleBuild.BRUISER, 1)),
            () -> Assertions.assertTrue(points(formationBreaker, ActiveEnum.BERSERK)
                < points(bruiser, ActiveEnum.BERSERK)),
            () -> Assertions.assertTrue(points(formationBreaker, ActiveEnum.TEMPO_BREAK)
                < points(tactician, ActiveEnum.TEMPO_BREAK))
        );
        Assertions.assertAll(
            "Артиллерист",
            () -> Assertions.assertEquals(4, points(artillerist, ActiveEnum.ACCUMULATION)),
            () -> Assertions.assertEquals(0, points(artillerist, ActiveEnum.DOUBLE_ATTACK)),
            () -> Assertions.assertTrue(attackAtDistance(MixedBuild.ARTILLERIST, 4)
                < attackAtDistance(RoleBuild.RANGER_PHYSICAL, 4))
        );
        Assertions.assertAll(
            "Заградитель",
            () -> Assertions.assertEquals(4, points(blocker, ActiveEnum.KNOCKBACK)),
            () -> Assertions.assertEquals(0, points(blocker, ActiveEnum.DOUBLE_ATTACK)),
            () -> Assertions.assertTrue(attackAtDistance(MixedBuild.BLOCKER, 4)
                < attackAtDistance(RoleBuild.RANGER_PHYSICAL, 4))
        );
        Assertions.assertEquals(4, points(ranger, ActiveEnum.DOUBLE_ATTACK));
    }

    private static ExpectedBuild build(
        String displayName,
        Position position,
        TargetingTactic tactic,
        String... itemCodes
    ) {
        return new ExpectedBuild(displayName, position, tactic, List.of(itemCodes));
    }

    private static ExpectedModifier modifier(String carrier, String code, ActiveEnum skill, int points) {
        return new ExpectedModifier(carrier, code, skill, points);
    }

    private static List<String> itemCodes(List<Item> items) {
        return items.stream().map(item -> item.object().code()).toList();
    }

    private static List<ExpectedModifier> modifiers(List<Item> items) {
        return items.stream()
            .map(item -> new ExpectedModifier(
                item.object().code(),
                item.modifier().orElseThrow().code(),
                item.modifier().orElseThrow().activeEnum(),
                item.skillPoints()
            ))
            .toList();
    }

    private static Map<ActiveEnum, Integer> skills(BattlePersonage personage) {
        return personage.skillSnapshots().stream().collect(Collectors.toMap(
            BattleSkillInitSnapshot::code,
            BattleSkillInitSnapshot::points
        ));
    }

    private static AttackType primaryAttackType(List<Item> items) {
        final var totals = new EnumMap<AttackType, Integer>(AttackType.class);
        items.stream().flatMap(item -> item.itemAttacks().stream()).forEach(attack ->
            totals.merge(attack.attackType(), attack.attack(), Math::addExact)
        );
        AttackType selected = null;
        var selectedAttack = 0;
        for (final var type : AttackType.values()) {
            final int attack = totals.getOrDefault(type, 0);
            if (attack > selectedAttack) {
                selected = type;
                selectedAttack = attack;
            }
        }
        return selected;
    }

    private static int points(BattlePersonage personage, ActiveEnum skill) {
        return skills(personage).getOrDefault(skill, 0);
    }

    private static int attackAtDistance(MixedBuild build, int distance) {
        return attackAtDistance(Step12SimulationFixtures.items(build, 0), distance);
    }

    private static int attackAtDistance(RoleBuild build, int distance) {
        return attackAtDistance(Step12SimulationFixtures.items(build, 0), distance);
    }

    private static int attackAtDistance(List<Item> items, int distance) {
        return items.stream()
            .flatMap(item -> item.itemAttacks().stream())
            .filter(attack -> attack.isAvailableAt(distance))
            .mapToInt(attack -> attack.attack())
            .sum();
    }

    private record ExpectedBuild(
        String displayName,
        Position position,
        TargetingTactic tactic,
        List<String> itemCodes
    ) {
    }

    private record ExpectedModifier(String carrier, String code, ActiveEnum skill, int points) {
    }
}
