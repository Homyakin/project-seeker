package ru.homyakin.seeker.game.item;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.PersonageConfig;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.utils.JsonUtils;

@EnabledIfEnvironmentVariable(named = "SEEKER_POSTGRES_TEST_URL", matches = ".+")
class EquipmentCatalogPostgresIntegrationTest {
    private static final int CHANGES_BEFORE_CATALOG_LIFECYCLE = 123;

    private PGSimpleDataSource adminDataSource;
    private PGSimpleDataSource schemaDataSource;
    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        schema = "catalog_step_11_" + UUID.randomUUID().toString().replace("-", "");
        adminDataSource = dataSource(System.getenv("SEEKER_POSTGRES_TEST_URL"));
        executeAdmin("CREATE SCHEMA " + schema);
        schemaDataSource = dataSource(System.getenv("SEEKER_POSTGRES_TEST_URL"));
        schemaDataSource.setCurrentSchema(schema);
        migrateBeforeCatalogLifecycle();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (adminDataSource != null && schema != null) {
            executeAdmin("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void stagesAndActivatesReleaseWithoutBreakingExistingItem() throws Exception {
        final var jsonUtils = new JsonUtils();
        final var previousObjects = load("game-data/item_objects_catalog.toml", ItemObjectsToml::load);
        final var previousModifiers = load("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load);
        final var previousItemCodes = previousObjects.itemObjects().stream()
            .map(ItemObject::code)
            .collect(Collectors.toUnmodifiableSet());
        final var previousModifierCodes = previousModifiers.modifiers().stream()
            .map(Modifier::code)
            .collect(Collectors.toUnmodifiableSet());
        seedPreviousCatalog(previousObjects, previousModifiers, jsonUtils);
        final var personageDao = new PersonageDao(
            schemaDataSource,
            jsonUtils,
            new PersonageConfig(Duration.ofHours(8), 200, 5)
        );
        final var jdbc = new JdbcTemplate(schemaDataSource);
        final int swordId = idForCode(jdbc, "item_object", "sword");
        final int daggerId = idForCode(jdbc, "item_object", "dagger");
        final int historicalModifierId = idForCode(jdbc, "item_modifier", "cunning");
        final var previousSword = previousObjects.itemObjects().stream()
            .filter(item -> item.code().equals("sword"))
            .findFirst()
            .orElseThrow();
        final long ownerId = personageDao.createDefault("Catalog Migration").value();
        final long itemId = jdbc.queryForObject("""
                INSERT INTO item (
                    item_object_id, item_modifier_id, rarity, personage_id, is_equipped,
                    enhance_level, enhance_revision
                ) VALUES (?, ?, ?, ?, true, 6, 3)
                RETURNING id
                """, Long.class, swordId, historicalModifierId, ItemRarity.RARE.ordinal(), ownerId);
        final long bagItemId = jdbc.queryForObject("""
                INSERT INTO item (
                    item_object_id, item_modifier_id, rarity, personage_id, is_equipped,
                    enhance_level, enhance_revision
                ) VALUES (?, NULL, ?, ?, false, 3, 5)
                RETURNING id
                """, Long.class, swordId, ItemRarity.COMMON.ordinal(), ownerId);
        final long legacySmallBaseItemId = jdbc.queryForObject("""
                INSERT INTO item (
                    item_object_id, item_modifier_id, rarity, personage_id, is_equipped,
                    enhance_level, enhance_revision
                ) VALUES (?, NULL, ?, ?, false, 1, 2)
                RETURNING id
                """, Long.class, daggerId, ItemRarity.COMMON.ordinal(), ownerId);
        migrate();

        final var objectDao = new ItemObjectDao(schemaDataSource, jsonUtils);
        final var modifierDao = new ItemModifierDao(schemaDataSource, jsonUtils);
        final var itemDao = new ItemDao(schemaDataSource, objectDao, modifierDao);
        final var lifecycleDao = new ItemCatalogLifecycleDao(schemaDataSource);
        final var service = new ItemCatalogService(objectDao, modifierDao, lifecycleDao);
        final int previousSwordAttackAtSix = itemDao.getById(itemId).orElseThrow()
            .toItem()
            .itemAttacks()
            .getFirst()
            .attack();
        Assertions.assertEquals(ItemProgressionVersion.LEGACY, previousSword.progressionVersion());
        Assertions.assertTrue(itemDao.getById(legacySmallBaseItemId).isPresent());

        inTransaction(() -> service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousObjects.itemObjects(),
            previousModifiers.modifiers()
        ));

        Assertions.assertAll(
            () -> Assertions.assertEquals(62, count(jdbc, "item_object")),
            () -> Assertions.assertEquals(15, count(jdbc, "item_modifier")),
            () -> Assertions.assertEquals(40, countEnabled(jdbc, "item_object", "acquisition_enabled")),
            () -> Assertions.assertEquals(11, countEnabled(jdbc, "item_modifier", "assignment_enabled")),
            () -> Assertions.assertEquals(swordId, idForCode(jdbc, "item_object", "sword"))
        );

        final var migrated = itemDao.getById(itemId).orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(6, migrated.enhanceLevel()),
            () -> Assertions.assertEquals(4, migrated.enhanceRevision()),
            () -> Assertions.assertEquals(ItemRarity.RARE, migrated.rarity()),
            () -> Assertions.assertEquals(historicalModifierId, migrated.modifierId().orElseThrow()),
            () -> Assertions.assertEquals(ownerId, migrated.personageId().orElseThrow().value()),
            () -> Assertions.assertTrue(migrated.isEquipped()),
            () -> Assertions.assertEquals(ItemProgressionVersion.V1, migrated.object().progressionVersion()),
            () -> Assertions.assertEquals(396, migrated.toItem().itemAttacks().getFirst().attack())
        );
        final var migratedBagItem = itemDao.getById(bagItemId).orElseThrow();
        final var migratedSmallBaseItem = itemDao.getById(legacySmallBaseItemId).orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, migratedBagItem.enhanceLevel()),
            () -> Assertions.assertEquals(6, migratedBagItem.enhanceRevision()),
            () -> Assertions.assertEquals(ownerId, migratedBagItem.personageId().orElseThrow().value()),
            () -> Assertions.assertFalse(migratedBagItem.isEquipped())
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(1, migratedSmallBaseItem.enhanceLevel()),
            () -> Assertions.assertEquals(3, migratedSmallBaseItem.enhanceRevision()),
            () -> Assertions.assertEquals(122, migratedSmallBaseItem.toItem().itemAttacks().getFirst().attack()),
            () -> Assertions.assertNotEquals(396, previousSwordAttackAtSix)
        );

        final int stagedId = idForCode(jdbc, "item_object", "ceremonial_tunic");
        final int stagedModifierId = idForCode(jdbc, "item_modifier", "guarding");
        Assertions.assertTrue(objectDao.getById(stagedId).isPresent());
        Assertions.assertTrue(objectDao.getAvailableById(stagedId).isEmpty());
        Assertions.assertTrue(modifierDao.getAvailableById(stagedModifierId).isEmpty());
        for (int attempt = 0; attempt < 100; attempt++) {
            Assertions.assertTrue(previousItemCodes.contains(
                objectDao.getRandomObject(PersonageSlot.BODY).object().code()
            ));
            Assertions.assertTrue(previousModifierCodes.contains(
                modifierDao.getRandomModifier(
                    PersonageSlot.MAIN_HAND,
                    Set.of(ModifierType.ATTACK, ModifierType.DEFENSE, ModifierType.ANY)
                ).modifier().code()
            ));
        }

        Assertions.assertTrue(itemDao.applyStormEnhance(itemId, migrated.personageId().orElseThrow(), 6, 4, 5));
        final var rolledBackEnhancement = itemDao.getById(itemId).orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(5, rolledBackEnhancement.enhanceLevel()),
            () -> Assertions.assertEquals(5, rolledBackEnhancement.enhanceRevision()),
            () -> Assertions.assertEquals(390, rolledBackEnhancement.toItem().itemAttacks().getFirst().attack())
        );

        inTransaction(() -> service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousObjects.itemObjects(),
            previousModifiers.modifiers()
        ));
        Assertions.assertEquals(5, itemDao.getById(itemId).orElseThrow().enhanceRevision());
        Assertions.assertEquals(40, countEnabled(jdbc, "item_object", "acquisition_enabled"));
        Assertions.assertEquals(11, countEnabled(jdbc, "item_modifier", "assignment_enabled"));

        inTransaction(() -> service.activateRelease(EquipmentCatalogVersion.SCALING_V1));
        Assertions.assertTrue(objectDao.getAvailableById(stagedId).isPresent());
        Assertions.assertTrue(modifierDao.getAvailableById(stagedModifierId).isPresent());
        assertArmorDistribution(objectDao);

        inTransaction(() -> service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousObjects.itemObjects(),
            previousModifiers.modifiers()
        ));
        Assertions.assertEquals(62, countEnabled(jdbc, "item_object", "acquisition_enabled"));
        Assertions.assertEquals(15, countEnabled(jdbc, "item_modifier", "assignment_enabled"));
        final long postSnapshotItemId = jdbc.queryForObject("""
                INSERT INTO item (
                    item_object_id, item_modifier_id, rarity, personage_id, is_equipped,
                    enhance_level, enhance_revision
                ) VALUES (?, ?, ?, ?, false, 0, 0)
                RETURNING id
                """, Long.class, stagedId, stagedModifierId, ItemRarity.RARE.ordinal(), ownerId);
        Assertions.assertThrows(Exception.class, this::rollbackLastChangeSet);
        Assertions.assertAll(
            () -> Assertions.assertEquals(4, count(jdbc, "item")),
            () -> Assertions.assertEquals(62, count(jdbc, "item_object")),
            () -> Assertions.assertEquals(15, count(jdbc, "item_modifier")),
            () -> Assertions.assertEquals(1,
                intValue(jdbc, "SELECT count(*) FROM item WHERE id = ?", postSnapshotItemId)),
            () -> Assertions.assertEquals(7, countLifecycleSchemaObjects(jdbc))
        );
        jdbc.update("DELETE FROM item WHERE id = ?", postSnapshotItemId);
        rollbackLastChangeSet();
        Assertions.assertAll(
            () -> Assertions.assertEquals(40, count(jdbc, "item_object")),
            () -> Assertions.assertEquals(11, count(jdbc, "item_modifier")),
            () -> Assertions.assertEquals(3, count(jdbc, "item")),
            () -> Assertions.assertEquals(previousSword.attacks().getFirst().attack(),
                intValue(jdbc, "SELECT attack FROM item_object WHERE id = ?", swordId)),
            () -> Assertions.assertEquals(previousSword.progressionVersion().name(),
                stringValue(jdbc, "SELECT progression_version FROM item_object WHERE id = ?", swordId)),
            () -> Assertions.assertEquals(5,
                intValue(jdbc, "SELECT enhance_level FROM item WHERE id = ?", itemId)),
            () -> Assertions.assertEquals(6,
                longValue(jdbc, "SELECT enhance_revision FROM item WHERE id = ?", itemId)),
            () -> Assertions.assertEquals(7,
                longValue(jdbc, "SELECT enhance_revision FROM item WHERE id = ?", bagItemId)),
            () -> Assertions.assertEquals(4,
                longValue(jdbc, "SELECT enhance_revision FROM item WHERE id = ?", legacySmallBaseItemId)),
            () -> Assertions.assertEquals(0, countLifecycleSchemaObjects(jdbc))
        );
    }

    @Test
    void freshDatabaseGetsRollbackBaselineBeforeStaging() throws Exception {
        final var jsonUtils = new JsonUtils();
        final var previousObjects = load("game-data/item_objects_catalog.toml", ItemObjectsToml::load);
        final var previousModifiers = load("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load);
        migrate();

        final var objectDao = new ItemObjectDao(schemaDataSource, jsonUtils);
        final var modifierDao = new ItemModifierDao(schemaDataSource, jsonUtils);
        final var lifecycleDao = new ItemCatalogLifecycleDao(schemaDataSource);
        final var service = new ItemCatalogService(objectDao, modifierDao, lifecycleDao);
        final var jdbc = new JdbcTemplate(schemaDataSource);

        inTransaction(() -> service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousObjects.itemObjects(),
            previousModifiers.modifiers()
        ));

        final int swordId = idForCode(jdbc, "item_object", "sword");
        final long itemId = jdbc.queryForObject("""
                INSERT INTO item (
                    item_object_id, item_modifier_id, rarity, personage_id, is_equipped,
                    enhance_level, enhance_revision
                ) VALUES (?, NULL, ?, NULL, false, 2, 0)
                RETURNING id
                """, Long.class, swordId, ItemRarity.COMMON.ordinal());
        Assertions.assertAll(
            () -> Assertions.assertEquals(40, count(jdbc, "item_object_catalog_lifecycle_backup")),
            () -> Assertions.assertEquals(11, count(jdbc, "item_modifier_catalog_lifecycle_backup")),
            () -> Assertions.assertEquals(62, count(jdbc, "item_object")),
            () -> Assertions.assertEquals(15, count(jdbc, "item_modifier"))
        );

        rollbackLastChangeSet();

        final var previousSword = previousObjects.itemObjects().stream()
            .filter(item -> item.code().equals("sword"))
            .findFirst()
            .orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(40, count(jdbc, "item_object")),
            () -> Assertions.assertEquals(11, count(jdbc, "item_modifier")),
            () -> Assertions.assertEquals(previousSword.attacks().getFirst().attack(),
                intValue(jdbc, "SELECT attack FROM item_object WHERE id = ?", swordId)),
            () -> Assertions.assertEquals(2,
                intValue(jdbc, "SELECT enhance_level FROM item WHERE id = ?", itemId)),
            () -> Assertions.assertEquals(1,
                longValue(jdbc, "SELECT enhance_revision FROM item WHERE id = ?", itemId)),
            () -> Assertions.assertEquals(0, countLifecycleSchemaObjects(jdbc))
        );
    }

    private void assertArmorDistribution(ItemObjectDao objectDao) {
        for (final var slot : Set.of(
            PersonageSlot.BODY,
            PersonageSlot.PANTS,
            PersonageSlot.SHOES,
            PersonageSlot.HELMET,
            PersonageSlot.GLOVES
        )) {
            final var objects = objectDao.listAvailableBySlot(slot);
            Assertions.assertEquals(8, objects.size(), slot.name());
            for (final var type : DefenseType.values()) {
                Assertions.assertEquals(
                    2,
                    objects.stream()
                        .flatMap(object -> object.object().defense().stream())
                        .filter(defense -> defense.defenseType() == type)
                        .count(),
                    slot + "/" + type
                );
            }
        }
    }

    private void inTransaction(Runnable action) {
        final var transaction = new TransactionTemplate(new DataSourceTransactionManager(schemaDataSource));
        transaction.executeWithoutResult(_ -> action.run());
    }

    private void seedPreviousCatalog(
        ItemObjectsToml itemObjects,
        ItemModifiersToml modifiers,
        JsonUtils jsonUtils
    ) {
        final var jdbc = JdbcClient.create(schemaDataSource);
        for (final var itemObject : itemObjects.itemObjects()) {
            final var legacyAttack = itemObject.attacks().stream().findFirst();
            jdbc.sql(INSERT_PREVIOUS_ITEM_OBJECT_SQL)
                .param("code", itemObject.code())
                .param("health", itemObject.health())
                .param("crit_chance", itemObject.critChance())
                .param("dodge_chance", itemObject.dodgeChance())
                .param("crit_multiplier", itemObject.critMultiplier())
                .param("speed", itemObject.speed())
                .param("base_threat", itemObject.baseThreat())
                .param("attack_type", legacyAttack.map(attack -> attack.attackType().name()).orElse(null))
                .param("attack_range", legacyAttack.map(attack -> attack.maxRange()).orElse(null))
                .param("attack", legacyAttack.map(attack -> attack.attack()).orElse(null))
                .param("defense_type", itemObject.defense().map(defense -> defense.defenseType().name()).orElse(null))
                .param("defense", itemObject.defense().map(defense -> defense.defense()).orElse(null))
                .param("locale", jsonUtils.mapToPostgresJson(itemObject.locales()))
                .param("personage_slot_ids", personageSlotIdsArray(itemObject.slots()))
                .param("attack_parts", jsonUtils.mapToPostgresJson(itemObject.attacks()))
                .param("impact", itemObject.impact())
                .param("progression_version", itemObject.progressionVersion().name())
                .update();
        }
        for (final var modifier : modifiers.modifiers()) {
            jdbc.sql(INSERT_PREVIOUS_MODIFIER_SQL)
                .param("code", modifier.code())
                .param("active_enum", modifier.activeEnum().name())
                .param("type_id", modifier.type().id)
                .param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
                .param("personage_slot_ids", personageSlotIdsArray(modifier.availableOnSlots()))
                .update();
        }
    }

    private Array personageSlotIdsArray(Set<PersonageSlot> slots) {
        final var ids = slots.stream().map(slot -> slot.id).toArray(Integer[]::new);
        try (Connection connection = schemaDataSource.getConnection()) {
            return connection.createArrayOf("integer", ids);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create personage slot array", e);
        }
    }

    private void migrateBeforeCatalogLifecycle() throws Exception {
        useLiquibase(liquibase -> liquibase.update(
            CHANGES_BEFORE_CATALOG_LIFECYCLE,
            new Contexts(),
            new LabelExpression()
        ));
    }

    private void migrate() throws Exception {
        useLiquibase(liquibase -> liquibase.update(new Contexts(), new LabelExpression()));
    }

    private void rollbackLastChangeSet() throws Exception {
        useLiquibase(liquibase -> liquibase.rollback(1, new Contexts(), new LabelExpression()));
    }

    private void useLiquibase(LiquibaseAction action) throws Exception {
        try (Connection connection = schemaDataSource.getConnection()) {
            final var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            try (final var liquibase = new Liquibase(
                "migrations/main-changelog.xml",
                new ClassLoaderResourceAccessor(),
                database
            )) {
                action.run(liquibase);
            }
        }
    }

    private void executeAdmin(String sql) throws SQLException {
        try (Connection connection = adminDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static PGSimpleDataSource dataSource(String url) {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(environmentOrDefault("SEEKER_POSTGRES_TEST_USER", "dev"));
        dataSource.setPassword(environmentOrDefault("SEEKER_POSTGRES_TEST_PASSWORD", "dev"));
        return dataSource;
    }

    private static String environmentOrDefault(String name, String fallback) {
        final var value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int idForCode(JdbcTemplate jdbc, String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?", Integer.class, code);
    }

    private static int count(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private static int countEnabled(JdbcTemplate jdbc, String table, String column) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM " + table + " WHERE " + column,
            Integer.class
        );
    }

    private int countLifecycleSchemaObjects(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
            SELECT
                (SELECT count(*)
                 FROM information_schema.columns
                 WHERE table_schema = ?
                   AND (
                       (table_name = 'item_object' AND column_name = 'acquisition_enabled')
                       OR (table_name = 'item_modifier' AND column_name = 'assignment_enabled')
                   ))
                +
                (SELECT count(*)
                 FROM information_schema.tables
                 WHERE table_schema = ?
                   AND table_name IN (
                       'item_catalog_release',
                       'item_object_catalog_lifecycle_backup',
                       'item_modifier_catalog_lifecycle_backup'
                   ))
                +
                (SELECT count(*)
                 FROM pg_indexes
                 WHERE schemaname = ?
                   AND indexname IN ('idx_item_item_object_id', 'idx_item_item_modifier_id'))
            """, Integer.class, schema, schema, schema);
    }

    private static int intValue(JdbcTemplate jdbc, String sql, Object argument) {
        return jdbc.queryForObject(sql, Integer.class, argument);
    }

    private static long longValue(JdbcTemplate jdbc, String sql, Object argument) {
        return jdbc.queryForObject(sql, Long.class, argument);
    }

    private static String stringValue(JdbcTemplate jdbc, String sql, Object argument) {
        return jdbc.queryForObject(sql, String.class, argument);
    }

    private static <T> T load(String path, Function<InputStream, T> loader) {
        try (final var stream = EquipmentCatalogPostgresIntegrationTest.class
            .getClassLoader()
            .getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return loader.apply(stream);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to close test resource: " + path, e);
        }
    }

    @FunctionalInterface
    private interface LiquibaseAction {
        void run(Liquibase liquibase) throws Exception;
    }

    private static final String INSERT_PREVIOUS_ITEM_OBJECT_SQL = """
        INSERT INTO item_object (
            code, health, crit_chance, dodge_chance, crit_multiplier, speed, base_threat,
            attack_type, attack_range, attack, defense_type, defense, locale, personage_slot_ids,
            attack_parts, impact, progression_version
        ) VALUES (
            :code, :health, :crit_chance, :dodge_chance, :crit_multiplier, :speed, :base_threat,
            :attack_type, :attack_range, :attack, :defense_type, :defense, CAST(:locale AS JSONB),
            :personage_slot_ids, CAST(:attack_parts AS JSONB), :impact, :progression_version
        )
        """;

    private static final String INSERT_PREVIOUS_MODIFIER_SQL = """
        INSERT INTO item_modifier (
            code, active_enum, type_id, locale, personage_slot_ids
        ) VALUES (
            :code, :active_enum, :type_id, CAST(:locale AS JSONB), :personage_slot_ids
        )
        """;
}
