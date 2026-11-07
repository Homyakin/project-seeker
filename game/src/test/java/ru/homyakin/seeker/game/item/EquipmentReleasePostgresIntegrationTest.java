package ru.homyakin.seeker.game.item;

import java.io.InputStream;
import java.sql.Connection;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.sql.DataSource;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseCoordinator;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.PersonageConfig;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.power.PowerDisplayService;
import ru.homyakin.seeker.game.personage.power.PowerDisplayVersion;
import ru.homyakin.seeker.utils.JsonUtils;

/** Only creates disposable schemas in an explicitly supplied test database; never starts the bot. */
@EnabledIfEnvironmentVariable(named = "SEEKER_POSTGRES_TEST_URL", matches = ".+")
class EquipmentReleasePostgresIntegrationTest {
    private static final String RELEASE = "homyakin.seeker.equipment-release.";
    private static final String COMBAT = "homyakin.seeker.player-combat.";
    private static final String DISPLAY = "homyakin.seeker.power-display.version";
    private static final List<String> SNAPSHOT_TABLES = List.of(
        "item", "item_object", "item_modifier", "item_catalog_release",
        "item_object_catalog_lifecycle_backup", "item_modifier_catalog_lifecycle_backup", "personage"
    );

    private final JsonUtils jsonUtils = new JsonUtils();
    private final ItemObjectsToml previousObjects = load("game-data/item_objects_catalog.toml", ItemObjectsToml::load);
    private final ItemModifiersToml previousModifiers = load("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load);
    private String schema;
    private PGSimpleDataSource adminDataSource;
    private PGSimpleDataSource schemaDataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() throws Exception {
        schema = "release_step123_" + UUID.randomUUID().toString().replace("-", "");
        adminDataSource = dataSource();
        new JdbcTemplate(adminDataSource).execute("CREATE SCHEMA " + schema);
        schemaDataSource = schemaDataSource();
        jdbc = new JdbcTemplate(schemaDataSource);
        migrate(true);
    }

    @AfterEach
    void tearDown() {
        if (adminDataSource != null && schema != null) {
            new JdbcTemplate(adminDataSource).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void stagesActivatesAndRestartsWithBoundCoordinatedConfiguration() throws Exception {
        final var itemIds = seedLegacyAndMigrate();
        final var identities = itemIdentities();
        final var revisions = jdbc.queryForList("SELECT enhance_revision FROM item ORDER BY id", Long.class);
        final var backups = backupSnapshot();

        prepare(settings(false));

        Assertions.assertEquals("STAGED", releaseStatus());
        Assertions.assertEquals(identities, itemIdentities());
        Assertions.assertEquals(revisions.stream().map(value -> value + 1).toList(),
            jdbc.queryForList("SELECT enhance_revision FROM item ORDER BY id", Long.class));
        Assertions.assertEquals(backups, backupSnapshot());
        assertAvailability(40, 11);
        final var staged = snapshot();
        prepare(settings(false));
        Assertions.assertEquals(staged, snapshot(), "Repeated staging must not revise equipment again");

        prepare(settings(true));

        assertActiveCatalog();
        Assertions.assertEquals(identities, itemIdentities());
        assertDatabaseEquipmentUsesBoundRuntime(itemIds);
        final var active = snapshot();
        prepare(settings(false));
        Assertions.assertEquals(active, snapshot(), "A fresh runtime must retain the active release");
        prepare(settings(true));
        Assertions.assertEquals(active, snapshot(), "Repeated activation must be idempotent");

        jdbc.update("UPDATE item_object SET acquisition_enabled = false WHERE code = 'ceremonial_tunic'");
        jdbc.update("UPDATE item_modifier SET assignment_enabled = false WHERE code = 'guarding'");
        final var retired = snapshot();
        prepare(settings(false));
        prepare(settings(true));
        Assertions.assertEquals(retired, snapshot(), "Restart must not reopen deliberately retired issuance");
    }

    @Test
    void activatesDirectlyFromLegacyWithoutLosingEquipment() throws Exception {
        final var itemIds = seedLegacyAndMigrate();
        final var identities = itemIdentities();
        final var backups = backupSnapshot();

        prepare(settings(true));

        assertActiveCatalog();
        Assertions.assertEquals(identities, itemIdentities());
        Assertions.assertEquals(backups, backupSnapshot());
        assertDatabaseEquipmentUsesBoundRuntime(itemIds);
    }

    @Test
    void freshDatabaseGetsLegacyBackupAndAnIdempotentActiveRelease() throws Exception {
        migrate(false);

        prepare(settings(true));

        assertActiveCatalog();
        Assertions.assertEquals(40, count("item_object_catalog_lifecycle_backup"));
        Assertions.assertEquals(11, count("item_modifier_catalog_lifecycle_backup"));
        Assertions.assertEquals(0, count("item"));
        final var active = snapshot();
        prepare(settings(false));
        Assertions.assertEquals(active, snapshot());
    }

    @ParameterizedTest(name = "active={0}, incompatible {1}={2}")
    @MethodSource("incompatibleRuntimes")
    void incompatibleRuntimeDoesNotMutateAnyCatalogOrOwnedItem(
        boolean alreadyActive,
        String property,
        String value
    ) throws Exception {
        seedLegacyAndMigrate();
        if (alreadyActive) {
            prepare(settings(true));
        }
        final var before = snapshot();
        final var incompatible = settings(!alreadyActive);
        incompatible.put(property, value);

        Assertions.assertThrows(IllegalStateException.class, () -> prepare(incompatible));

        Assertions.assertEquals(before, snapshot());
    }

    static Stream<Arguments> incompatibleRuntimes() {
        return Stream.of(false, true).flatMap(active -> Stream.of(
            Arguments.of(active, COMBAT + "version", "LEGACY_SKILLS_V1"),
            Arguments.of(active, COMBAT + "version", "SCALING_SKILLS_V2"),
            Arguments.of(active, COMBAT + "tempo-version", "BASELINE_V1"),
            Arguments.of(active, DISPLAY, "LEGACY_V1")
        ));
    }

    @Test
    void staleDefaultsCannotOverwriteAnActiveFifthRelease() throws Exception {
        seedLegacyAndMigrate();
        prepare(settings(true));
        final var before = snapshot();

        Assertions.assertThrows(IllegalStateException.class, () -> prepare(Map.of()));

        Assertions.assertEquals(before, snapshot());
    }

    @Test
    void anotherActiveCatalogRequiresAnExplicitUpgradeProcedure() throws Exception {
        seedLegacyAndMigrate();
        final var historical = settings(false);
        historical.put(RELEASE + "catalog-version", "SCALING_V4");
        historical.put(COMBAT + "version", "SCALING_SKILLS_V2");
        historical.put(COMBAT + "tempo-version", "BASELINE_V1");
        try (final var context = runtime(historical)) {
            context.getBean(EquipmentReleaseCoordinator.class).prepare(
                previousObjects.itemObjects(), previousModifiers.modifiers()
            );
            // Model an already active historical V4, not permission for startup to activate it now.
            context.getBean(ItemCatalogService.class).activateRelease(EquipmentCatalogVersion.SCALING_V4);
        }
        final var activeFourth = snapshot();
        prepare(historical);
        Assertions.assertEquals(activeFourth, snapshot());

        Assertions.assertThrows(IllegalStateException.class, () -> prepare(settings(true)));

        Assertions.assertEquals(activeFourth, snapshot());
    }

    @ParameterizedTest(name = "fresh={0}: activation failure rolls back the whole coordinator transaction")
    @ValueSource(booleans = {false, true})
    void activationFailureRollsBackPreparationAndRevisions(boolean fresh) throws Exception {
        if (fresh) {
            migrate(false);
        } else {
            seedLegacyAndMigrate();
        }
        jdbc.execute("""
            CREATE FUNCTION reject_test_activation() RETURNS trigger AS $$
            BEGIN
                IF NEW.status = 'ACTIVE' THEN
                    RAISE EXCEPTION 'Intentional integration-test activation failure';
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
            """);
        jdbc.execute("""
            CREATE TRIGGER reject_test_activation BEFORE UPDATE ON item_catalog_release
            FOR EACH ROW EXECUTE FUNCTION reject_test_activation()
            """);
        final var before = snapshot();

        final var failure = Assertions.assertThrows(RuntimeException.class, () -> prepare(settings(true)));

        Assertions.assertTrue(rootCause(failure).getMessage().contains("Intentional integration-test activation failure"));
        Assertions.assertEquals(before, snapshot(), "Stage and activation must share one real Spring transaction");
        jdbc.execute("DROP TRIGGER reject_test_activation ON item_catalog_release");
        jdbc.execute("DROP FUNCTION reject_test_activation()");
        prepare(settings(true));
        assertActiveCatalog();
    }

    private void assertDatabaseEquipmentUsesBoundRuntime(List<Long> itemIds) {
        try (final var context = runtime(settings(false))) {
            final var itemDao = context.getBean(ItemDao.class);
            final var items = itemIds.stream().map(id -> itemDao.getById(id).orElseThrow())
                .filter(item -> item.isEquipped()).map(item -> item.toItem()).toList();
            final var combat = context.getBean(PlayerCombatConfig.class);
            final var display = context.getBean(PowerDisplayService.class);
            Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V3, combat.version());
            Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, combat.tempoVersion());
            Assertions.assertEquals(PowerDisplayVersion.REFERENCE_V1, display.version());
            final var player = BattlePersonage.forCombat(
                items, Position.FRONT, PersonageEffects.EMPTY, Optional.of("DB player"), combat.version()
            );
            final var opponent = BattlePersonage.forCombat(
                items, Position.FRONT, PersonageEffects.EMPTY, Optional.of("DB opponent"), combat.version()
            );
            Assertions.assertTrue(player.skillSnapshots().stream().anyMatch(skill -> skill.code() == ActiveEnum.HIT_AND_RUN));
            Assertions.assertTrue(player.skillSnapshots().stream().allMatch(skill -> skill.formulaVersion() == combat.version()));
            Assertions.assertEquals(display.value(items), player.battlePersonageStats().displayedPower().orElseThrow().value());

            final var battle = new Battle(2_026_110_711L).process(
                List.of(player), List.of(opponent), 200, combat.tempoVersion()
            );

            Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, battle.initState().tempoVersion());
            Assertions.assertEquals(combat.tempoVersion(), player.battleTempoVersion());
            Assertions.assertEquals(combat.tempoVersion(), opponent.battleTempoVersion());
            Assertions.assertTrue(battle.rounds() < 200);
            Assertions.assertNotEquals(player.isAlive(), opponent.isAlive());
            Assertions.assertFalse(battle.actionLog().events().isEmpty());
            Assertions.assertTrue(battle.initState().personagesById().values().stream()
                .flatMap(personage -> personage.skills().stream())
                .allMatch(skill -> skill.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V3));
        }
    }

    private void assertActiveCatalog() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5);
        Assertions.assertEquals("ACTIVE", releaseStatus());
        assertAvailability(catalog.itemObjects().size(), catalog.modifiers().size());
        final var objects = new ItemObjectDao(schemaDataSource, jsonUtils);
        final var modifiers = new ItemModifierDao(schemaDataSource, jsonUtils);
        for (final var object : catalog.itemObjects()) {
            Assertions.assertEquals(object, objects.getAvailableById(id("item_object", object.code())).orElseThrow().object());
        }
        for (final var modifier : catalog.modifiers()) {
            Assertions.assertEquals(modifier, modifiers.getAvailableById(id("item_modifier", modifier.code())).orElseThrow().modifier());
        }
        Assertions.assertEquals(420, objects.getById(id("item_object", "halberd")).orElseThrow()
            .object().attacks().getFirst().attack());
    }

    private void assertAvailability(int items, int modifiers) {
        Assertions.assertEquals(items, jdbc.queryForObject(
            "SELECT count(*) FROM item_object WHERE acquisition_enabled", Integer.class
        ));
        Assertions.assertEquals(modifiers, jdbc.queryForObject(
            "SELECT count(*) FROM item_modifier WHERE assignment_enabled", Integer.class
        ));
    }

    private Map<String, List<String>> snapshot() {
        final var snapshot = new LinkedHashMap<String, List<String>>();
        for (final var table : SNAPSHOT_TABLES) {
            snapshot.put(table, jdbc.queryForList(
                "SELECT to_jsonb(row)::text FROM " + table + " row ORDER BY to_jsonb(row)::text", String.class
            ));
        }
        return snapshot;
    }

    private Map<String, List<String>> backupSnapshot() {
        final var snapshot = snapshot();
        return Map.of(
            "objects", snapshot.get("item_object_catalog_lifecycle_backup"),
            "modifiers", snapshot.get("item_modifier_catalog_lifecycle_backup")
        );
    }

    private List<Map<String, Object>> itemIdentities() {
        return jdbc.queryForList("""
            SELECT id, item_object_id, item_modifier_id, rarity, personage_id, is_equipped, enhance_level
            FROM item ORDER BY id
            """);
    }

    private String releaseStatus() {
        return jdbc.queryForObject("SELECT status FROM item_catalog_release WHERE code = 'SCALING_V5'", String.class);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int id(String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?", Integer.class, code);
    }

    private void prepare(Map<String, Object> settings) {
        try (final var context = runtime(settings)) {
            final var coordinator = context.getBean(EquipmentReleaseCoordinator.class);
            Assertions.assertTrue(AopUtils.isAopProxy(coordinator), "Must exercise @Transactional, not a direct constructor");
            coordinator.prepare(previousObjects.itemObjects(), previousModifiers.modifiers());
        }
    }

    private AnnotationConfigApplicationContext runtime(Map<String, Object> settings) {
        final var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-release-test", settings));
        context.registerBean(DataSource.class, this::schemaDataSource);
        context.register(RuntimeConfiguration.class);
        try {
            context.refresh();
            return context;
        } catch (RuntimeException failure) {
            context.close();
            throw failure;
        }
    }

    private static Map<String, Object> settings(boolean activate) {
        final var settings = new LinkedHashMap<String, Object>();
        settings.put(RELEASE + "catalog-version", "SCALING_V5");
        settings.put(RELEASE + "activate-on-startup", activate);
        settings.put(COMBAT + "version", "SCALING_SKILLS_V3");
        settings.put(COMBAT + "tempo-version", "ACCELERATED_V1");
        settings.put(DISPLAY, "REFERENCE_V1");
        return settings;
    }

    private List<Long> seedLegacyAndMigrate() throws Exception {
        final var client = JdbcClient.create(schemaDataSource);
        for (final var object : previousObjects.itemObjects()) {
            final var attack = object.attacks().stream().findFirst();
            try (Connection connection = schemaDataSource.getConnection()) {
                client.sql("""
                    INSERT INTO item_object (
                        code, health, crit_chance, dodge_chance, crit_multiplier, speed, base_threat,
                        attack_type, attack_range, attack, defense_type, defense, locale, personage_slot_ids,
                        attack_parts, impact, progression_version
                    ) VALUES (
                        :code, :health, :crit, :dodge, :multiplier, :speed, :threat,
                        :attack_type, :range, :attack, :defense_type, :defense, CAST(:locale AS JSONB), :slots,
                        CAST(:parts AS JSONB), :impact, :progression
                    )
                    """)
                    .param("code", object.code()).param("health", object.health())
                    .param("crit", object.critChance()).param("dodge", object.dodgeChance())
                    .param("multiplier", object.critMultiplier()).param("speed", object.speed())
                    .param("threat", object.baseThreat()).param("impact", object.impact())
                    .param("progression", object.progressionVersion().name())
                    .param("attack_type", attack.map(value -> value.attackType().name()).orElse(null))
                    .param("range", attack.map(value -> value.maxRange()).orElse(null))
                    .param("attack", attack.map(value -> value.attack()).orElse(null))
                    .param("defense_type", object.defense().map(value -> value.defenseType().name()).orElse(null))
                    .param("defense", object.defense().map(value -> value.defense()).orElse(null))
                    .param("locale", jsonUtils.mapToPostgresJson(object.locales()))
                    .param("parts", jsonUtils.mapToPostgresJson(object.attacks()))
                    .param("slots", connection.createArrayOf("integer", object.slots().stream().map(value -> value.id).toArray()))
                    .update();
            }
        }
        for (final var modifier : previousModifiers.modifiers()) {
            try (Connection connection = schemaDataSource.getConnection()) {
                client.sql("""
                    INSERT INTO item_modifier (code, active_enum, type_id, locale, personage_slot_ids)
                    VALUES (:code, :active, :type, CAST(:locale AS JSONB), :slots)
                    """)
                    .param("code", modifier.code()).param("active", modifier.activeEnum().name())
                    .param("type", modifier.type().id).param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
                    .param("slots", connection.createArrayOf("integer", modifier.availableOnSlots().stream()
                        .map(value -> value.id).toArray()))
                    .update();
            }
        }
        final long owner = new PersonageDao(
            schemaDataSource, jsonUtils, new PersonageConfig(Duration.ofHours(8), 200, 5)
        ).createDefault("Release Audit").value();
        final var items = List.of(
            insertItem(owner, "halberd", "sharp", true, 6, 7),
            insertItem(owner, "boots", "swift", true, 3, 11),
            insertItem(owner, "cuirass", "healing", true, 0, 3),
            insertItem(owner, "sword", "cunning", false, 6, 13)
        );
        migrate(false);
        return items;
    }

    private long insertItem(long owner, String object, String modifier, boolean equipped, int level, long revision) {
        return jdbc.queryForObject("""
            INSERT INTO item (
                item_object_id, item_modifier_id, rarity, personage_id, is_equipped, enhance_level, enhance_revision
            ) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
            """, Long.class, id("item_object", object), id("item_modifier", modifier),
            ItemRarity.RARE.ordinal(), owner, equipped, level, revision);
    }

    private void migrate(boolean beforeLifecycle) throws Exception {
        try (Connection connection = schemaDataSource.getConnection()) {
            final var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            try (final var liquibase = new Liquibase(
                "migrations/main-changelog.xml", new ClassLoaderResourceAccessor(), database
            )) {
                if (beforeLifecycle) {
                    liquibase.update(123, new Contexts(), new LabelExpression());
                } else {
                    liquibase.update(new Contexts(), new LabelExpression());
                }
            }
        }
    }

    private PGSimpleDataSource schemaDataSource() {
        final var source = dataSource();
        source.setCurrentSchema(schema);
        return source;
    }

    private static PGSimpleDataSource dataSource() {
        final var source = new PGSimpleDataSource();
        source.setUrl(System.getenv("SEEKER_POSTGRES_TEST_URL"));
        source.setUser(environmentOrDefault("SEEKER_POSTGRES_TEST_USER", "dev"));
        source.setPassword(environmentOrDefault("SEEKER_POSTGRES_TEST_PASSWORD", "dev"));
        return source;
    }

    private static String environmentOrDefault(String name, String fallback) {
        final var value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Throwable rootCause(Throwable failure) {
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        return failure;
    }

    private static <T> T load(String path, Function<InputStream, T> loader) {
        try (final var input = EquipmentReleasePostgresIntegrationTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return loader.apply(input);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableConfigurationProperties({EquipmentReleaseConfig.class, PlayerCombatConfig.class})
    @Import({
        ItemObjectDao.class, ItemModifierDao.class, ItemCatalogLifecycleDao.class,
        ItemCatalogService.class, ItemDao.class, EquipmentReleaseCoordinator.class, PowerDisplayService.class
    })
    static class RuntimeConfiguration {
        @Bean
        JsonUtils jsonUtils() {
            return new JsonUtils();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
    }
}
