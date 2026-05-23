package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldRaidPersonage(
    List<PersonageSkill> skills,
    int health,
    int critChance,
    int dodgeChance,
    double critMultiplier,
    int speed,
    int baseThreat,
    List<AttackTemplate> attacks,
    List<DefenseTemplate> defenses,
    Position position
) {
    public List<PersonageSkill> skillsOrEmpty() {
        return skills == null ? List.of() : skills;
    }

    public List<AttackTemplate> attacksOrEmpty() {
        return attacks == null ? List.of() : attacks;
    }

    public List<DefenseTemplate> defensesOrEmpty() {
        return defenses == null ? List.of() : defenses;
    }

    public WorldRaidPersonage withHealth(int health) {
        return new WorldRaidPersonage(
            skills,
            health,
            critChance,
            dodgeChance,
            critMultiplier,
            speed,
            baseThreat,
            attacks,
            defenses,
            position
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonageSkill(
        ActiveEnum activeEnum,
        SkillRank rank
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AttackTemplate(
        AttackType attackType,
        int range,
        int attack
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DefenseTemplate(
        DefenseType defenseType,
        int defense
    ) {
    }
}
