package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorldRaidLaunchedBattleInfo(
    List<WorldRaidPersonage> personages
) {
    public List<WorldRaidPersonage> personagesOrEmpty() {
        return personages == null ? List.of() : personages;
    }

    public boolean hasPersonages() {
        return !personagesOrEmpty().isEmpty();
    }

    public int totalHealth() {
        return personagesOrEmpty().stream().mapToInt(WorldRaidPersonage::health).sum();
    }

    public int enemiesCount() {
        return personagesOrEmpty().size();
    }
}
