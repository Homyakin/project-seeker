package ru.homyakin.seeker.game.item.database;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;

@Component
public class ItemCatalogLifecycleDao {
    private static final String STAGED = "STAGED";
    private static final String ACTIVE = "ACTIVE";

    private final JdbcClient jdbcClient;

    public ItemCatalogLifecycleDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public boolean registerStagedIfMissing(EquipmentCatalogVersion version) {
        return jdbcClient.sql(REGISTER_STAGED_SQL)
            .param("code", version.name())
            .param("status", STAGED)
            .update() == 1;
    }

    public boolean isActive(EquipmentCatalogVersion version) {
        return jdbcClient.sql(GET_STATUS_SQL)
            .param("code", version.name())
            .query(String.class)
            .optional()
            .map(ACTIVE::equals)
            .orElseThrow(() -> new IllegalStateException("Catalog release is not registered: " + version));
    }

    public boolean activateIfStaged(EquipmentCatalogVersion version) {
        final int changed = jdbcClient.sql(ACTIVATE_IF_STAGED_SQL)
            .param("code", version.name())
            .param("staged", STAGED)
            .param("active", ACTIVE)
            .update();
        if (changed == 1) {
            return true;
        }
        if (isActive(version)) {
            return false;
        }
        throw new IllegalStateException("Unsupported catalog release state: " + version);
    }

    public CatalogBackupState getCatalogBackupState() {
        return jdbcClient.sql(GET_CATALOG_BACKUP_STATE_SQL)
            .query((rs, _) -> new CatalogBackupState(
                rs.getInt("item_object_backup_count"),
                rs.getInt("item_modifier_backup_count"),
                rs.getInt("item_object_count"),
                rs.getInt("item_modifier_count")
            ))
            .single();
    }

    public int snapshotItemObjects() {
        return jdbcClient.sql(SNAPSHOT_ITEM_OBJECTS_SQL).update();
    }

    public int snapshotItemModifiers() {
        return jdbcClient.sql(SNAPSHOT_ITEM_MODIFIERS_SQL).update();
    }

    private static final String REGISTER_STAGED_SQL = """
        INSERT INTO item_catalog_release (code, status)
        VALUES (:code, :status)
        ON CONFLICT (code) DO NOTHING
        """;

    private static final String GET_STATUS_SQL = """
        SELECT status
        FROM item_catalog_release
        WHERE code = :code
        """;

    private static final String ACTIVATE_IF_STAGED_SQL = """
        UPDATE item_catalog_release
        SET status = :active
        WHERE code = :code AND status = :staged
        """;

    private static final String GET_CATALOG_BACKUP_STATE_SQL = """
        SELECT
            (SELECT count(*) FROM item_object_catalog_lifecycle_backup) AS item_object_backup_count,
            (SELECT count(*) FROM item_modifier_catalog_lifecycle_backup) AS item_modifier_backup_count,
            (SELECT count(*) FROM item_object) AS item_object_count,
            (SELECT count(*) FROM item_modifier) AS item_modifier_count
        """;

    private static final String SNAPSHOT_ITEM_OBJECTS_SQL = """
        INSERT INTO item_object_catalog_lifecycle_backup (
            id, code, health, crit_chance, dodge_chance, crit_multiplier, speed, base_threat,
            attack_type, attack_range, attack, defense_type, defense, locale, personage_slot_ids,
            attack_parts, impact, progression_version
        )
        SELECT id, code, health, crit_chance, dodge_chance, crit_multiplier, speed, base_threat,
               attack_type, attack_range, attack, defense_type, defense, locale, personage_slot_ids,
               attack_parts, impact, progression_version
        FROM item_object
        """;

    private static final String SNAPSHOT_ITEM_MODIFIERS_SQL = """
        INSERT INTO item_modifier_catalog_lifecycle_backup (
            id, code, active_enum, type_id, locale, personage_slot_ids
        )
        SELECT id, code, active_enum, type_id, locale, personage_slot_ids
        FROM item_modifier
        """;

    public record CatalogBackupState(
        int itemObjectBackupCount,
        int itemModifierBackupCount,
        int itemObjectCount,
        int itemModifierCount
    ) {
        public boolean hasSnapshot() {
            return itemObjectBackupCount > 0 && itemModifierBackupCount > 0;
        }

        public boolean isFreshEmptyCatalog() {
            return itemObjectBackupCount == 0
                && itemModifierBackupCount == 0
                && itemObjectCount == 0
                && itemModifierCount == 0;
        }
    }
}
