package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldRaidTemplateBattleInfo(
    List<PersonageTemplate> personageTemplates
) {
    public List<PersonageTemplate> personageTemplatesOrEmpty() {
        return personageTemplates == null ? List.of() : personageTemplates;
    }

    public boolean hasPersonageTemplates() {
        return !personageTemplatesOrEmpty().isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonageTemplate(
        int count,
        WorldRaidPersonage personage,
        List<WorldRaidPersonage.PersonageSkill> skills,
        int health,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat,
        List<WorldRaidPersonage.AttackTemplate> attacks,
        List<WorldRaidPersonage.DefenseTemplate> defenses
    ) {
        public WorldRaidPersonage toPersonage() {
            if (personage != null) {
                return personage;
            }
            final var resolvedAttacks = attacks == null ? List.<WorldRaidPersonage.AttackTemplate>of() : attacks;
            final var resolvedDefenses = defenses == null ? List.<WorldRaidPersonage.DefenseTemplate>of() : defenses;
            final var resolvedSkills = skills == null ? List.<WorldRaidPersonage.PersonageSkill>of() : skills;
            if (
                resolvedSkills.isEmpty()
                    && resolvedAttacks.isEmpty()
                    && resolvedDefenses.isEmpty()
                    && health == 0
                && critChance == 0
                && dodgeChance == 0
                && critMultiplier == 0
                && speed == 0
                && baseThreat == 0
            ) {
                throw new IllegalStateException("World raid template personage must be present");
            }
            return new WorldRaidPersonage(
                resolvedSkills,
                health,
                critChance,
                dodgeChance,
                critMultiplier,
                speed,
                baseThreat,
                resolvedAttacks,
                resolvedDefenses
            );
        }
    }
}
