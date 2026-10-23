package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/** Representative questions, not a requirement that every alternative wins every comparison. */
final class Step12RepresentativeMatrix {
    static final List<Integer> LEVELS = List.of(0, 3, 6, 10, 20);
    private static final List<String> ARMORS = List.of("CLOTH", "LEATHER", "PLATE", "ARCANE");

    private Step12RepresentativeMatrix() {
    }

    static List<Scenario> scenarios() {
        final var result = new ArrayList<Scenario>();
        addHands(result);
        addHandModifiers(result);
        addArmorPairs(result);
        addArmorSets(result);
        addExtremes(result);
        addRoleOverview(result);
        addRisks(result);
        result.sort(Comparator.comparing(Scenario::code));
        if (result.stream().map(Scenario::code).distinct().count() != result.size()) {
            throw new IllegalStateException("Representative scenario codes must be unique");
        }
        return List.copyOf(result);
    }

    static List<Build> opponents() {
        return List.of(
            Step12RepresentativeBuilds.hand(
                "SLASH_PLATE", List.of("sword", "shortsword"), "PLATE", Position.FRONT, TargetingTactic.THREAT
            ),
            Step12RepresentativeBuilds.hand(
                "BLUNT_LEATHER", List.of("sledgehammer"), "LEATHER", Position.FRONT, TargetingTactic.THREAT
            ),
            Step12RepresentativeBuilds.hand(
                "PIERCE_CLOTH", List.of("longbow"), "CLOTH", Position.BACK, TargetingTactic.THREAT
            ),
            Step12RepresentativeBuilds.hand(
                "MAGICAL_ARCANE", List.of("ritual_staff"), "ARCANE", Position.BACK, TargetingTactic.THREAT
            )
        );
    }

    static List<Build> anchored(Build tested) {
        final var front = Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN)
            .withAssignments(Map.of()).renamed("NEUTRAL_FRONT").withPosition(Position.FRONT);
        final var back = Step12RepresentativeBuilds.role(RoleBuild.RANGER_PHYSICAL)
            .withAssignments(Map.of()).renamed("NEUTRAL_BACK").withPosition(Position.BACK);
        return List.of(front, tested, back);
    }

    private static void addHands(List<Scenario> result) {
        final var endpointHands = Set.of(
            "mace+club", "sledgehammer", "staff+orb", "halberd", "sword+shortsword",
            "two_handed_sword", "command_staff", "bow", "longbow", "rapier+dagger"
        );
        for (final var hands : Step12RepresentativeBuilds.handBlocks()) {
            final var code = String.join("+", hands);
            var build = Step12RepresentativeBuilds.hand(
                code, hands, "LEATHER", Position.FRONT, TargetingTactic.THREAT
            );
            final int range = build.items(0).stream().flatMap(item -> item.itemAttacks().stream())
                .mapToInt(attack -> attack.maxRange()).max().orElseThrow();
            build = build.withPosition(range >= 3 ? Position.BACK : range == 2 ? Position.MID : Position.FRONT);
            final var levels = endpointHands.contains(code) ? LEVELS : List.of(10);
            for (final int level : levels) {
                for (final var opponent : opponents()) {
                    result.add(Scenario.of("HAND_" + code + "_" + opponent.code(), "HANDS", level,
                        List.of(build), List.of(opponent), 100));
                }
            }
            result.add(Scenario.of("HAND_TEAM_" + code, "HANDS_TEAM", 10,
                anchored(build), anchored(opponents().get(3)), 100));
        }
    }

    private static void addHandModifiers(List<Scenario> result) {
        final var groups = List.of(
            List.of("mace+club", "sledgehammer"),
            List.of("staff+orb", "halberd"),
            List.of("sword+shortsword", "two_handed_sword"),
            List.of("staff+orb", "command_staff")
        );
        int groupIndex = 0;
        for (final var group : groups) {
            groupIndex++;
            final var pairHands = List.of(group.getFirst().split("\\+"));
            final var bothHands = List.of(group.get(1));
            final var pair = Step12RepresentativeBuilds.hand(
                "PAIR_SAME_" + groupIndex, pairHands, "LEATHER", Position.FRONT, TargetingTactic.THREAT
            ).withAssignments(Map.of(
                pairHands.get(0), new Assignment(ActiveEnum.BERSERK, ItemRarity.RARE),
                pairHands.get(1), new Assignment(ActiveEnum.BERSERK, ItemRarity.RARE)
            ));
            final var both = Step12RepresentativeBuilds.hand(
                "BOTH_SAME_" + groupIndex, bothHands, "LEATHER", Position.FRONT, TargetingTactic.THREAT
            ).withAssignments(Map.of(bothHands.getFirst(), new Assignment(ActiveEnum.BERSERK, ItemRarity.RARE)));
            final var pairSplit = pair.renamed("PAIR_SPLIT_" + groupIndex).withAssignments(Map.of(
                pairHands.get(0), new Assignment(ActiveEnum.BERSERK, ItemRarity.RARE),
                pairHands.get(1), new Assignment(ActiveEnum.ACCUMULATION, ItemRarity.RARE)
            ));
            final var bothBurst = both.renamed("BOTH_BURST_" + groupIndex).withAssignments(Map.of(
                bothHands.getFirst(), new Assignment(ActiveEnum.ACCUMULATION, ItemRarity.RARE)
            ));
            for (final int level : LEVELS) {
                for (final var opponent : List.of(opponents().get(0), opponents().get(3))) {
                    final var seedGroup = "HAND_MOD_" + groupIndex + "_" + opponent.code();
                    for (final var build : List.of(pair, both, pairSplit, bothBurst)) {
                        result.add(Scenario.of(seedGroup + "_" + build.code(), "HAND_MODIFIERS", level,
                            List.of(build), List.of(opponent), 120).withSeedGroup(seedGroup));
                    }
                }
            }
        }
    }

    private static void addArmorPairs(List<Scenario> result) {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        final var objects = catalog.itemObjects().stream().collect(java.util.stream.Collectors.toMap(
            ItemObject::code, object -> object
        ));
        for (final var pair : Step12RepresentativeBuilds.armorPairs()) {
            for (final var environment : List.of("OLD", "NEW", "MIXED")) {
                final var family = environment.equals("NEW") ? "NEW_" + pair.family()
                    : environment.equals("MIXED") ? "LEATHER" : pair.family();
                var oldBuild = Step12RepresentativeBuilds.hand(
                    "OLD_" + pair.oldCode(), List.of("rapier", "dagger"), family,
                    Position.FRONT, TargetingTactic.THREAT
                );
                if (environment.equals("MIXED")) {
                    oldBuild = Step12RepresentativeBuilds.custom("MIXED_" + pair.oldCode(),
                        List.of("rapier", "dagger", "robe", "leather_chausses", "sabatons", "circlet", "ceremonial_gloves"),
                        Position.FRONT, TargetingTactic.THREAT, Map.of());
                }
                final var slots = objects.get(pair.oldCode()).slots();
                final var carrier = oldBuild.itemCodes().stream()
                    .filter(code -> objects.get(code).slots().equals(slots)).findFirst().orElseThrow();
                oldBuild = oldBuild.replaceItem(carrier, pair.oldCode());
                final var newBuild = oldBuild.replaceItem(pair.oldCode(), pair.newCode()).renamed("NEW_" + pair.newCode());
                final var levels = environment.equals("OLD") ? List.of(0, 10, 20) : List.of(10);
                for (final int level : levels) {
                    for (final var opponent : opponents()) {
                        final var group = "ARMOR_" + pair.oldCode() + "_" + environment + "_" + opponent.code();
                        result.add(Scenario.of(group + "_OLD", "ARMOR_PAIR", level,
                            List.of(oldBuild), List.of(opponent), 100).withSeedGroup(group));
                        result.add(Scenario.of(group + "_NEW", "ARMOR_PAIR", level,
                            List.of(newBuild), List.of(opponent), 100).withSeedGroup(group));
                    }
                }
            }
        }
    }

    private static void addArmorSets(List<Scenario> result) {
        for (final var family : ARMORS) {
            for (final var variant : List.of(family, "NEW_" + family)) {
                final var build = Step12RepresentativeBuilds.hand(
                    variant, List.of("rapier", "dagger"), variant, Position.FRONT, TargetingTactic.THREAT
                );
                for (final int level : List.of(0, 10, 20)) {
                    for (final var opponent : opponents()) {
                        result.add(Scenario.of("SET_" + variant + "_" + opponent.code(), "ARMOR_SET", level,
                            List.of(build), List.of(opponent), 120).withSeedGroup("SET_" + family + "_" + opponent.code()));
                    }
                    result.add(Scenario.of("SET_TEAM_" + variant, "ARMOR_SET_TEAM", level,
                        anchored(build), anchored(opponents().get(3)), 120).withSeedGroup("SET_TEAM_" + family));
                }
            }
        }
    }

    private static void addExtremes(List<Scenario> result) {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        final Map<String, ToDoubleFunction<ItemObject>> metrics = new LinkedHashMap<>();
        metrics.put("HEALTH", ItemObject::health);
        metrics.put("SPEED", ItemObject::speed);
        metrics.put("DODGE", ItemObject::dodgeChance);
        metrics.put("CRIT", ItemObject::critChance);
        metrics.put("CRIT_DAMAGE", ItemObject::critMultiplier);
        metrics.put("THREAT", ItemObject::baseThreat);
        metrics.put("IMPACT", ItemObject::impact);
        final var seen = new HashSet<List<String>>();
        for (final var metric : metrics.entrySet()) {
            for (final boolean maximum : List.of(false, true)) {
                final var chosen = new ArrayList<String>();
                var comparator = Comparator.comparingDouble(metric.getValue()).thenComparing(ItemObject::code);
                if (maximum) {
                    comparator = comparator.reversed();
                }
                final var ordering = comparator;
                for (final var slot : List.of(
                    PersonageSlot.BODY, PersonageSlot.PANTS, PersonageSlot.SHOES,
                    PersonageSlot.HELMET, PersonageSlot.GLOVES
                )) {
                    chosen.add(catalog.itemObjects().stream().filter(object -> object.slots().equals(Set.of(slot)))
                        .min(ordering).orElseThrow().code());
                }
                final var hands = Step12RepresentativeBuilds.handBlocks().stream()
                    .min(Comparator.<List<String>>comparingDouble(block -> block.stream().mapToDouble(code ->
                        metric.getValue().applyAsDouble(catalog.itemObjects().stream()
                            .filter(object -> object.code().equals(code)).findFirst().orElseThrow())
                    ).sum() * (maximum ? -1 : 1)).thenComparing(block -> String.join("+", block)))
                    .orElseThrow();
                chosen.addAll(0, hands);
                if (!seen.add(List.copyOf(chosen))) {
                    continue;
                }
                var build = Step12RepresentativeBuilds.custom(
                    (maximum ? "MAX_" : "MIN_") + metric.getKey(), chosen,
                    Position.FRONT, TargetingTactic.THREAT, Map.of()
                );
                if (metric.getKey().equals("IMPACT") && maximum) {
                    final var carrier = build.items(0).stream().filter(item -> !item.object().attacks().isEmpty())
                        .map(item -> item.object().code()).findFirst().orElseThrow();
                    build = build.withAssignments(Map.of(carrier, new Assignment(ActiveEnum.TEMPO_BREAK, ItemRarity.RARE)));
                }
                for (final int level : List.of(0, 10, 20)) {
                    for (final var opponent : opponents()) {
                        result.add(Scenario.of("EXTREME_" + build.code() + "_" + opponent.code(), "EXTREME", level,
                            List.of(build), List.of(opponent), 120));
                    }
                }
            }
        }
    }

    private static void addRoleOverview(List<Scenario> result) {
        final var builds = new ArrayList<Build>();
        for (final var role : RoleBuild.values()) {
            builds.add(Step12RepresentativeBuilds.role(role));
        }
        for (final var mixed : MixedBuild.values()) {
            builds.add(Step12RepresentativeBuilds.mixed(mixed));
        }
        for (int first = 0; first < builds.size(); first++) {
            for (int second = first + 1; second < builds.size(); second++) {
                final var left = builds.get(first);
                final var right = builds.get(second);
                result.add(Scenario.of("OVERVIEW_" + left.code() + "_" + right.code(), "OVERVIEW", 10,
                    anchored(left), anchored(right), 160));
            }
        }
    }

    private static void addRisks(List<Scenario> result) {
        for (final var rarity : List.of(ItemRarity.RARE, ItemRarity.LEGENDARY)) {
            final var levels = rarity == ItemRarity.LEGENDARY ? LEVELS : List.of(0, 20);
            for (final var pair : Step12RepresentativeRisks.pairs(rarity)) {
                for (final int level : levels) {
                    final var scenario = Scenario.of(pair.code() + "_" + rarity, "RISK", level,
                        pair.left(), pair.right(), 240);
                    result.add(scenario);
                    final var disabled = pair.code().startsWith("RISK01") || pair.code().startsWith("RISK03")
                        ? ActiveEnum.TEMPO_BREAK
                        : pair.code().startsWith("RISK07") ? ActiveEnum.KNOCKBACK : null;
                    if (rarity == ItemRarity.LEGENDARY && disabled != null) {
                        result.add(scenario.withoutLeftSkill(disabled));
                    }
                }
            }
        }
    }

    record Scenario(
        String code,
        String family,
        int level,
        List<Build> left,
        List<Build> right,
        String seedGroup,
        int iterations,
        Set<ActiveEnum> disabledLeft,
        Set<ActiveEnum> disabledRight
    ) {
        Scenario {
            left = List.copyOf(left);
            right = List.copyOf(right);
            disabledLeft = Set.copyOf(disabledLeft);
            disabledRight = Set.copyOf(disabledRight);
            if (left.size() != right.size() || left.isEmpty() || iterations < 2 || level < 0) {
                throw new IllegalArgumentException("Invalid representative scenario");
            }
        }

        static Scenario of(String code, String family, int level, List<Build> left, List<Build> right, int iterations) {
            return new Scenario(code + "_L" + level, family, level, left, right, code, iterations, Set.of(), Set.of());
        }

        Scenario withSeedGroup(String group) {
            return new Scenario(code, family, level, left, right, group, iterations, disabledLeft, disabledRight);
        }

        Scenario withoutLeftSkill(ActiveEnum skill) {
            return new Scenario(code + "_OFF_" + skill, family, level, left, right,
                seedGroup, iterations, Set.of(skill), disabledRight);
        }
    }
}
