package ru.homyakin.seeker.game.battle.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

/** Exact versioned catalog builds preregistered for step 12 battle simulations. */
public final class Step12SimulationFixtures {
    public static final List<Integer> CONTROL_LEVELS = List.of(0, 3, 6, 10, 20);
    public static final List<Integer> MIRROR_PARTY_SIZES = List.of(1, 3, 7);

    private static final Map<String, ItemObject> CURRENT_OBJECTS_BY_CODE;
    private static final Map<String, ItemObject> HISTORICAL_V2_OBJECTS_BY_CODE;
    private static final Map<String, Modifier> CURRENT_MODIFIERS_BY_CODE;
    private static final Map<String, Modifier> HISTORICAL_V2_MODIFIERS_BY_CODE;
    private static final BuildSpec REFERENCE_FRONT = new BuildSpec(
        "Опора-фронт",
        Position.FRONT,
        TargetingTactic.THREAT,
        List.of(
            "sword", "shortsword",
            "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
        ),
        Optional.empty()
    );
    private static final BuildSpec REFERENCE_BACK = new BuildSpec(
        "Опора-тыл",
        Position.BACK,
        TargetingTactic.THREAT,
        List.of(
            "longbow",
            "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
        ),
        Optional.empty()
    );

    static {
        final var currentCatalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V2);
        final var historicalV2Catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1);
        CURRENT_OBJECTS_BY_CODE = currentCatalog.itemObjects().stream()
            .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));
        HISTORICAL_V2_OBJECTS_BY_CODE = historicalV2Catalog.itemObjects().stream()
            .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));
        CURRENT_MODIFIERS_BY_CODE = currentCatalog.modifiers().stream()
            .collect(Collectors.toUnmodifiableMap(Modifier::code, Function.identity()));
        HISTORICAL_V2_MODIFIERS_BY_CODE = historicalV2Catalog.modifiers().stream()
            .collect(Collectors.toUnmodifiableMap(Modifier::code, Function.identity()));
        for (final var build : RoleBuild.values()) {
            validate(build.spec(), CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE);
        }
        for (final var build : V2Build.values()) {
            validate(build.spec(), HISTORICAL_V2_OBJECTS_BY_CODE, HISTORICAL_V2_MODIFIERS_BY_CODE);
        }
        for (final var build : V3Build.values()) {
            validate(build.spec(), CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE);
        }
        for (final var build : MixedBuild.values()) {
            validate(build.spec(), CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE);
        }
        validate(REFERENCE_FRONT, CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE);
        validate(REFERENCE_BACK, CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE);
        validate(REFERENCE_FRONT, HISTORICAL_V2_OBJECTS_BY_CODE, HISTORICAL_V2_MODIFIERS_BY_CODE);
        validate(REFERENCE_BACK, HISTORICAL_V2_OBJECTS_BY_CODE, HISTORICAL_V2_MODIFIERS_BY_CODE);
    }

    private Step12SimulationFixtures() {
    }

    public static BattlePersonage personage(RoleBuild build, int enhanceLevel) {
        return currentPersonage(build.spec(), enhanceLevel);
    }

    public static List<Item> items(RoleBuild build, int enhanceLevel) {
        return currentItems(build.spec(), enhanceLevel);
    }

    public static BattlePersonage personage(MixedBuild build, int enhanceLevel) {
        return currentPersonage(build.spec(), enhanceLevel);
    }

    public static List<Item> items(MixedBuild build, int enhanceLevel) {
        return currentItems(build.spec(), enhanceLevel);
    }

    public static BattlePersonage v2Personage(V2Build build, int enhanceLevel) {
        return historicalV2Personage(build.spec(), enhanceLevel);
    }

    public static BattlePersonage v2Personage(V2Build build, Position position, int enhanceLevel) {
        return historicalV2Personage(build.spec(), position, enhanceLevel, true);
    }

    /**
     * Creates the same catalog build while removing only its single modifier. The carrier keeps its rarity and level,
     * so the comparison changes the tested skill and nothing else.
     */
    public static BattlePersonage v2PersonageWithoutModifier(V2Build build, int enhanceLevel) {
        return historicalV2Personage(build.spec(), build.position(), enhanceLevel, false);
    }

    public static BattlePersonage v2PersonageWithoutModifier(
        V2Build build,
        Position position,
        int enhanceLevel
    ) {
        return historicalV2Personage(build.spec(), position, enhanceLevel, false);
    }

    public static List<Item> v2Items(V2Build build, int enhanceLevel) {
        return historicalV2Items(build.spec(), enhanceLevel, true);
    }

    public static List<Item> v2ItemsWithoutModifier(V2Build build, int enhanceLevel) {
        return historicalV2Items(build.spec(), enhanceLevel, false);
    }

    public static CombatSimulationTeams v2Teams(V2Matchup matchup, int enhanceLevel) {
        return v2Teams(matchup, enhanceLevel, Set.of());
    }

    /**
     * Creates an exact revision-two composition while removing only the modifiers of the requested builds.
     * Carrier rarity, enhance level, placement, tactic and every other item remain unchanged.
     */
    public static CombatSimulationTeams v2Teams(
        V2Matchup matchup,
        int enhanceLevel,
        Set<V2Build> buildsWithoutModifier
    ) {
        validateControlLevel(enhanceLevel);
        final var disabled = Set.copyOf(buildsWithoutModifier);
        return new CombatSimulationTeams(
            v2Team(matchup.evaluatedTeam(), enhanceLevel, disabled),
            v2Team(matchup.opponents(), enhanceLevel, disabled)
        );
    }

    public static BattlePersonage v3Personage(V3Build build, int enhanceLevel) {
        return currentPersonage(build.spec(), enhanceLevel);
    }

    public static BattlePersonage v3Personage(V3Build build, Position position, int enhanceLevel) {
        return currentPersonage(build.spec(), position, enhanceLevel, true);
    }

    public static BattlePersonage v3PersonageWithoutModifier(V3Build build, int enhanceLevel) {
        return currentPersonage(build.spec(), build.position(), enhanceLevel, false);
    }

    public static BattlePersonage v3PersonageWithoutModifier(
        V3Build build,
        Position position,
        int enhanceLevel
    ) {
        return currentPersonage(build.spec(), position, enhanceLevel, false);
    }

    public static List<Item> v3Items(V3Build build, int enhanceLevel) {
        return currentItems(build.spec(), enhanceLevel, true);
    }

    public static List<Item> v3ItemsWithoutModifier(V3Build build, int enhanceLevel) {
        return currentItems(build.spec(), enhanceLevel, false);
    }

    public static CombatSimulationTeams v3Teams(V3Matchup matchup, int enhanceLevel) {
        return v3Teams(matchup, enhanceLevel, Set.of());
    }

    /** Creates one revision-three composition while disabling only the requested role modifiers. */
    public static CombatSimulationTeams v3Teams(
        V3Matchup matchup,
        int enhanceLevel,
        Set<V3Build> buildsWithoutModifier
    ) {
        validateControlLevel(enhanceLevel);
        final var disabled = Set.copyOf(buildsWithoutModifier);
        return new CombatSimulationTeams(
            v3Team(matchup.evaluatedTeam(), enhanceLevel, disabled),
            v3Team(matchup.opponents(), enhanceLevel, disabled)
        );
    }

    /**
     * Creates the preregistered 1-, 3- or 7-person role team. Every item in a team uses the same enhance level.
     */
    public static List<BattlePersonage> roleTeam(RoleBuild role, int partySize, int enhanceLevel) {
        validateControlLevel(enhanceLevel);
        return switch (partySize) {
            case 1 -> List.of(personage(role, enhanceLevel));
            case 3 -> List.of(
                currentPersonage(REFERENCE_FRONT, enhanceLevel),
                currentPersonage(role.spec(), enhanceLevel),
                currentPersonage(REFERENCE_BACK, enhanceLevel)
            );
            case 7 -> {
                final var result = new ArrayList<BattlePersonage>(partySize);
                IntStream.range(0, 2).forEach(ignored ->
                    result.add(currentPersonage(REFERENCE_FRONT, enhanceLevel))
                );
                IntStream.range(0, 3).forEach(ignored ->
                    result.add(currentPersonage(role.spec(), enhanceLevel))
                );
                IntStream.range(0, 2).forEach(ignored ->
                    result.add(currentPersonage(REFERENCE_BACK, enhanceLevel))
                );
                yield List.copyOf(result);
            }
            default -> throw new IllegalArgumentException("Unsupported step 12 party size: " + partySize);
        };
    }

    public static CombatSimulationTeams teams(
        RoleBuild evaluated,
        RoleBuild opponents,
        int partySize,
        int enhanceLevel
    ) {
        return new CombatSimulationTeams(
            roleTeam(evaluated, partySize, enhanceLevel),
            roleTeam(opponents, partySize, enhanceLevel)
        );
    }

    static CombatSimulationTeams historicalV2Teams(
        RoleBuild evaluated,
        RoleBuild opponents,
        int partySize,
        int enhanceLevel
    ) {
        return new CombatSimulationTeams(
            historicalV2RoleTeam(evaluated, partySize, enhanceLevel),
            historicalV2RoleTeam(opponents, partySize, enhanceLevel)
        );
    }

    static BattlePersonage historicalV2Personage(RoleBuild build, int enhanceLevel) {
        return historicalV2Personage(build.spec(), enhanceLevel);
    }

    static List<Item> historicalV2Items(RoleBuild build, int enhanceLevel) {
        return historicalV2Items(build.spec(), enhanceLevel, true);
    }

    private static List<BattlePersonage> historicalV2RoleTeam(
        RoleBuild role,
        int partySize,
        int enhanceLevel
    ) {
        validateControlLevel(enhanceLevel);
        return switch (partySize) {
            case 1 -> List.of(historicalV2Personage(role.spec(), enhanceLevel));
            case 3 -> List.of(
                historicalV2Personage(REFERENCE_FRONT, enhanceLevel),
                historicalV2Personage(role.spec(), enhanceLevel),
                historicalV2Personage(REFERENCE_BACK, enhanceLevel)
            );
            case 7 -> {
                final var result = new ArrayList<BattlePersonage>(partySize);
                IntStream.range(0, 2).forEach(ignored ->
                    result.add(historicalV2Personage(REFERENCE_FRONT, enhanceLevel))
                );
                IntStream.range(0, 3).forEach(ignored ->
                    result.add(historicalV2Personage(role.spec(), enhanceLevel))
                );
                IntStream.range(0, 2).forEach(ignored ->
                    result.add(historicalV2Personage(REFERENCE_BACK, enhanceLevel))
                );
                yield List.copyOf(result);
            }
            default -> throw new IllegalArgumentException("Unsupported step 12 party size: " + partySize);
        };
    }

    private static BattlePersonage currentPersonage(BuildSpec build, int enhanceLevel) {
        return currentPersonage(build, build.position(), enhanceLevel, true);
    }

    private static BattlePersonage currentPersonage(
        BuildSpec build,
        Position position,
        int enhanceLevel,
        boolean includeModifier
    ) {
        final var personage = BattlePersonage.forScalingSkillsV2Combat(
            items(build, enhanceLevel, includeModifier, CURRENT_OBJECTS_BY_CODE, CURRENT_MODIFIERS_BY_CODE),
            position,
            Map.of(),
            Optional.of(build.displayName())
        );
        personage.setTargetingTactic(build.targetingTactic());
        return personage;
    }

    private static BattlePersonage historicalV2Personage(BuildSpec build, int enhanceLevel) {
        return historicalV2Personage(build, build.position(), enhanceLevel, true);
    }

    private static BattlePersonage historicalV2Personage(
        BuildSpec build,
        Position position,
        int enhanceLevel,
        boolean includeModifier
    ) {
        return personage(
            build,
            position,
            enhanceLevel,
            includeModifier,
            HISTORICAL_V2_OBJECTS_BY_CODE,
            HISTORICAL_V2_MODIFIERS_BY_CODE,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );
    }

    private static BattlePersonage personage(
        BuildSpec build,
        Position position,
        int enhanceLevel,
        boolean includeModifier,
        Map<String, ItemObject> objects,
        Map<String, Modifier> modifiers,
        SkillFormulaVersion formulaVersion
    ) {
        final var personage = new BattlePersonage(
            items(build, enhanceLevel, includeModifier, objects, modifiers),
            position,
            Map.of(),
            PersonageEffects.EMPTY,
            null,
            Optional.of(build.displayName()),
            formulaVersion
        );
        personage.setTargetingTactic(build.targetingTactic());
        return personage;
    }

    private static List<Item> currentItems(BuildSpec build, int enhanceLevel) {
        return currentItems(build, enhanceLevel, true);
    }

    private static List<Item> currentItems(
        BuildSpec build,
        int enhanceLevel,
        boolean includeModifier
    ) {
        return items(
            build,
            enhanceLevel,
            includeModifier,
            CURRENT_OBJECTS_BY_CODE,
            CURRENT_MODIFIERS_BY_CODE
        );
    }

    private static List<Item> historicalV2Items(
        BuildSpec build,
        int enhanceLevel,
        boolean includeModifier
    ) {
        return items(
            build,
            enhanceLevel,
            includeModifier,
            HISTORICAL_V2_OBJECTS_BY_CODE,
            HISTORICAL_V2_MODIFIERS_BY_CODE
        );
    }

    private static List<Item> items(
        BuildSpec build,
        int enhanceLevel,
        boolean includeModifier,
        Map<String, ItemObject> objects,
        Map<String, Modifier> modifiers
    ) {
        validateControlLevel(enhanceLevel);
        return build.itemCodes().stream()
            .map(code -> item(build, code, enhanceLevel, includeModifier, objects, modifiers))
            .toList();
    }

    private static Item item(
        BuildSpec build,
        String code,
        int enhanceLevel,
        boolean includeModifier,
        Map<String, ItemObject> objects,
        Map<String, Modifier> modifiers
    ) {
        final var object = object(objects, code);
        final var assignment = build.modifiers().stream()
            .filter(it -> it.itemCode().equals(code))
            .findFirst();
        if (assignment.isPresent()) {
            return new Item(
                object,
                includeModifier
                    ? Optional.of(modifier(modifiers, assignment.get().modifierCode()))
                    : Optional.empty(),
                assignment.get().rarity(),
                enhanceLevel
            );
        }
        return new Item(object, Optional.empty(), ItemRarity.COMMON, enhanceLevel);
    }

    private static List<BattlePersonage> v2Team(
        List<V2Placement> placements,
        int enhanceLevel,
        Set<V2Build> buildsWithoutModifier
    ) {
        return placements.stream()
            .map(placement -> buildsWithoutModifier.contains(placement.build())
                ? v2PersonageWithoutModifier(placement.build(), placement.position(), enhanceLevel)
                : v2Personage(placement.build(), placement.position(), enhanceLevel))
            .toList();
    }

    private static List<BattlePersonage> v3Team(
        List<V3Placement> placements,
        int enhanceLevel,
        Set<V3Build> buildsWithoutModifier
    ) {
        return placements.stream()
            .map(placement -> buildsWithoutModifier.contains(placement.build())
                ? v3PersonageWithoutModifier(placement.build(), placement.position(), enhanceLevel)
                : v3Personage(placement.build(), placement.position(), enhanceLevel))
            .toList();
    }

    private static void validate(
        BuildSpec build,
        Map<String, ItemObject> objects,
        Map<String, Modifier> modifiers
    ) {
        if (build.itemCodes().stream().distinct().count() != build.itemCodes().size()) {
            throw new IllegalStateException("Duplicate item in step 12 build: " + build.displayName());
        }
        build.itemCodes().forEach(code -> object(objects, code));
        if (build.modifiers().stream().map(ModifierAssignment::itemCode).distinct().count()
            != build.modifiers().size()) {
            throw new IllegalStateException("Multiple modifiers use one carrier: " + build.displayName());
        }
        build.modifiers().forEach(assignment -> {
            if (!build.itemCodes().contains(assignment.itemCode())) {
                throw new IllegalStateException("Modifier carrier is absent from build: " + build.displayName());
            }
            final var object = object(objects, assignment.itemCode());
            final var modifier = modifier(modifiers, assignment.modifierCode());
            if (!ModifierCompatibility.isCompatible(object, modifier, assignment.selectedSlot())) {
                throw new IllegalStateException(
                    "Incompatible modifier in step 12 build: " + build.displayName()
                );
            }
        });
        final int points = build.modifiers().stream()
            .mapToInt(assignment -> assignment.rarity().skillPoints()
                * object(objects, assignment.itemCode()).slots().size())
            .sum();
        if (!build.modifiers().isEmpty() && points != 4) {
            throw new IllegalStateException(
                "Step 12 build modifiers must provide exactly four points in total: " + build.displayName()
            );
        }
    }

    private static ItemObject object(Map<String, ItemObject> objects, String code) {
        final var object = objects.get(code);
        if (object == null) {
            throw new IllegalStateException("Unknown catalog item in step 12 fixture: " + code);
        }
        return object;
    }

    private static Modifier modifier(Map<String, Modifier> modifiers, String code) {
        final var modifier = modifiers.get(code);
        if (modifier == null) {
            throw new IllegalStateException("Unknown catalog modifier in step 12 fixture: " + code);
        }
        return modifier;
    }

    private static void validateControlLevel(int enhanceLevel) {
        if (!CONTROL_LEVELS.contains(enhanceLevel)) {
            throw new IllegalArgumentException("Unsupported step 12 control level: " + enhanceLevel);
        }
    }

    public enum RoleBuild {
        GUARDIAN(new BuildSpec(
            "Страж",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "mace", "tower_shield",
                "cuirass", "greaves", "sabatons", "great_helm", "gauntlets"
            ),
            Optional.of(new ModifierAssignment(
                "tower_shield", "guarding", ItemRarity.LEGENDARY, PersonageSlot.OFF_HAND
            ))
        )),
        BRUISER(new BuildSpec(
            "Громила",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "sledgehammer",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "sledgehammer", "furious", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        SKIRMISHER(new BuildSpec(
            "Застрельщик",
            Position.MID,
            TargetingTactic.CHALLENGE_THE_AGILE,
            List.of(
                "spear", "dirk",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "cloth_boots", "swift", ItemRarity.LEGENDARY, PersonageSlot.SHOES
            ))
        )),
        ASSASSIN(new BuildSpec(
            "Убийца",
            Position.FRONT,
            TargetingTactic.EXECUTIONER,
            List.of(
                "rapier", "dagger",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "rapier", "penetrating", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        )),
        RANGER_PHYSICAL(new BuildSpec(
            "Дальнобоец, физический",
            Position.BACK,
            TargetingTactic.EXPLOIT_WEAKNESS,
            List.of(
                "longbow",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "longbow", "double", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        RANGER_MAGICAL(new BuildSpec(
            "Дальнобоец, магический",
            Position.BACK,
            TargetingTactic.EXPLOIT_WEAKNESS,
            List.of(
                "battle_staff",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "battle_staff", "double", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        BREAKER_PHYSICAL(new BuildSpec(
            "Разрушитель, физический",
            Position.BACK,
            TargetingTactic.EXECUTIONER,
            List.of(
                "crossbow",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "crossbow", "charging", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        BREAKER_MAGICAL(new BuildSpec(
            "Разрушитель, магический",
            Position.BACK,
            TargetingTactic.EXECUTIONER,
            List.of(
                "ritual_staff",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "ritual_staff", "charging", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        TACTICIAN_PHYSICAL(new BuildSpec(
            "Тактик, физический",
            Position.MID,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "halberd",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "halberd", "disrupting", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        TACTICIAN_MAGICAL(new BuildSpec(
            "Тактик, магический",
            Position.MID,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "staff", "orb",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "staff", "disrupting", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        ));

        private final BuildSpec spec;

        RoleBuild(BuildSpec spec) {
            this.spec = spec;
        }

        public String displayName() {
            return spec.displayName();
        }

        public Position position() {
            return spec.position();
        }

        public TargetingTactic targetingTactic() {
            return spec.targetingTactic();
        }

        BuildSpec spec() {
            return spec;
        }
    }

    /** Exact catalog builds that deliberately combine two role mechanics at a fixed four-point budget. */
    public enum MixedBuild {
        RAM(new BuildSpec(
            "Таран",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "mace", "tower_shield",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            List.of(
                new ModifierAssignment("mace", "furious", ItemRarity.RARE, PersonageSlot.MAIN_HAND),
                new ModifierAssignment("tower_shield", "guarding", ItemRarity.RARE, PersonageSlot.OFF_HAND)
            )
        )),
        KEEPER(new BuildSpec(
            "Хранитель",
            Position.FRONT,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "spear", "tower_shield",
                "cuirass", "greaves", "sabatons", "hood", "cloth_gloves"
            ),
            List.of(
                new ModifierAssignment("spear", "disrupting", ItemRarity.RARE, PersonageSlot.MAIN_HAND),
                new ModifierAssignment("tower_shield", "guarding", ItemRarity.RARE, PersonageSlot.OFF_HAND)
            )
        )),
        DUELIST(new BuildSpec(
            "Дуэлянт",
            Position.MID,
            TargetingTactic.CHALLENGE_THE_AGILE,
            List.of(
                "sword", "dirk",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            List.of(
                new ModifierAssignment("sword", "furious", ItemRarity.RARE, PersonageSlot.MAIN_HAND),
                new ModifierAssignment("boots", "swift", ItemRarity.RARE, PersonageSlot.SHOES)
            )
        )),
        RAIDER(new BuildSpec(
            "Налётчик",
            Position.FRONT,
            TargetingTactic.EXECUTIONER,
            List.of(
                "rapier", "dirk",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            List.of(
                new ModifierAssignment("rapier", "penetrating", ItemRarity.RARE, PersonageSlot.MAIN_HAND),
                new ModifierAssignment("hood", "swift", ItemRarity.RARE, PersonageSlot.HELMET)
            )
        )),
        FORMATION_BREAKER(new BuildSpec(
            "Ломатель строя",
            Position.MID,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "spear", "club",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            List.of(
                new ModifierAssignment("spear", "disrupting", ItemRarity.RARE, PersonageSlot.MAIN_HAND),
                new ModifierAssignment("club", "furious", ItemRarity.RARE, PersonageSlot.OFF_HAND)
            )
        )),
        ARTILLERIST(new BuildSpec(
            "Артиллерист",
            Position.BACK,
            TargetingTactic.EXECUTIONER,
            List.of(
                "bow",
                "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves"
            ),
            List.of(new ModifierAssignment(
                "bow", "charging", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        BLOCKER(new BuildSpec(
            "Заградитель",
            Position.BACK,
            TargetingTactic.EXPLOIT_WEAKNESS,
            List.of(
                "bow",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            List.of(new ModifierAssignment(
                "bow", "knocking", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        ));

        private final BuildSpec spec;

        MixedBuild(BuildSpec spec) {
            this.spec = spec;
        }

        public String displayName() {
            return spec.displayName();
        }

        public Position position() {
            return spec.position();
        }

        public TargetingTactic targetingTactic() {
            return spec.targetingTactic();
        }

        BuildSpec spec() {
            return spec;
        }
    }

    /** Catalog-only builds used by the preregistered second revision of the six directed matchups. */
    public enum V2Build {
        SUPPORT(new BuildSpec(
            "ОПОРА",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "staff", "tome",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            Optional.empty()
        )),
        CROSSBOW_SUPPORT(new BuildSpec(
            "ОПОРА-АРБАЛЕТ",
            Position.MID,
            TargetingTactic.THREAT,
            List.of(
                "crossbow",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            Optional.empty()
        )),
        BRUISER_BLUNT(new BuildSpec(
            "Громила, дробящий",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "sledgehammer",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "sledgehammer", "furious", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        GUARDIAN_BLUNT(new BuildSpec(
            "Страж, дробящий",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "mace", "tower_shield",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "tower_shield", "guarding", ItemRarity.LEGENDARY, PersonageSlot.OFF_HAND
            ))
        )),
        SKIRMISHER_SLASH(new BuildSpec(
            "Застрельщик, рубящий",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "sword", "shortsword",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "cloth_boots", "swift", ItemRarity.LEGENDARY, PersonageSlot.SHOES
            ))
        )),
        BREAKER_CLOSE_SLASH(new BuildSpec(
            "Разрушитель, ближний рубящий",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "two_handed_sword",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "two_handed_sword", "charging", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        ASSASSIN_PIERCE(new BuildSpec(
            "Убийца, колющий",
            Position.FRONT,
            TargetingTactic.EXECUTIONER,
            List.of(
                "rapier", "dagger",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "rapier", "penetrating", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        )),
        RANGER_PIERCE(new BuildSpec(
            "Дальнобоец, колющий",
            Position.BACK,
            TargetingTactic.EXPLOIT_WEAKNESS,
            List.of(
                "longbow",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "longbow", "double", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        RANGER_MAGICAL(new BuildSpec(
            "Дальнобоец, магический",
            Position.BACK,
            TargetingTactic.EXPLOIT_WEAKNESS,
            List.of(
                "battle_staff",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "battle_staff", "double", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        BRUISER_MAGICAL_CLOTH(new BuildSpec(
            "Громила, магический в ткани",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "staff", "orb",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "staff", "furious", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        )),
        BRUISER_MAGICAL_LEATHER(new BuildSpec(
            "Громила, магический в коже",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "staff", "orb",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "staff", "furious", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        )),
        BREAKER_MAGICAL_LEATHER(new BuildSpec(
            "Разрушитель, магический в коже",
            Position.BACK,
            TargetingTactic.EXECUTIONER,
            List.of(
                "ritual_staff",
                "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "ritual_staff", "charging", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        TACTICIAN_MAGICAL(new BuildSpec(
            "Тактик, магический",
            Position.FRONT,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "staff", "orb",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "staff", "disrupting", ItemRarity.LEGENDARY, PersonageSlot.MAIN_HAND
            ))
        ));

        private final BuildSpec spec;

        V2Build(BuildSpec spec) {
            this.spec = spec;
        }

        public String displayName() {
            return spec.displayName();
        }

        public Position position() {
            return spec.position();
        }

        public TargetingTactic targetingTactic() {
            return spec.targetingTactic();
        }

        BuildSpec spec() {
            return spec;
        }
    }

    /** Candidate builds used only by the third revision and its non-final tuning runs. */
    public enum V3Build {
        SUPPORT(V2Build.SUPPORT.spec()),
        CROSSBOW_SUPPORT(V2Build.CROSSBOW_SUPPORT.spec()),
        BRUISER_BLUNT(new BuildSpec(
            "Громила, дробящий V3",
            Position.FRONT,
            TargetingTactic.WOUNDED_HUNTER,
            List.of(
                "sledgehammer",
                "breastplate", "leather_chausses", "boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "sledgehammer", "furious", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        )),
        GUARDIAN_BLUNT(V2Build.GUARDIAN_BLUNT.spec()),
        SKIRMISHER_SLASH(new BuildSpec(
            "Застрельщик, рубящий V3",
            Position.FRONT,
            TargetingTactic.THREAT,
            List.of(
                "sword", "shortsword",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "cloth_boots", "swift", ItemRarity.LEGENDARY, PersonageSlot.SHOES
            ))
        )),
        BREAKER_CLOSE_SLASH(V2Build.BREAKER_CLOSE_SLASH.spec()),
        ASSASSIN_PIERCE(V2Build.ASSASSIN_PIERCE.spec()),
        RANGER_PIERCE(V2Build.RANGER_PIERCE.spec()),
        RANGER_MAGICAL(V2Build.RANGER_MAGICAL.spec()),
        BRUISER_MAGICAL_CLOTH(V2Build.BRUISER_MAGICAL_CLOTH.spec()),
        BRUISER_MAGICAL_LEATHER(V2Build.BRUISER_MAGICAL_LEATHER.spec()),
        BREAKER_MAGICAL_LEATHER(V2Build.BREAKER_MAGICAL_LEATHER.spec()),
        TACTICIAN_MAGICAL(new BuildSpec(
            "Тактик, магический V3",
            Position.FRONT,
            TargetingTactic.INITIATIVE_INTERCEPTION,
            List.of(
                "command_staff",
                "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"
            ),
            Optional.of(new ModifierAssignment(
                "command_staff", "disrupting", ItemRarity.RARE, PersonageSlot.MAIN_HAND
            ))
        ));

        private final BuildSpec spec;

        V3Build(BuildSpec spec) {
            this.spec = spec;
        }

        public String displayName() {
            return spec.displayName();
        }

        public Position position() {
            return spec.position();
        }

        public TargetingTactic targetingTactic() {
            return spec.targetingTactic();
        }

        BuildSpec spec() {
            return spec;
        }
    }

    /** Exact three-person candidate compositions of the third revision. */
    public enum V3Matchup {
        BRUISER_V3(
            "БЛАГО-ГРОМИЛА_V3",
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.BRUISER_BLUNT, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID)
            ),
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.GUARDIAN_BLUNT, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID)
            )
        ),
        SKIRMISHER_V3(
            "БЛАГО-ЗАСТРЕЛЬЩИК_V3",
            List.of(
                at(V3Build.SKIRMISHER_SLASH, Position.FRONT),
                at(V3Build.CROSSBOW_SUPPORT, Position.MID),
                at(V3Build.CROSSBOW_SUPPORT, Position.MID)
            ),
            List.of(
                at(V3Build.BREAKER_CLOSE_SLASH, Position.FRONT),
                at(V3Build.CROSSBOW_SUPPORT, Position.MID),
                at(V3Build.CROSSBOW_SUPPORT, Position.MID)
            )
        ),
        ASSASSIN_V3(
            "БЛАГО-УБИЙЦА_V3",
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.ASSASSIN_PIERCE, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID)
            ),
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID),
                at(V3Build.RANGER_PIERCE, Position.BACK)
            )
        ),
        RANGER_V3(
            "БЛАГО-ДАЛЬНОБОЕЦ_V3",
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID),
                at(V3Build.RANGER_MAGICAL, Position.BACK)
            ),
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.BRUISER_MAGICAL_CLOTH, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID)
            )
        ),
        BREAKER_V3(
            "БЛАГО-РАЗРУШИТЕЛЬ_V3",
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID),
                at(V3Build.BREAKER_MAGICAL_LEATHER, Position.BACK)
            ),
            List.of(
                at(V3Build.SUPPORT, Position.FRONT),
                at(V3Build.BRUISER_MAGICAL_LEATHER, Position.FRONT),
                at(V3Build.SUPPORT, Position.MID)
            )
        ),
        TACTICIAN_V3(
            "БЛАГО-ТАКТИК_V3",
            List.of(
                at(V3Build.TACTICIAN_MAGICAL, Position.FRONT),
                at(V3Build.CROSSBOW_SUPPORT, Position.BACK),
                at(V3Build.CROSSBOW_SUPPORT, Position.BACK)
            ),
            List.of(
                at(V3Build.BRUISER_MAGICAL_CLOTH, Position.FRONT),
                at(V3Build.CROSSBOW_SUPPORT, Position.BACK),
                at(V3Build.CROSSBOW_SUPPORT, Position.BACK)
            )
        );

        private final String code;
        private final List<V3Placement> evaluatedTeam;
        private final List<V3Placement> opponents;

        V3Matchup(String code, List<V3Placement> evaluatedTeam, List<V3Placement> opponents) {
            this.code = code;
            this.evaluatedTeam = List.copyOf(evaluatedTeam);
            this.opponents = List.copyOf(opponents);
        }

        public String code() {
            return code;
        }

        public List<V3Placement> evaluatedTeam() {
            return evaluatedTeam;
        }

        public List<V3Placement> opponents() {
            return opponents;
        }
    }

    /** Exact three-person compositions of the preregistered second revision. */
    public enum V2Matchup {
        BRUISER_V2(
            "БЛАГО-ГРОМИЛА_V2",
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.BRUISER_BLUNT, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID)
            ),
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.GUARDIAN_BLUNT, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID)
            )
        ),
        SKIRMISHER_V2(
            "БЛАГО-ЗАСТРЕЛЬЩИК_V2",
            List.of(
                at(V2Build.SKIRMISHER_SLASH, Position.FRONT),
                at(V2Build.CROSSBOW_SUPPORT, Position.MID),
                at(V2Build.CROSSBOW_SUPPORT, Position.MID)
            ),
            List.of(
                at(V2Build.BREAKER_CLOSE_SLASH, Position.FRONT),
                at(V2Build.CROSSBOW_SUPPORT, Position.MID),
                at(V2Build.CROSSBOW_SUPPORT, Position.MID)
            )
        ),
        ASSASSIN_V2(
            "БЛАГО-УБИЙЦА_V2",
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.ASSASSIN_PIERCE, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID)
            ),
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID),
                at(V2Build.RANGER_PIERCE, Position.BACK)
            )
        ),
        RANGER_V2(
            "БЛАГО-ДАЛЬНОБОЕЦ_V2",
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID),
                at(V2Build.RANGER_MAGICAL, Position.BACK)
            ),
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.BRUISER_MAGICAL_CLOTH, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID)
            )
        ),
        BREAKER_V2(
            "БЛАГО-РАЗРУШИТЕЛЬ_V2",
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID),
                at(V2Build.BREAKER_MAGICAL_LEATHER, Position.BACK)
            ),
            List.of(
                at(V2Build.SUPPORT, Position.FRONT),
                at(V2Build.BRUISER_MAGICAL_LEATHER, Position.FRONT),
                at(V2Build.SUPPORT, Position.MID)
            )
        ),
        TACTICIAN_V2(
            "БЛАГО-ТАКТИК_V2",
            List.of(
                at(V2Build.TACTICIAN_MAGICAL, Position.FRONT),
                at(V2Build.CROSSBOW_SUPPORT, Position.BACK),
                at(V2Build.CROSSBOW_SUPPORT, Position.BACK)
            ),
            List.of(
                at(V2Build.BRUISER_MAGICAL_CLOTH, Position.FRONT),
                at(V2Build.CROSSBOW_SUPPORT, Position.BACK),
                at(V2Build.CROSSBOW_SUPPORT, Position.BACK)
            )
        );

        private final String code;
        private final List<V2Placement> evaluatedTeam;
        private final List<V2Placement> opponents;

        V2Matchup(String code, List<V2Placement> evaluatedTeam, List<V2Placement> opponents) {
            this.code = code;
            this.evaluatedTeam = List.copyOf(evaluatedTeam);
            this.opponents = List.copyOf(opponents);
        }

        public String code() {
            return code;
        }

        public List<V2Placement> evaluatedTeam() {
            return evaluatedTeam;
        }

        public List<V2Placement> opponents() {
            return opponents;
        }
    }

    public record V2Placement(V2Build build, Position position) {
    }

    public record V3Placement(V3Build build, Position position) {
    }

    private static V2Placement at(V2Build build, Position position) {
        return new V2Placement(build, position);
    }

    private static V3Placement at(V3Build build, Position position) {
        return new V3Placement(build, position);
    }

    private record BuildSpec(
        String displayName,
        Position position,
        TargetingTactic targetingTactic,
        List<String> itemCodes,
        List<ModifierAssignment> modifiers
    ) {
        private BuildSpec(
            String displayName,
            Position position,
            TargetingTactic targetingTactic,
            List<String> itemCodes,
            Optional<ModifierAssignment> modifier
        ) {
            this(displayName, position, targetingTactic, itemCodes, modifier.stream().toList());
        }

        private BuildSpec {
            itemCodes = List.copyOf(itemCodes);
            modifiers = List.copyOf(modifiers);
        }
    }

    private record ModifierAssignment(
        String itemCode,
        String modifierCode,
        ItemRarity rarity,
        PersonageSlot selectedSlot
    ) {
    }
}
