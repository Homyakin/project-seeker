package ru.homyakin.seeker.game.outpost;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Validated
@ConfigurationProperties("homyakin.seeker.outpost")
public class OutpostBuildingConfig {
    @NotEmpty
    private Map<Building, BuildingLevelMaterials> building;

    public Map<Building, BuildingLevelMaterials> getBuilding() {
        return building;
    }

    public void setBuilding(Map<Building, BuildingLevelMaterials> building) {
        for (final var entry : building.entrySet()) {
            final var currentBuilding = entry.getKey();
            final var currentMaterials = entry.getValue();
            if (currentMaterials.getMaterials().size() != currentBuilding.maxLevel()) {
                throw new IllegalStateException(
                    "Outpost config: building " + currentBuilding + " must have exactly " + currentBuilding.maxLevel()
                        + " materials entries (levels 1.." + currentBuilding.maxLevel() + "), got "
                        + currentMaterials.getMaterials().size()
                );
            }
            for (int i = 0; i < currentMaterials.getMaterials().size(); i++) {
                final var materials = currentMaterials.getMaterials().get(i);
                if (materials == null || materials <= 0) {
                    throw new IllegalStateException(
                        "Outpost config: building " + currentBuilding + " level " + (i + 1)
                            + " must have a positive materials amount, got " + materials
                    );
                }
            }
        }
        this.building = building;
    }

    public int materialsToReachLevel(Building building, int targetLevel, int taxMultiplier) {
        final var entry = this.building.get(building);
        if (entry == null) {
            throw new IllegalStateException("No outpost materials configured for " + building);
        }
        return entry.materialsForLevel(targetLevel) * taxMultiplier;
    }

    public static class BuildingLevelMaterials {
        private List<Integer> materials;

        public List<Integer> getMaterials() {
            return materials;
        }

        public int materialsForLevel(int level) {
            if (level < 1 || level > materials.size()) {
                throw new IllegalArgumentException("Level " + level + " out of range for building");
            }
            return materials.get(level - 1);
        }

        public void setMaterials(List<Integer> materials) {
            this.materials = materials;
        }
    }
}
