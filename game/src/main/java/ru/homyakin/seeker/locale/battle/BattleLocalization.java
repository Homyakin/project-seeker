package ru.homyakin.seeker.locale.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class BattleLocalization {
    private static final Resources<BattleResource> resources = new Resources<>();

    public static void add(Language language, BattleResource resource) {
        resources.add(language, resource);
    }

    public static String battleStats(Language language, BattlePersonage personage, List<Item> equippedItems) {
        final var params = new HashMap<String, Object>();
        params.put("position_name", positionName(language, personage.startPosition()));
        params.put("battle_position_command", CommandType.CHANGE_BATTLE_POSITION.getText());
        params.put("power_icon", Icons.POWER);
        params.put("power_value", LocaleUtils.power((int) personage.power()));
        params.put("health_icon", Icons.HEALTH);
        params.put("health_value", personage.maxHealth());
        params.put("range_icon", Icons.RANGE);
        params.put("range_value", personage.range());
        params.put("speed_icon", Icons.SPEED);
        params.put("speed_value", personage.initiative());
        params.put("crit_attack_icon", Icons.CRIT_ATTACK);
        params.put("crit_chance_value", personage.critChance());
        params.put("dodge_icon", Icons.DODGE);
        params.put("dodge_chance_value", personage.dodgeChance());
        params.put("crit_multiplier_icon", Icons.CRIT_MULTIPLIER);
        params.put("crit_multiplier_value", formatCritMultiplier(personage.critMultiplier()));
        params.put("threat_icon", Icons.THREAT);
        params.put("threat_value", personage.totalThreat());
        params.put("attack_lines", formatAttackLines(language, personage));
        params.put("defense_lines", formatDefenseLines(language, personage));
        params.put("mitigation_lines", formatMitigationLines(language, personage));
        params.put("skills_section", formatSkillsSection(language, equippedItems));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BattleResource::battleStats),
            params
        );
    }

    public static String chooseBattlePosition(Language language) {
        return resources.getOrDefault(language, BattleResource::chooseBattlePosition);
    }

    public static String battleVisualizerButton(Language language) {
        return resources.getOrDefault(language, BattleResource::battleVisualizerButton);
    }

    public static String positionName(Language language, Position position) {
        return switch (position) {
            case FRONT -> resources.getOrDefault(language, BattleResource::positionFront);
            case MID -> resources.getOrDefault(language, BattleResource::positionMid);
            case BACK -> resources.getOrDefault(language, BattleResource::positionBack);
        };
    }

    public static String skillDescription(Language language, ActiveEnum activeEnum, int points) {
        final var rank = SkillRank.forPoints(points);
        final var entry = skillEntry(language, activeEnum);
        final var description = switch (rank) {
            case FIRST -> entry.first();
            case SECOND -> entry.second();
            case THIRD -> entry.third();
            case FOURTH -> entry.fourth();
            case FIFTH -> entry.fifth();
        };
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BattleResource::battleStatsSkillLine),
            Map.of(
                "skill_name", entry.name(),
                "skill_rank", rankLabel(rank),
                "skill_points", points,
                "skill_description", description
            )
        );
    }

    private static String formatCritMultiplier(double critMultiplier) {
        return String.format("%.1f", critMultiplier).replace(',', '.');
    }

    private static String formatAttackLines(Language language, BattlePersonage personage) {
        if (personage.attackTypes().isEmpty()) {
            return "";
        }
        final var lines = new StringBuilder();
        for (int distance = 1; distance <= personage.range(); distance++) {
            final var attacks = personage.attackAtRange(distance);
            if (attacks.isEmpty()) {
                continue;
            }
            for (final var entry : attacks.entrySet()) {
                lines.append(StringNamedTemplate.format(
                    resources.getOrDefault(language, BattleResource::battleStatsAttackLine),
                    Map.of(
                        "attack_icon", Icons.ATTACK,
                        "attack_type", attackTypeName(language, entry.getKey()),
                        "range_value", distance,
                        "attack_value", entry.getValue()
                    )
                )).append('\n');
            }
        }
        return lines.toString();
    }

    private static String formatDefenseLines(Language language, BattlePersonage personage) {
        final var defenses = personage.defenses();
        if (defenses.isEmpty()) {
            return "";
        }
        return defenses.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> StringNamedTemplate.format(
                resources.getOrDefault(language, BattleResource::battleStatsDefenseLine),
                Map.of(
                    "defense_icon", Icons.DEFENSE,
                    "defense_type", defenseTypeName(language, entry.getKey()),
                    "defense_value", entry.getValue()
                )
            ))
            .collect(Collectors.joining("\n")) + "\n";
    }

    private static String formatMitigationLines(Language language, BattlePersonage personage) {
        return personage.defenseReductions().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> StringNamedTemplate.format(
                resources.getOrDefault(language, BattleResource::battleStatsMitigationLine),
                Map.of(
                    "attack_type", attackTypeName(language, entry.getKey()),
                    "mitigation_percent", Math.round((1.0 - entry.getValue()) * 100)
                )
            ))
            .collect(Collectors.joining("\n")) + "\n";
    }

    private static String formatSkillsSection(Language language, List<Item> equippedItems) {
        final var skillPoints = collectSkillPoints(equippedItems);
        if (skillPoints.isEmpty()) {
            return resources.getOrDefault(language, BattleResource::battleStatsSkillsEmpty);
        }
        final var lines = new ArrayList<String>();
        skillPoints.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> lines.add(skillDescription(
                language,
                entry.getKey(),
                entry.getValue()
            )));
        return String.join("\n", lines) + "\n";
    }

    private static Map<ActiveEnum, Integer> collectSkillPoints(List<Item> equippedItems) {
        final var skillPoints = new HashMap<ActiveEnum, Integer>();
        for (final var item : equippedItems) {
            if (item.modifier().isEmpty() || item.rarity() == ItemRarity.COMMON) {
                continue;
            }
            skillPoints.merge(
                item.modifier().get().activeEnum(),
                item.skillPoints(),
                Integer::sum
            );
        }
        return skillPoints;
    }

    private static BattleResource.SkillEntry skillEntry(Language language, ActiveEnum activeEnum) {
        final var resource = resources.resolve(language);
        return switch (activeEnum) {
            case COUNTER_ATTACK -> resource.counterAttack();
            case THORNS -> resource.thorns();
            case DOUBLE_ATTACK -> resource.doubleAttack();
            case BERSERK -> resource.berserk();
            case HIT_AND_RUN -> resource.hitAndRun();
            case BLEEDING -> resource.bleeding();
            case KNOCKBACK -> resource.knockback();
            case SELF_HEAL -> resource.selfHeal();
            case PRECISE_STRIKE -> resource.preciseStrike();
            case RETREAT -> resource.retreat();
            case FEINT -> resource.feint();
        };
    }

    private static String attackTypeName(Language language, AttackType attackType) {
        return switch (attackType) {
            case SLASH -> resources.getOrDefault(language, BattleResource::attackTypeSlash);
            case BLUNT -> resources.getOrDefault(language, BattleResource::attackTypeBlunt);
            case PIERCE -> resources.getOrDefault(language, BattleResource::attackTypePierce);
            case MAGICAL -> resources.getOrDefault(language, BattleResource::attackTypeMagical);
        };
    }

    private static String defenseTypeName(Language language, DefenseType defenseType) {
        return switch (defenseType) {
            case CLOTH -> resources.getOrDefault(language, BattleResource::defenseTypeCloth);
            case LEATHER -> resources.getOrDefault(language, BattleResource::defenseTypeLeather);
            case PLATE -> resources.getOrDefault(language, BattleResource::defenseTypePlate);
            case ARCANE -> resources.getOrDefault(language, BattleResource::defenseTypeArcane);
        };
    }

    private static String rankLabel(SkillRank rank) {
        return switch (rank) {
            case FIRST -> "I";
            case SECOND -> "II";
            case THIRD -> "III";
            case FOURTH -> "IV";
            case FIFTH -> "V";
        };
    }
}
