package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.ItemCatalogService;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.power.PowerDisplayService;
import ru.homyakin.seeker.game.personage.power.PowerDisplayVersion;

/** Coordinates only an explicitly selected release; it never infers combat rules from individual items. */
@Service
public class EquipmentReleaseCoordinator {
    private final EquipmentReleaseConfig config;
    private final PlayerCombatConfig playerCombatConfig;
    private final PowerDisplayService powerDisplayService;
    private final ItemCatalogService itemCatalogService;
    private final ItemCatalogLifecycleDao lifecycleDao;

    public EquipmentReleaseCoordinator(
        EquipmentReleaseConfig config,
        PlayerCombatConfig playerCombatConfig,
        PowerDisplayService powerDisplayService,
        ItemCatalogService itemCatalogService,
        ItemCatalogLifecycleDao lifecycleDao
    ) {
        this.config = config;
        this.playerCombatConfig = playerCombatConfig;
        this.powerDisplayService = powerDisplayService;
        this.itemCatalogService = itemCatalogService;
        this.lifecycleDao = lifecycleDao;
    }

    public LoadedEquipmentCatalog selectedCatalog() {
        return EquipmentCatalogLoader.loadValidated(config.catalogVersion());
    }

    @Transactional
    public void prepare(List<ItemObject> previousItemObjects, List<Modifier> previousModifiers) {
        // Validate resources and requested activation before the first database operation.
        final var catalog = selectedCatalog();
        final var version = catalog.release().catalogVersion();
        if (config.activateOnStartup()) {
            if (version != EquipmentCatalogVersion.SCALING_V5) {
                throw new IllegalStateException("Only the reviewed SCALING_V5 candidate may be activated by startup configuration");
            }
            requireCompatibleRuntime(version);
        }
        final var activeVersions = lifecycleDao.getActiveVersions();
        if (activeVersions.stream().anyMatch(active -> active != version)) {
            throw new IllegalStateException(
                "Refusing to overwrite an active catalog %s with %s; upgrades and rollback require an explicit procedure"
                    .formatted(activeVersions, version)
            );
        }
        if (activeVersions.contains(version)
            && (version == EquipmentCatalogVersion.SCALING_V4 || version == EquipmentCatalogVersion.SCALING_V5)) {
            // Resetting the startup activation switch must not silently restore legacy combat/display rules.
            requireCompatibleRuntime(version);
        }
        itemCatalogService.stageRelease(version, previousItemObjects, previousModifiers);
        if (config.activateOnStartup()) {
            itemCatalogService.activateRelease(version);
        }
    }

    private void requireCompatibleRuntime(EquipmentCatalogVersion version) {
        if (playerCombatConfig.version() != SkillFormulaVersion.SCALING_SKILLS_V2) {
            throw new IllegalStateException(version + " requires player combat SCALING_SKILLS_V2");
        }
        if (powerDisplayService.version() != PowerDisplayVersion.REFERENCE_V1) {
            throw new IllegalStateException(version + " requires reference power display REFERENCE_V1");
        }
    }
}
