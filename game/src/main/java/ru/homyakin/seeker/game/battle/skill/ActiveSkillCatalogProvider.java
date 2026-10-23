package ru.homyakin.seeker.game.battle.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;

/** Gives help one immutable availability snapshot backed by the actually active selected release. */
@Component
public final class ActiveSkillCatalogProvider {
    private static final Map<EquipmentCatalogVersion, ActiveSkillSlots.CatalogSnapshot> CATALOGS = new ConcurrentHashMap<>();
    private final EquipmentReleaseConfig config;
    private final ItemCatalogLifecycleDao lifecycleDao;

    public ActiveSkillCatalogProvider(EquipmentReleaseConfig config, ItemCatalogLifecycleDao lifecycleDao) {
        this.config = config;
        this.lifecycleDao = lifecycleDao;
    }

    public ActiveSkillSlots.CatalogSnapshot snapshot() {
        final var version = config.catalogVersion();
        if (!lifecycleDao.getActiveVersions().contains(version)) {
            return ActiveSkillSlots.legacySnapshot();
        }
        return CATALOGS.computeIfAbsent(version, ActiveSkillSlots::snapshotForVersion);
    }
}
