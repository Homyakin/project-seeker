package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Obtainable complete builds used to look for unanswered control, sustain and burst damage. */
final class Step12RepresentativeRisks {
    private Step12RepresentativeRisks() {
    }

    static List<RiskPair> pairs(ItemRarity rarity) {
        final var tempo = ranked("T", rarity);
        final var breaker = ranked("B", rarity);
        final var blocker = ranked("K", rarity);
        final var ranger = ranked("D", rarity);
        final var mage = ranked("M", rarity);
        final var assassin = ranked("P", rarity);
        final var guardian = ranked("G", rarity);
        final var bruiser = ranked("C", rarity);
        final var protectedRanger = behindGuard(ranger);
        final var protectedMage = behindGuard(mage);
        final var protectedBreaker = behindGuard(breaker);
        final var massTempo = List.of(
            guardian, tempo, tempo, tempo, protectedRanger, protectedRanger, protectedRanger
        );
        final var rearGuard = guardian.replaceItem("mace", "spear")
            .renamed("G_REAR_GUARD_" + rarity).withPosition(Position.MID);
        final var guardChain = List.of(guardian, rearGuard, ranger);
        final var knockback = List.of(blocker, blocker, guardian);
        final var accumulation = List.of(guardian, protectedBreaker, protectedBreaker);
        return List.of(
            new RiskPair(
                "RISK01_TEMPO_MIXED",
                List.of(tempo, tempo, tempo),
                List.of(guardian, bruiser, protectedRanger)
            ),
            new RiskPair(
                "RISK02_TEMPO_ACCESS_RESPONSE",
                List.of(tempo, tempo, tempo),
                List.of(guardian, protectedMage, assassin)
            ),
            new RiskPair(
                "RISK03_MASS_TEMPO_MIXED",
                massTempo,
                List.of(guardian, guardian, bruiser, bruiser, protectedRanger, protectedRanger, assassin)
            ),
            new RiskPair(
                "RISK04_MASS_TEMPO_ACCESS_RESPONSE",
                massTempo,
                List.of(guardian, guardian, assassin, assassin, protectedMage, protectedMage, protectedRanger)
            ),
            new RiskPair(
                "RISK05_GUARD_REACTIONS_MELEE",
                guardChain,
                List.of(bruiser, bruiser, ranger)
            ),
            new RiskPair(
                "RISK06_GUARD_MAGIC_ACCESS_RESPONSE",
                guardChain,
                List.of(assassin, mage, mage)
            ),
            new RiskPair(
                "RISK07_KNOCKBACK_MELEE",
                knockback,
                List.of(bruiser, bruiser, guardian)
            ),
            new RiskPair(
                "RISK08_KNOCKBACK_RANGE_RESPONSE",
                knockback,
                List.of(protectedRanger, protectedMage, guardian)
            ),
            new RiskPair(
                "RISK09_ACCUMULATION_DAMAGE",
                accumulation,
                List.of(guardian, protectedRanger, protectedRanger)
            ),
            new RiskPair(
                "RISK10_ACCUMULATION_ACCESS_RESPONSE",
                accumulation,
                List.of(guardian, assassin, assassin)
            )
        );
    }

    static Build ranked(String template, ItemRarity rarity) {
        return switch (template) {
            case "T" -> equipped(
                "T_TEMPO", List.of("halberd"), "ARCANE", Position.MID,
                TargetingTactic.INITIATIVE_INTERCEPTION, rarity, Map.of("halberd", ActiveEnum.TEMPO_BREAK)
            );
            case "B" -> equipped(
                "B_ACCUMULATION", List.of("crossbow"), "ARCANE", Position.BACK,
                TargetingTactic.EXECUTIONER, rarity, Map.of("crossbow", ActiveEnum.ACCUMULATION)
            );
            case "K" -> equipped(
                "K_KNOCKBACK", List.of("bow"), "CLOTH", Position.BACK,
                TargetingTactic.EXPLOIT_WEAKNESS, rarity, Map.of("bow", ActiveEnum.KNOCKBACK)
            );
            case "D" -> equipped(
                "D_RANGER", List.of("longbow"), "CLOTH", Position.BACK,
                TargetingTactic.EXPLOIT_WEAKNESS, rarity, Map.of("longbow", ActiveEnum.DOUBLE_ATTACK)
            );
            case "M" -> equipped(
                "M_MAGE", List.of("battle_staff"), "CLOTH", Position.BACK,
                TargetingTactic.EXPLOIT_WEAKNESS, rarity, Map.of("battle_staff", ActiveEnum.DOUBLE_ATTACK)
            );
            case "P" -> equipped(
                "P_PENETRATION", List.of("rapier", "dagger"), "ARCANE", Position.FRONT,
                TargetingTactic.EXECUTIONER, rarity,
                Map.of("rapier", ActiveEnum.PENETRATION, "dagger", ActiveEnum.PRECISE_STRIKE)
            );
            case "G" -> equipped(
                "G_GUARD", List.of("mace", "tower_shield"), "PLATE", Position.FRONT,
                TargetingTactic.THREAT, rarity,
                Map.of("mace", ActiveEnum.BERSERK, "tower_shield", ActiveEnum.GUARD)
            );
            case "C" -> equipped(
                "C_BRUISER", List.of("sledgehammer"), "LEATHER", Position.FRONT,
                TargetingTactic.WOUNDED_HUNTER, rarity, Map.of("sledgehammer", ActiveEnum.BERSERK)
            );
            default -> throw new IllegalArgumentException("Unknown representative risk template: " + template);
        };
    }

    /** A front-line guard can intercept attacks against the immediately following middle line. */
    static Build behindGuard(Build build) {
        return build.withPosition(Position.MID);
    }

    private static Build equipped(
        String code,
        List<String> hands,
        String armorFamily,
        Position position,
        TargetingTactic tactic,
        ItemRarity rarity,
        Map<String, ActiveEnum> handSkills
    ) {
        final var armor = Step12RepresentativeBuilds.armor(armorFamily);
        final var items = new ArrayList<>(hands);
        items.addAll(armor);
        final var assignments = new LinkedHashMap<String, Assignment>();
        handSkills.forEach((carrier, skill) -> assignments.put(carrier, new Assignment(skill, rarity)));
        assignments.put(armor.get(0), new Assignment(ActiveEnum.SELF_HEAL, rarity));
        assignments.put(armor.get(1), new Assignment(ActiveEnum.SELF_HEAL, rarity));
        assignments.put(armor.get(2), new Assignment(ActiveEnum.RETREAT, rarity));
        assignments.put(armor.get(3), new Assignment(ActiveEnum.COUNTER_ATTACK, rarity));
        assignments.put(armor.get(4), new Assignment(ActiveEnum.THORNS, rarity));
        if (handSkills.containsValue(ActiveEnum.GUARD)) {
            // Shield plus body give Guard 8/4; pants retain SelfHeal 4/2, at the same 28/14 total budget.
            assignments.put(armor.get(0), new Assignment(ActiveEnum.GUARD, rarity));
        }
        return Step12RepresentativeBuilds.custom(code + "_" + rarity.name(), items, position, tactic, assignments);
    }

    record RiskPair(String code, List<Build> left, List<Build> right) {
        RiskPair {
            left = List.copyOf(left);
            right = List.copyOf(right);
            if (left.size() != right.size() || left.isEmpty()) {
                throw new IllegalArgumentException("Representative risk teams must have the same nonzero size");
            }
        }
    }
}
