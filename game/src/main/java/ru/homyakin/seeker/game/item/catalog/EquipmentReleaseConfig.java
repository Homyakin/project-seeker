package ru.homyakin.seeker.game.item.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** A prepared catalog and its explicit startup activation switch. Defaults preserve the staged first release. */
@ConfigurationProperties(prefix = "homyakin.seeker.equipment-release")
public class EquipmentReleaseConfig {
    private EquipmentCatalogVersion catalogVersion = EquipmentCatalogVersion.SCALING_V1;
    private boolean activateOnStartup;

    public EquipmentCatalogVersion catalogVersion() {
        return catalogVersion;
    }

    public void setCatalogVersion(EquipmentCatalogVersion catalogVersion) {
        if (catalogVersion == null) {
            throw new IllegalArgumentException("Equipment catalog version must be specified");
        }
        this.catalogVersion = catalogVersion;
    }

    public boolean activateOnStartup() {
        return activateOnStartup;
    }

    public void setActivateOnStartup(boolean activateOnStartup) {
        this.activateOnStartup = activateOnStartup;
    }
}
