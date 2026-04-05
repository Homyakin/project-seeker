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
    /**
     * Total raid gold bonus percent = level × this value (Shadow Shop). Display-only until raid payout uses it.
     */
    private int shadowShopRaidGoldPercentPerLevel = 1;

    @NotEmpty
    private Map<Building, BuildingLevelMaterials> building;

    public int getShadowShopRaidGoldPercentPerLevel() {
        return shadowShopRaidGoldPercentPerLevel;
    }

    public void setShadowShopRaidGoldPercentPerLevel(int shadowShopRaidGoldPercentPerLevel) {
        if (shadowShopRaidGoldPercentPerLevel < 0) {
            throw new IllegalStateException("shadowShopRaidGoldPercentPerLevel must be >= 0");
        }
        this.shadowShopRaidGoldPercentPerLevel = shadowShopRaidGoldPercentPerLevel;
    }

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

    public int slotsByMonolithLevel(int monolithLevel) {
        if (monolithLevel < 2) {
            return 1;
        } else {
            return 2;
        }
    }
}
