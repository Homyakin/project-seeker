package ru.homyakin.seeker.game.event.anomaly;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.sql.DataSource;
import io.vavr.control.Either;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.framework.Advised;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.action.PersonageBadgeService;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleResult;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.BattleVisualizerConfig;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.infra.database.EventBattleLogDao;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyBattleService;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.anomaly.infra.database.AnomalyGvgPostgresDao;
import ru.homyakin.seeker.game.event.anomaly.infra.database.AnomalyPostgresDao;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventDao;
import ru.homyakin.seeker.game.event.launched.LaunchedEventGroupDao;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.ResolvedCombatGear;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.PersonageBattleResultDao;
import ru.homyakin.seeker.game.personage.PersonageConfig;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventDao;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.power.PowerDisplayService;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.anomaly.TelegramAnomalyService;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.TelegramError;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.TimeUtils;

/** Real PostgreSQL/DAO experiments; requires its own explicit disposable database URL. */
@EnabledIfEnvironmentVariable(named = "SEEKER_ANOMALY_POSTGRES_TEST_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class AnomalyCompletionPostgresIntegrationTest {
    private static final long RANDOM_SEED = 2026111700194L;
    private static final int INITIAL_MONEY = 200;
    private static final int INITIAL_SHARDS = 7;
    private final JsonUtils json = new JsonUtils();
    private final boolean baseline = "BASELINE".equals(System.getenv("SEEKER_ANOMALY_PG_STAGE"));
    private String schema;
    private PGSimpleDataSource admin;
    private JdbcTemplate jdbc;
    private ItemObject sword;
    private int objectId;

    @BeforeAll
    void setUp() throws Exception {
        schema = "anomaly_194_" + UUID.randomUUID().toString().replace("-", "");
        admin = dataSource(false);
        new JdbcTemplate(admin).execute("CREATE SCHEMA " + schema);
        jdbc = externalJdbc();
        try (Connection connection = dataSource(true).getConnection()) {
            final var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            try (final var liquibase = new Liquibase(
                "migrations/main-changelog.xml", new ClassLoaderResourceAccessor(), database
            )) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
        sword = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5).itemObjects()
            .stream().filter(object -> object.code().equals("sword")).findFirst().orElseThrow();
        jdbc.update("INSERT INTO badge (code, locale) VALUES ('standard', '{}') ON CONFLICT (code) DO NOTHING");
        new ItemObjectDao(dataSource(true), json).save(sword, false);
        objectId = jdbc.queryForObject("SELECT id FROM item_object WHERE code = 'sword'", Integer.class);
        record("database", Map.of("schema", schema, "stage", baseline ? "BASELINE" : "FINAL",
            "database", jdbc.queryForObject("SELECT current_database()", String.class),
            "version", jdbc.queryForObject("SHOW server_version", String.class),
            "controlledOutcome", "real battle init/log/stats; firstWin branch flag is test-controlled"));
    }

    @AfterAll
    void tearDown() {
        if (admin != null && schema != null) {
            new JdbcTemplate(admin).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    static Stream<Arguments> routesSizesAndOutcomes() {
        return Stream.of(Route.values()).flatMap(route -> IntStream.rangeClosed(1, 5)
            .boxed().flatMap(size -> Stream.of(true, false)
                .map(victory -> Arguments.of(route, size, victory))));
    }

    @ParameterizedTest(name = "{0}, N={1}, victory={2}")
    @MethodSource("routesSizesAndOutcomes")
    void exactPayoutAndResultsThroughRealServices(Route route, int size, boolean victory) throws Exception {
        final var fixture = fixture(route, size);
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, victory)) {
            final var result = context.getBean(AnomalyService.class).processExpired(fixture.event());
            Assertions.assertInstanceOf(EventResult.AnomalyResult.PveBattleFinished.class, result);
            Assertions.assertEquals(victory, ((EventResult.AnomalyResult.PveBattleFinished) result).victory());
        }
        assertCommitted(fixture, victory, before);
        record("exact-payout", Map.of("route", route, "size", size, "victory", victory,
            "before", before, "after", snapshot(fixture)));
    }

    static Stream<Arguments> routesAndCompletionFaults() {
        return Stream.of(Route.values()).flatMap(route -> Stream.of(Fault.values())
            .filter(Fault::completion).map(fault -> Arguments.of(route, fault)));
    }

    @ParameterizedTest(name = "{0}, fault={1}")
    @MethodSource("routesAndCompletionFaults")
    void failuresAreObservedFromAnotherConnectionAndRetriedAfterRestart(Route route, Fault fault) throws Exception {
        final var fixture = fixture(route, 3);
        final var before = snapshot(fixture);
        installInsideFault(fixture, fault);
        final RuntimeException failure;
        try (final var context = runtime(fixture, fault, true)) {
            failure = Assertions.assertThrows(RuntimeException.class,
                () -> context.getBean(AnomalyService.class).processExpired(fixture.event()));
        } finally {
            removeInsideFault();
        }
        final var afterFailure = snapshot(fixture);
        Assertions.assertTrue(rootCause(failure).getMessage().contains("anomaly-194-intentional"));
        if (!baseline || fault == Fault.LOG_INSIDE) {
            Assertions.assertEquals(before, afterFailure,
                "Every DAO write must participate in the same real Spring transaction");
        } else {
            Assertions.assertNotEquals(before, afterFailure,
                "Baseline characterization must really reproduce a committed prefix");
            Assertions.assertEquals(1, externalJdbc().queryForObject(
                "SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?", Integer.class, fixture.event().id()));
        }
        String retryResult;
        try (final var restarted = runtime(fixture, Fault.NONE, true)) {
            try {
                retryResult = restarted.getBean(AnomalyService.class).processExpired(
                    restarted.getBean(LaunchedEventService.class).getById(fixture.event().id()).orElseThrow()
                ).getClass().getSimpleName();
            } catch (RuntimeException retryFailure) {
                retryResult = "ERROR: " + rootCause(retryFailure).getClass().getSimpleName();
                if (!baseline) {
                    throw retryFailure;
                }
            }
        }
        if (!baseline || fault == Fault.LOG_INSIDE) {
            assertCommitted(fixture, true, before);
        }
        record("fault-restart", Map.of("route", route, "fault", fault, "failure", rootCause(failure).toString(),
            "before", before, "afterFailure", afterFailure, "retry", retryResult,
            "afterRetry", snapshot(fixture)));
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void repeatedStaleAndRestartedCompletionNeverPaysAgain(Route route) throws Exception {
        Assumptions.assumeFalse(baseline);
        final var fixture = fixture(route, 5);
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            context.getBean(AnomalyService.class).processExpired(fixture.event());
            final var completed = snapshot(fixture);
            Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class,
                context.getBean(AnomalyService.class).processExpired(fixture.event()));
            Assertions.assertEquals(completed, snapshot(fixture));
        }
        final var completed = snapshot(fixture);
        try (final var restarted = runtime(fixture, Fault.NONE, false)) {
            Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class,
                restarted.getBean(AnomalyService.class).processExpired(fixture.event()));
        }
        Assertions.assertEquals(completed, snapshot(fixture));
        assertCommitted(fixture, true, before);
        record("idempotence", Map.of("route", route, "after", completed));
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void concurrentContextsHaveSeparateInMemoryLocksButOnlyOneCommit(Route route) throws Exception {
        final var fixture = fixture(route, 5);
        final var before = snapshot(fixture);
        final var gate = new CompletionGate();
        try (final var first = runtime(fixture, Fault.NONE, true, gate);
             final var second = runtime(fixture, Fault.NONE, true);
             final var executor = Executors.newFixedThreadPool(2)) {
            Assertions.assertNotSame(first.getBean(DataSource.class), second.getBean(DataSource.class));
            Assertions.assertNotSame(first.getBean(LockService.class), second.getBean(LockService.class));
            final var secondSource = (PGSimpleDataSource) second.getBean(DataSource.class);
            final var secondName = "anomaly_194_second_" + UUID.randomUUID().toString().replace("-", "");
            secondSource.setApplicationName(secondName);
            final var firstTask = executor.submit(() ->
                first.getBean(AnomalyService.class).processExpired(fixture.event()).getClass().getSimpleName());
            Assertions.assertTrue(gate.afterLog.await(10, TimeUnit.SECONDS));
            final var secondTask = executor.submit(() -> {
                try {
                    return second.getBean(AnomalyService.class).processExpired(fixture.event()).getClass().getSimpleName();
                } catch (RuntimeException failure) {
                    return "ERROR: " + rootCause(failure).getClass().getSimpleName();
                }
            });
            boolean waitedOnDatabaseLock = false;
            try {
                if (!baseline) {
                    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
                    while (System.nanoTime() < deadline && !secondTask.isDone()) {
                        waitedOnDatabaseLock = externalJdbc().queryForObject("""
                            SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                            WHERE application_name = ? AND wait_event_type = 'Lock'
                                AND lower(query) LIKE '%for update%')
                            """, Boolean.class, secondName);
                        if (waitedOnDatabaseLock) {
                            break;
                        }
                        Thread.sleep(20);
                    }
                    Assertions.assertTrue(waitedOnDatabaseLock, "Second context must demonstrably wait on the event row lock");
                    Assertions.assertFalse(secondTask.isDone());
                } else {
                    Assertions.assertTrue(secondTask.get(10, TimeUnit.SECONDS).startsWith("ERROR:"));
                }
            } finally {
                gate.release.countDown();
            }
            final var outcomes = List.of(firstTask.get(25, TimeUnit.SECONDS), secondTask.get(25, TimeUnit.SECONDS));
            if (!baseline) {
                Assertions.assertEquals(1, outcomes.stream().filter(value -> value.equals("PveBattleFinished")).count());
                Assertions.assertEquals(1, outcomes.stream().filter(value -> value.equals("AlreadyFinal")).count());
            }
            assertCommitted(fixture, true, before);
            record("concurrent", Map.of("route", route, "outcomes", outcomes,
                "secondWaitedOnDatabaseRowLock", waitedOnDatabaseLock, "after", snapshot(fixture)));
        }
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void staleExpiredSnapshotMustRespectFreshPhaseAndExtendedDeadline(Route route) throws Exception {
        final var fixture = fixture(route, 5);
        final var future = TimeUtils.moscowTime().plusHours(2);
        jdbc.update("UPDATE launched_event SET end_date = ? WHERE id = ?", future, fixture.event().id());
        final var before = snapshot(fixture);
        String result;
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            result = context.getBean(AnomalyService.class).processExpired(fixture.event()).getClass().getSimpleName();
        }
        if (baseline) {
            Assertions.assertNotEquals(before, snapshot(fixture));
        } else {
            Assertions.assertEquals(before, snapshot(fixture), "Fresh future deadline must suppress the stale expired snapshot");
        }
        record("stale-deadline", Map.of("route", route, "result", result, "before", before, "after", snapshot(fixture)));
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void gatheringPhaseAndExtendedDeadlineMustCommitTogether(Route route) throws Exception {
        final var fixture = fixture(route, 5);
        setGathering(fixture);
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.END_DATE_AFTER, true)) {
            Assertions.assertThrows(RuntimeException.class,
                () -> context.getBean(AnomalyService.class).processExpired(fixture.event()));
        }
        final var afterFailure = snapshot(fixture);
        if (baseline) {
            Assertions.assertNotEquals(before, afterFailure);
        } else {
            Assertions.assertEquals(before, afterFailure);
        }
        try (final var restarted = runtime(fixture, Fault.NONE, true)) {
            restarted.getBean(AnomalyService.class).processExpired(fixture.event());
        }
        if (!baseline) {
            Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(fixture));
            Assertions.assertTrue(jdbc.queryForObject("SELECT end_date > ? FROM launched_event WHERE id = ?",
                Boolean.class, TimeUtils.moscowTime(), fixture.event().id()));
            Assertions.assertEquals(route == Route.SAFE ? "PVE_WAITING" : "SEARCHING", jdbc.queryForObject(
                "SELECT phase FROM anomaly WHERE launched_event_id = ?", String.class, fixture.event().id()));
            Assertions.assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?",
                Integer.class, fixture.event().id()));
        }
        record("phase-rollback", Map.of("route", route, "before", before,
            "afterFailure", afterFailure, "afterRetry", snapshot(fixture)));
    }

    static Stream<Arguments> phaseTransitionFaults() {
        return Stream.of(Route.values()).flatMap(route -> Stream.of(Fault.PHASE_AFTER, Fault.END_DATE_INSIDE)
            .map(fault -> Arguments.of(route, fault)));
    }

    @ParameterizedTest(name = "{0}, transition fault={1}")
    @MethodSource("phaseTransitionFaults")
    void failureBetweenPhaseAndDeadlineCannotLeaveHalfTransition(Route route, Fault fault) throws Exception {
        final var fixture = fixture(route, 5);
        setGathering(fixture);
        final var before = snapshot(fixture);
        installInsideFault(fixture, fault);
        try (final var context = runtime(fixture, fault, true)) {
            final var failure = Assertions.assertThrows(RuntimeException.class,
                () -> context.getBean(AnomalyService.class).processExpired(fixture.event()));
            Assertions.assertTrue(rootCause(failure).getMessage().contains("anomaly-194-intentional"));
        } finally {
            removeInsideFault();
        }
        final var afterFailure = snapshot(fixture);
        if (baseline) {
            Assertions.assertNotEquals(before, afterFailure);
        } else {
            Assertions.assertEquals(before, afterFailure);
        }
        try (final var restarted = runtime(fixture, Fault.NONE, true)) {
            restarted.getBean(AnomalyService.class).processExpired(fixture.event());
        }
        if (!baseline) {
            Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(fixture));
            Assertions.assertEquals(0, externalJdbc().queryForObject(
                "SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?", Integer.class, fixture.event().id()));
            Assertions.assertEquals(route == Route.SAFE ? "PVE_WAITING" : "SEARCHING", externalJdbc().queryForObject(
                "SELECT phase FROM anomaly WHERE launched_event_id = ?", String.class, fixture.event().id()));
        }
        record("mid-transition-fault", Map.of("route", route, "fault", fault,
            "before", before, "afterFailure", afterFailure, "afterRetry", snapshot(fixture)));
    }

    @ParameterizedTest(name = "manual ready {0}, transition fault={1}")
    @MethodSource("phaseTransitionFaults")
    void manualReadyPhaseAndDeadlineAreAtomicThroughSpringProxy(Route route, Fault fault) throws Exception {
        final var fixture = fixture(route, 5);
        setGathering(fixture);
        final var before = snapshot(fixture);
        installInsideFault(fixture, fault);
        try (final var context = runtime(fixture, fault, true)) {
            final var failure = Assertions.assertThrows(RuntimeException.class,
                () -> context.getBean(AnomalyService.class).ready(fixture.event().id(), fixture.members().getFirst()));
            Assertions.assertTrue(rootCause(failure).getMessage().contains("anomaly-194-intentional"));
        } finally {
            removeInsideFault();
        }
        final var afterFailure = snapshot(fixture);
        if (baseline) {
            Assertions.assertNotEquals(before, afterFailure);
        } else {
            Assertions.assertEquals(before, afterFailure);
        }
        try (final var restarted = runtime(fixture, Fault.NONE, true)) {
            final var retry = restarted.getBean(AnomalyService.class).ready(fixture.event().id(), fixture.members().getFirst());
            if (!baseline) {
                Assertions.assertTrue(retry.isRight());
                Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(fixture));
                Assertions.assertEquals(route == Route.SAFE ? "PVE_WAITING" : "SEARCHING", externalJdbc().queryForObject(
                    "SELECT phase FROM anomaly WHERE launched_event_id = ?", String.class, fixture.event().id()));
                Assertions.assertEquals(0, externalJdbc().queryForObject(
                    "SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?", Integer.class, fixture.event().id()));
            }
            record("ready-mid-transition-fault", Map.of("route", route, "fault", fault, "before", before,
                "afterFailure", afterFailure, "retryRight", retry.isRight(), "afterRetry", snapshot(fixture)));
        }
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void staleGatheringSnapshotCannotSkipNewWaitingPhase(Route route) throws Exception {
        final var fixture = fixture(route, 5);
        setGathering(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            final var service = context.getBean(AnomalyService.class);
            Assertions.assertInstanceOf(EventResult.AnomalyResult.GatheringStarted.class,
                service.processExpired(fixture.event()));
            final var started = snapshot(fixture);
            final var second = service.processExpired(fixture.event());
            if (baseline) {
                Assertions.assertInstanceOf(EventResult.AnomalyResult.PveBattleFinished.class, second);
                Assertions.assertNotEquals(started, snapshot(fixture));
            } else {
                Assertions.assertEquals(started, snapshot(fixture));
            }
            record("stale-phase", Map.of("route", route, "afterFirst", started,
                "secondResult", second.getClass().getSimpleName(), "afterSecond", snapshot(fixture)));
        }
    }

    static Stream<Arguments> routesAndFinalStatuses() {
        return Stream.of(Route.values()).flatMap(route -> Stream.of(EventStatus.values()).filter(EventStatus::isFinal)
            .map(status -> Arguments.of(route, status)));
    }

    @ParameterizedTest(name = "{0}, stale status={1}")
    @MethodSource("routesAndFinalStatuses")
    void staleLaunchedSnapshotCannotReplayAnyFinalStatus(Route route, EventStatus status) throws Exception {
        final var fixture = fixture(route, 3);
        jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?", status.id(), fixture.event().id());
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            final var result = context.getBean(AnomalyService.class).processExpired(fixture.event());
            if (baseline) {
                Assertions.assertInstanceOf(EventResult.AnomalyResult.PveBattleFinished.class, result);
                Assertions.assertNotEquals(before, snapshot(fixture));
            } else {
                Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class, result);
                Assertions.assertEquals(before, snapshot(fixture));
            }
            record("stale-final", Map.of("route", route, "status", status,
                "result", result.getClass().getSimpleName(), "before", before, "after", snapshot(fixture)));
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 3, 4})
    void fallbackFiltersDepartedAndForeignMembersAndPaysOnlyRemainingRoster(int size) throws Exception {
        final var fixture = fixture(Route.FALLBACK, 5);
        final var foreignGroup = fixture(Route.SAFE, 1).group();
        for (int index = size; index < fixture.members().size(); index++) {
            jdbc.update("UPDATE personage SET member_pgroup_id = ? WHERE id = ?",
                index == size ? foreignGroup.value() : null, fixture.members().get(index).value());
        }
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            final var result = (EventResult.AnomalyResult.PveBattleFinished)
                context.getBean(AnomalyService.class).processExpired(fixture.event());
            Assertions.assertEquals(size, result.personageResults().size());
        }
        final var read = externalJdbc();
        Assertions.assertEquals(size, read.queryForObject(
            "SELECT count(*) FROM personage_raid_result WHERE launched_event_id = ?", Integer.class, fixture.event().id()));
        for (int index = 0; index < fixture.members().size(); index++) {
            final var member = fixture.members().get(index);
            final var reward = index < size ? reward(Route.FALLBACK, true) : AnomalyReward.of(0, 0);
            Assertions.assertEquals(INITIAL_MONEY + reward.money().value(), read.queryForObject(
                "SELECT money FROM personage WHERE id = ?", Integer.class, member.value()));
            Assertions.assertEquals(INITIAL_SHARDS + reward.stormShards().value(), read.queryForObject(
                "SELECT storm_shards FROM personage WHERE id = ?", Integer.class, member.value()));
        }
        final var after = snapshot(fixture);
        Assertions.assertEquals(before.get("items"), after.get("items"));
        Assertions.assertEquals(before.get("ratings"), after.get("ratings"));
        record("reduced-fallback-roster", Map.of("remaining", size, "before", before, "after", after));
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void telegramFailureAfterCommitDoesNotRollbackOrRepayOnRestart(Route route) throws Exception {
        final var fixture = fixture(route, 3);
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            final var result = (EventResult.AnomalyResult.PveBattleFinished)
                context.getBean(AnomalyService.class).processExpired(fixture.event());
            final var committed = snapshot(fixture);
            final var sender = Mockito.mock(TelegramSender.class);
            Mockito.when(sender.send(Mockito.any(SendMessage.class)))
                .thenReturn(Either.left(new TelegramError.InternalError("anomaly-194-intentional-notification")));
            final var groupEvents = Mockito.mock(GroupEventService.class);
            final var telegramGroups = Mockito.mock(GroupTgService.class);
            final var telegramId = GroupTgId.from(-194L);
            Mockito.when(groupEvents.getByLaunchedEventId(fixture.event().id()))
                .thenReturn(List.of(new GroupLaunchedEvent(fixture.event().id(), telegramId, 194)));
            Mockito.when(telegramGroups.getOrCreate(telegramId))
                .thenReturn(new GroupTg(telegramId, Language.RU, fixture.group()));
            LocalizationInitializer.initLocale();
            final var notifications = new TelegramAnomalyService(telegramGroups, Mockito.mock(GetGroup.class), sender,
                groupEvents, context.getBean(AnomalyService.class), anomalyConfig(), new BattleVisualizerConfig("https://example.invalid/"));
            notifications.notifyExpired(fixture.event(), result);
            Mockito.verify(sender).send(Mockito.any(SendMessage.class));
            Assertions.assertEquals(committed, snapshot(fixture));
        }
        try (final var restarted = runtime(fixture, Fault.NONE, true)) {
            final var current = restarted.getBean(LaunchedEventService.class).getById(fixture.event().id()).orElseThrow();
            Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class,
                restarted.getBean(AnomalyService.class).processExpired(current));
        }
        assertCommitted(fixture, true, before);
        record("notification-failure", Map.of("route", route, "before", before, "after", snapshot(fixture)));
    }

    @Test
    void emptyFilteredFallbackDoesNotWriteAPlaceholderBattleOrRewards() throws Exception {
        final var fixture = fixture(Route.FALLBACK, 1);
        jdbc.update("UPDATE personage SET member_pgroup_id = NULL WHERE id = ?", fixture.members().getFirst().value());
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> context.getBean(AnomalyService.class).processExpired(fixture.event()));
        }
        Assertions.assertEquals(before, snapshot(fixture));
        record("empty-filtered-fallback", Map.of("before", before, "after", snapshot(fixture)));
    }

    @ParameterizedTest
    @EnumSource(Route.class)
    void finalHistoricEventIsReadableAndNotReplayed(Route route) throws Exception {
        final var fixture = fixture(route, 3);
        jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?", EventStatus.FAILED.id(), fixture.event().id());
        final var before = snapshot(fixture);
        try (final var context = runtime(fixture, Fault.NONE, true)) {
            final var current = context.getBean(LaunchedEventService.class).getById(fixture.event().id()).orElseThrow();
            Assertions.assertEquals(EventStatus.FAILED, current.status());
            Assertions.assertTrue(context.getBean(AnomalyPostgresDao.class).findByLaunchedEventId(current.id()).isPresent());
            Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class,
                context.getBean(AnomalyService.class).processExpired(current));
        }
        Assertions.assertEquals(before, snapshot(fixture));
        record("historical-final", Map.of("route", route, "snapshot", before));
    }

    static Stream<Arguments> acceptedVariants() {
        return Stream.of(true, false).flatMap(linked -> Stream.of(true, false)
            .map(victory -> Arguments.of(linked, victory)));
    }

    @ParameterizedTest(name = "accepted linked={0}, controlled flag={1}")
    @MethodSource("acceptedVariants")
    void acceptedMatcherCompletionRetainsActualPvpRewardsRatingAndHistoricalLinkVariants(
        boolean linked, boolean controlledFlag
    ) throws Exception {
        Assumptions.assumeFalse(baseline);
        final var pair = acceptedPair(linked);
        final var before = pairSnapshot(pair);
        try (final var context = runtime(pair.host(), Fault.NONE, controlledFlag)) {
            final var completed = completeAccepted(context, pair.host().event().id());
            Assertions.assertTrue(completed.isPresent());
            Assertions.assertInstanceOf(EventResult.AnomalyResult.BattleFinished.class, completed.orElseThrow());
            Assertions.assertTrue(completeAccepted(context, pair.host().event().id()).isEmpty());
        }
        assertPvpCommitted(pair, !controlledFlag, before);
        final var after = pairSnapshot(pair);
        try (final var restarted = runtime(pair.host(), Fault.NONE, !controlledFlag)) {
            Assertions.assertTrue(completeAccepted(restarted, pair.host().event().id()).isEmpty());
        }
        Assertions.assertEquals(after, pairSnapshot(pair));
        record("accepted-completion", Map.of("linked", linked, "controlledFlag", controlledFlag,
            "before", before, "after", after));
    }

    @Test
    void acceptedWinnerAndFinalGuestCannotReplayPvpCompletion() throws Exception {
        Assumptions.assumeFalse(baseline);
        for (final var variant : List.of("WINNER", "FINAL_GUEST", "FINAL_HOST", "SAFE", "SEARCHING")) {
            final var pair = acceptedPair(true);
            final var storage = new AnomalyPostgresDao(dataSource(true), json);
            if (variant.equals("WINNER")) {
                final var accepted = (Anomaly.Dangerous.Accepted) storage.findByLaunchedEventId(pair.host().event().id()).orElseThrow();
                storage.update(accepted.withWinner(pair.host().group()));
            } else if (variant.equals("FINAL_HOST")) {
                jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?",
                    EventStatus.SUCCESS.id(), pair.host().event().id());
            } else if (variant.equals("SAFE")) {
                storage.update(new Anomaly.Safe(pair.host().event().id(), pair.host().group(),
                    pair.host().members().getFirst(), AnomalyPveTemplate.ASH_WIND, Anomaly.Safe.Phase.PVE_WAITING));
            } else if (variant.equals("SEARCHING")) {
                storage.update(new Anomaly.Dangerous.Searching(pair.host().event().id(), pair.host().group(),
                    pair.host().members().getFirst(), 1234));
            }
            final var before = pairSnapshot(pair);
            try (final var context = runtime(pair.host(), Fault.NONE, true)) {
                final var eventId = variant.equals("FINAL_GUEST") ? pair.guest().event().id() : pair.host().event().id();
                Assertions.assertTrue(completeAccepted(context, eventId).isEmpty(), variant);
            }
            Assertions.assertEquals(before, pairSnapshot(pair), variant);
            record("accepted-noop", Map.of("variant", variant, "snapshot", before));
        }
    }

    @Test
    void acceptedCompletionRollbackIncludesBothRatingsWinnersAndBothTeamsRewards() throws Exception {
        Assumptions.assumeFalse(baseline);
        final var pair = acceptedPair(true);
        final var before = pairSnapshot(pair);
        try (final var context = runtime(pair.host(), Fault.RESULTS_AFTER, true)) {
            final var failure = Assertions.assertThrows(Exception.class,
                () -> completeAccepted(context, pair.host().event().id()));
            Assertions.assertTrue(rootCause(failure).getMessage().contains("anomaly-194-intentional"));
        }
        Assertions.assertEquals(before, pairSnapshot(pair), "PvP ratings, winner links and both payouts must roll back too");
        try (final var restarted = runtime(pair.host(), Fault.NONE, true)) {
            Assertions.assertTrue(completeAccepted(restarted, pair.host().event().id()).isPresent());
        }
        assertPvpCommitted(pair, false, before);
        record("accepted-rollback", Map.of("before", before, "afterRetry", pairSnapshot(pair)));
    }

    @Test
    void acceptedMatcherAndExpiredProcessorReallyOverlapButCommitOnlyOnePvpBattle() throws Exception {
        Assumptions.assumeFalse(baseline);
        final var pair = acceptedPair(true);
        final var before = pairSnapshot(pair);
        final var gate = new CompletionGate();
        try (final var first = runtime(pair.host(), Fault.NONE, true, gate);
             final var second = runtime(pair.host(), Fault.NONE, true);
             final var executor = Executors.newFixedThreadPool(2)) {
            final var secondName = "anomaly_194_accepted_" + UUID.randomUUID().toString().replace("-", "");
            ((PGSimpleDataSource) second.getBean(DataSource.class)).setApplicationName(secondName);
            final var completion = executor.submit(() -> completeAccepted(first, pair.host().event().id()).isPresent());
            Assertions.assertTrue(gate.afterLog.await(10, TimeUnit.SECONDS));
            final var expiration = executor.submit(() -> second.getBean(AnomalyService.class)
                .processExpired(pair.host().event()));
            boolean databaseBlocked = false;
            try {
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
                while (System.nanoTime() < deadline && !expiration.isDone()) {
                    databaseBlocked = externalJdbc().queryForObject("""
                        SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                        WHERE application_name = ? AND wait_event_type = 'Lock'
                            AND lower(query) LIKE '%for update%')
                        """, Boolean.class, secondName);
                    if (databaseBlocked) {
                        break;
                    }
                    Thread.sleep(20);
                }
                Assertions.assertTrue(databaseBlocked);
                Assertions.assertFalse(expiration.isDone());
            } finally {
                gate.release.countDown();
            }
            Assertions.assertTrue(completion.get(25, TimeUnit.SECONDS));
            Assertions.assertInstanceOf(EventResult.AnomalyResult.AlreadyFinal.class,
                expiration.get(25, TimeUnit.SECONDS));
            assertPvpCommitted(pair, false, before);
            record("accepted-expiry-concurrency", Map.of("secondWaitedOnDatabaseRowLock", databaseBlocked,
                "before", before, "after", pairSnapshot(pair)));
        }
    }

    private Optional<?> completeAccepted(AnnotationConfigApplicationContext context, long id) throws Exception {
        // Reflection keeps the same class compilable against the archived pre-fix baseline.
        return (Optional<?>) AnomalyBattleService.class.getMethod("fightAcceptedIfActive", long.class)
            .invoke(context.getBean(AnomalyBattleService.class), id);
    }

    private AcceptedPair acceptedPair(boolean linked) {
        final var host = fixture(Route.FALLBACK, 3);
        final var guest = fixture(Route.FALLBACK, 3);
        final var storage = new AnomalyPostgresDao(dataSource(true), json);
        storage.update(new Anomaly.Dangerous.Accepted(host.event().id(), host.group(), host.members().getFirst(),
            guest.group(), guest.members().getFirst(), linked ? Optional.of(guest.event().id()) : Optional.empty(),
            Optional.empty(), 1234));
        storage.update(new Anomaly.Dangerous.Accepted(guest.event().id(), guest.group(), guest.members().getFirst(),
            host.group(), host.members().getFirst(), linked ? Optional.of(host.event().id()) : Optional.empty(),
            Optional.empty(), 1234));
        jdbc.update("UPDATE personage_to_event SET launched_event_id = ? WHERE launched_event_id = ?",
            host.event().id(), guest.event().id());
        jdbc.update("INSERT INTO launched_event_to_pgroup (launched_event_id, pgroup_id) VALUES (?, ?)",
            host.event().id(), guest.group().value());
        jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?", EventStatus.CANCELED.id(), guest.event().id());
        return new AcceptedPair(host, guest, linked);
    }

    private Map<String, Map<String, List<String>>> pairSnapshot(AcceptedPair pair) {
        return Map.of("host", snapshot(pair.host()), "guest", snapshot(pair.guest()));
    }

    private void assertPvpCommitted(
        AcceptedPair pair, boolean hostWins, Map<String, Map<String, List<String>>> before
    ) {
        final var read = externalJdbc();
        Assertions.assertEquals(EventStatus.SUCCESS.id(), status(pair.host()));
        Assertions.assertEquals(EventStatus.CANCELED.id(), status(pair.guest()));
        Assertions.assertEquals(1, read.queryForObject("SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?",
            Integer.class, pair.host().event().id()));
        Assertions.assertEquals(6, read.queryForObject("SELECT count(*) FROM personage_raid_result WHERE launched_event_id = ?",
            Integer.class, pair.host().event().id()));
        for (final var team : List.of(pair.host(), pair.guest())) {
            final boolean won = team == pair.host() ? hostWins : !hostWins;
            Assertions.assertEquals(1234 + (won ? 16 : -16), read.queryForObject(
                "SELECT rating FROM anomaly_rating WHERE pgroup_id = ?", Integer.class, team.group().value()));
            for (final var personage : team.members()) {
                Assertions.assertEquals(INITIAL_MONEY + (won ? 30 : 25), read.queryForObject(
                    "SELECT money FROM personage WHERE id = ?", Integer.class, personage.value()));
                Assertions.assertEquals(INITIAL_SHARDS + (won ? 10 : 8), read.queryForObject(
                    "SELECT storm_shards FROM personage WHERE id = ?", Integer.class, personage.value()));
            }
        }
        final var winner = hostWins ? pair.host().group().value() : pair.guest().group().value();
        Assertions.assertEquals(winner, read.queryForObject(
            "SELECT winner_pgroup_id FROM anomaly WHERE launched_event_id = ?", Long.class, pair.host().event().id()));
        if (pair.linked()) {
            Assertions.assertEquals(winner, read.queryForObject(
                "SELECT winner_pgroup_id FROM anomaly WHERE launched_event_id = ?", Long.class, pair.guest().event().id()));
        }
        final var after = pairSnapshot(pair);
        Assertions.assertEquals(before.get("host").get("items"), after.get("host").get("items"));
        Assertions.assertEquals(before.get("guest").get("items"), after.get("guest").get("items"));
    }

    private Fixture fixture(Route route, int size) {
        final var group = GroupId.from(jdbc.queryForObject("""
            INSERT INTO pgroup (is_active, init_date, next_event_date, next_rumor_date,
                event_intervals_setting, time_zone_setting, name, is_hidden, settings, active_badge_id, raid_level)
            VALUES (true, localtimestamp, localtimestamp, localtimestamp, '[]', '+03:00',
                'Anomaly transaction test', false, '{}', (SELECT id FROM badge WHERE code = 'standard'), 1)
            RETURNING id
            """, Long.class));
        jdbc.update("INSERT INTO anomaly_rating (pgroup_id, rating) VALUES (?, 1234)", group.value());
        final var people = new PersonageDao(dataSource(true), json, personageConfig());
        final var members = IntStream.range(0, size).mapToObj(index -> {
            final var id = people.createDefault("Anomaly DB " + index);
            jdbc.update("UPDATE personage SET member_pgroup_id = ?, storm_shards = ? WHERE id = ?",
                group.value(), INITIAL_SHARDS, id.value());
            jdbc.update("""
                INSERT INTO personage_available_badge (personage_id, badge_id, is_active)
                VALUES (?, (SELECT id FROM badge WHERE code = 'standard'), true)
                """, id.value());
            jdbc.update("""
                INSERT INTO item (item_object_id, rarity, personage_id, is_equipped, enhance_level, enhance_revision)
                VALUES (?, ?, ?, true, 6, 17)
                """, objectId, ItemRarity.COMMON.ordinal(), id.value());
            return id;
        }).toList();
        final var events = new LaunchedEventDao(dataSource(true), json);
        final int eventId = jdbc.queryForObject("SELECT id FROM event WHERE code = 'anomaly-expedition'", Integer.class);
        final var now = TimeUtils.moscowTime();
        final var event = events.getById(events.save(eventId, now.minusHours(2), now.minusMinutes(1))).orElseThrow();
        // The unrelated create-time group DAO explicitly targets public; completion uses no such query.
        jdbc.update("INSERT INTO launched_event_to_pgroup (launched_event_id, pgroup_id) VALUES (?, ?)",
            event.id(), group.value());
        final var participants = new PersonageEventDao(dataSource(true), json);
        members.forEach(id -> participants.save(new AddPersonageToEventRequest(event.id(), id, Optional.empty(), 0)));
        final var storage = new AnomalyPostgresDao(dataSource(true), json);
        storage.save(route == Route.SAFE
            ? new Anomaly.Safe(event.id(), group, members.getFirst(), AnomalyPveTemplate.ASH_WIND,
                Anomaly.Safe.Phase.PVE_WAITING)
            : new Anomaly.Dangerous.Searching(event.id(), group, members.getFirst(), 1234));
        return new Fixture(route, group, members, event);
    }

    private void setGathering(Fixture fixture) {
        final var storage = new AnomalyPostgresDao(dataSource(true), json);
        storage.update(fixture.route() == Route.SAFE
            ? new Anomaly.Safe(fixture.event().id(), fixture.group(), fixture.members().getFirst(),
                AnomalyPveTemplate.ASH_WIND, Anomaly.Safe.Phase.GATHERING)
            : new Anomaly.Dangerous.Gathering(fixture.event().id(), fixture.group(), fixture.members().getFirst()));
    }

    private AnnotationConfigApplicationContext runtime(Fixture fixture, Fault fault, boolean victory) throws Exception {
        return runtime(fixture, fault, victory, null);
    }

    private AnnotationConfigApplicationContext runtime(
        Fixture fixture, Fault fault, boolean victory, CompletionGate gate
    ) throws Exception {
        final var context = new AnnotationConfigApplicationContext();
        final var source = dataSource(true);
        final var people = Mockito.spy(new PersonageDao(source, json, personageConfig()));
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.MONEY_AFTER);
            return result;
        }).when(people).addMoney(Mockito.anyMap());
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.SHARDS_AFTER);
            return result;
        }).when(people).addStormShards(Mockito.anyMap());
        final var results = Mockito.spy(new PersonageBattleResultDao(source, json));
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.RESULTS_AFTER);
            return result;
        }).when(results).saveBatch(Mockito.anyList());
        final var logs = Mockito.spy(new EventBattleLogDao(source, json));
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            if (gate != null) {
                gate.waitAfterLog();
            }
            failAfter(fault, Fault.LOG_AFTER);
            return result;
        }).when(logs).save(Mockito.anyLong(), Mockito.any(), Mockito.any());
        final var events = Mockito.spy(new LaunchedEventDao(source, json));
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.STATUS_AFTER);
            return result;
        }).when(events).updateStatus(Mockito.anyLong(), Mockito.any());
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.END_DATE_AFTER);
            return result;
        }).when(events).updateEndDate(Mockito.anyLong(), Mockito.any());
        final var anomaly = Mockito.spy(new AnomalyPostgresDao(source, json));
        Mockito.doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            failAfter(fault, Fault.PHASE_AFTER);
            return result;
        }).when(anomaly).update(Mockito.any());
        final var loadouts = Mockito.mock(EquipmentLoadoutService.class);
        Mockito.when(loadouts.resolveCombatGear(Mockito.anyList(), Mockito.eq(EventType.ANOMALY)))
            .thenAnswer(invocation -> {
                final List<Personage> participants = invocation.getArgument(0);
                final var gear = new ResolvedCombatGear(
                    List.of(new Item(sword, Optional.empty(), ItemRarity.COMMON, 6)),
                    Position.FRONT, TargetingTactic.THREAT
                );
                final var map = new LinkedHashMap<PersonageId, ResolvedCombatGear>();
                participants.forEach(person -> map.put(person.id(), gear));
                return map;
            });
        context.registerBean(DataSource.class, () -> source);
        context.registerBean(JsonUtils.class, () -> json);
        context.registerBean(PersonageConfig.class, AnomalyCompletionPostgresIntegrationTest::personageConfig);
        context.registerBean(PersonageDao.class, () -> people);
        context.registerBean(PersonageBattleResultDao.class, () -> results);
        context.registerBean(EventBattleLogDao.class, () -> logs);
        context.registerBean(LaunchedEventDao.class, () -> events);
        context.registerBean(AnomalyPostgresDao.class, () -> anomaly);
        context.registerBean(EventConfig.class, () -> new EventConfig(Duration.ofHours(1), 1));
        context.registerBean(AnomalyConfig.class, AnomalyCompletionPostgresIntegrationTest::anomalyConfig);
        context.registerBean(PlayerCombatConfig.class, AnomalyCompletionPostgresIntegrationTest::combatConfig);
        context.registerBean(LockService.class, InMemoryLockService::new);
        context.registerBean(EquipmentLoadoutService.class, () -> loadouts);
        context.registerBean(PersonageBadgeService.class, () -> Mockito.mock(PersonageBadgeService.class));
        context.registerBean(ItemService.class, () -> Mockito.mock(ItemService.class));
        context.registerBean(PowerDisplayService.class, () -> Mockito.mock(PowerDisplayService.class));
        context.registerBean(GetGroup.class, () -> Mockito.mock(GetGroup.class));
        context.registerBean(OutpostStorage.class, () -> Mockito.mock(OutpostStorage.class));
        context.registerBean(EventService.class, () -> Mockito.mock(EventService.class));
        context.register(RuntimeConfiguration.class, PersonageService.class, PersonageEventDao.class,
            PersonageEventService.class, EventBattleLogService.class, LaunchedEventGroupDao.class,
            LaunchedEventService.class, AnomalyGvgPostgresDao.class,
            AnomalyBattleService.class, AnomalyService.class);
        try {
            context.refresh();
            Object battleService = context.getBean(AnomalyBattleService.class);
            if (battleService instanceof Advised proxy) {
                battleService = proxy.getTargetSource().getTarget();
            }
            final Field field = AnomalyBattleService.class.getDeclaredField("battle");
            field.setAccessible(true);
            field.set(battleService, new ControlledBattle(victory));
            return context;
        } catch (Exception failure) {
            context.close();
            throw failure;
        }
    }

    private void installInsideFault(Fixture fixture, Fault fault) {
        if (!fault.name().endsWith("_INSIDE")) {
            return;
        }
        final var second = fixture.members().get(1).value();
        final String table;
        final String condition;
        switch (fault) {
            case LOG_INSIDE -> {
                table = "event_battle_log";
                condition = "NEW.launched_event_id = " + fixture.event().id();
            }
            case MONEY_INSIDE -> {
                table = "personage";
                condition = "NEW.id = " + second + " AND NEW.money IS DISTINCT FROM OLD.money";
            }
            case SHARDS_INSIDE -> {
                table = "personage";
                condition = "NEW.id = " + second + " AND NEW.storm_shards IS DISTINCT FROM OLD.storm_shards";
            }
            case STATUS_INSIDE -> {
                table = "launched_event";
                condition = "NEW.id = " + fixture.event().id() + " AND NEW.status_id IS DISTINCT FROM OLD.status_id";
            }
            case END_DATE_INSIDE -> {
                table = "launched_event";
                condition = "NEW.id = " + fixture.event().id() + " AND NEW.end_date IS DISTINCT FROM OLD.end_date";
            }
            case RESULTS_INSIDE -> {
                table = "personage_raid_result";
                condition = "NEW.personage_id = " + second + " AND NEW.launched_event_id = " + fixture.event().id();
            }
            default -> throw new IllegalStateException("Unexpected inside fault " + fault);
        }
        jdbc.execute("CREATE FUNCTION anomaly_194_reject() RETURNS trigger AS $$ BEGIN IF " + condition
            + " THEN RAISE EXCEPTION 'anomaly-194-intentional-inside-" + fault
            + "'; END IF; RETURN NEW; END; $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER anomaly_194_reject BEFORE INSERT OR UPDATE ON " + table
            + " FOR EACH ROW EXECUTE FUNCTION anomaly_194_reject()");
    }

    private void removeInsideFault() {
        jdbc.execute("DROP FUNCTION IF EXISTS anomaly_194_reject() CASCADE");
    }

    private Map<String, List<String>> snapshot(Fixture fixture) {
        final var read = externalJdbc();
        final var result = new LinkedHashMap<String, List<String>>();
        result.put("event", rows(read, "SELECT to_jsonb(row)::text FROM launched_event row WHERE id = ?", fixture.event().id()));
        result.put("anomaly", rows(read, "SELECT to_jsonb(row)::text FROM anomaly row WHERE launched_event_id = ?", fixture.event().id()));
        result.put("log", rows(read, """
            SELECT json_build_object('id', launched_event_id, 'init', md5(init::text), 'log', md5(log::text))::text
            FROM event_battle_log WHERE launched_event_id = ?
            """, fixture.event().id()));
        result.put("personages", rows(read, """
            SELECT to_jsonb(row)::text FROM personage row WHERE id IN
                (SELECT personage_id FROM personage_to_event WHERE launched_event_id = ?) ORDER BY id
            """, fixture.event().id()));
        result.put("personageNonWalletFields", rows(read, """
            SELECT (to_jsonb(row) - 'money' - 'storm_shards')::text FROM personage row WHERE id IN
                (SELECT personage_id FROM personage_to_event WHERE launched_event_id = ?) ORDER BY id
            """, fixture.event().id()));
        result.put("items", rows(read, """
            SELECT to_jsonb(row)::text FROM item row WHERE personage_id IN
                (SELECT personage_id FROM personage_to_event WHERE launched_event_id = ?) ORDER BY id
            """, fixture.event().id()));
        result.put("results", rows(read, "SELECT to_jsonb(row)::text FROM personage_raid_result row WHERE launched_event_id = ? ORDER BY personage_id",
            fixture.event().id()));
        result.put("ratings", rows(read, "SELECT to_jsonb(row)::text FROM anomaly_rating row WHERE pgroup_id = ?", fixture.group().value()));
        result.put("groupStats", rows(read, "SELECT to_jsonb(row)::text FROM pgroup_battle_result row WHERE launched_event_id = ?", fixture.event().id()));
        return result;
    }

    private static List<String> rows(JdbcTemplate read, String sql, long id) {
        return read.queryForList(sql, String.class, id);
    }

    private void assertCommitted(Fixture fixture, boolean victory, Map<String, List<String>> before) {
        final var reward = reward(fixture.route(), victory);
        Assertions.assertEquals(victory ? EventStatus.SUCCESS.id() : EventStatus.FAILED.id(), status(fixture));
        final var read = externalJdbc();
        Assertions.assertEquals(1, read.queryForObject("SELECT count(*) FROM event_battle_log WHERE launched_event_id = ?",
            Integer.class, fixture.event().id()));
        Assertions.assertEquals(fixture.members().size(), read.queryForObject(
            "SELECT count(*) FROM personage_raid_result WHERE launched_event_id = ?", Integer.class, fixture.event().id()));
        for (final var member : fixture.members()) {
            Assertions.assertEquals(INITIAL_MONEY + reward.money().value(), read.queryForObject(
                "SELECT money FROM personage WHERE id = ?", Integer.class, member.value()));
            Assertions.assertEquals(INITIAL_SHARDS + reward.stormShards().value(), read.queryForObject(
                "SELECT storm_shards FROM personage WHERE id = ?", Integer.class, member.value()));
            Assertions.assertEquals(reward.money().value(), read.queryForObject(
                "SELECT reward FROM personage_raid_result WHERE personage_id = ? AND launched_event_id = ?",
                Integer.class, member.value(), fixture.event().id()));
            Assertions.assertEquals(reward.stormShards().value(), read.queryForObject(
                "SELECT storm_shards FROM personage_raid_result WHERE personage_id = ? AND launched_event_id = ?",
                Integer.class, member.value(), fixture.event().id()));
            Assertions.assertTrue(read.queryForObject("""
                SELECT stats IS NOT NULL AND generated_item_id IS NULL AND generated_contraband_id IS NULL
                FROM personage_raid_result WHERE personage_id = ? AND launched_event_id = ?
                """, Boolean.class, member.value(), fixture.event().id()));
        }
        final var after = snapshot(fixture);
        Assertions.assertEquals(before.get("items"), after.get("items"));
        Assertions.assertEquals(before.get("personageNonWalletFields"), after.get("personageNonWalletFields"));
        Assertions.assertEquals(before.get("ratings"), after.get("ratings"));
        Assertions.assertEquals(before.get("groupStats"), after.get("groupStats"));
    }

    private int status(Fixture fixture) {
        return externalJdbc().queryForObject("SELECT status_id FROM launched_event WHERE id = ?",
            Integer.class, fixture.event().id());
    }

    private JdbcTemplate externalJdbc() {
        final var result = new JdbcTemplate(dataSource(true));
        result.setQueryTimeout(10);
        return result;
    }

    private PGSimpleDataSource dataSource(boolean scoped) {
        final var source = new PGSimpleDataSource();
        final var url = System.getenv("SEEKER_ANOMALY_POSTGRES_TEST_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Explicit disposable anomaly test database URL required");
        }
        source.setUrl(url);
        source.setUser(requiredEnvironment("SEEKER_ANOMALY_POSTGRES_TEST_USER"));
        source.setPassword(requiredEnvironment("SEEKER_ANOMALY_POSTGRES_TEST_PASSWORD"));
        source.setConnectTimeout(5);
        source.setSocketTimeout(20);
        source.setOptions("-c statement_timeout=10000 -c lock_timeout=5000");
        if (scoped) {
            source.setCurrentSchema(schema);
        }
        return source;
    }

    private static String requiredEnvironment(String name) {
        final var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Explicit isolated database setting missing: " + name);
        }
        return value;
    }

    private void record(String scenario, Map<String, ?> details) throws IOException {
        final var output = System.getenv("SEEKER_ANOMALY_PG_EVIDENCE");
        if (output == null || output.isBlank()) {
            return;
        }
        final var row = new LinkedHashMap<String, Object>();
        row.put("scenario", scenario);
        row.put("stage", baseline ? "BASELINE" : "FINAL");
        row.put("details", details);
        Files.writeString(Path.of(output), json.mapToPostgresJson(row).getValue() + "\n",
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void failAfter(Fault actual, Fault selected) {
        if (actual == selected) {
            throw new IllegalStateException("anomaly-194-intentional-after-" + actual);
        }
    }

    private static Throwable rootCause(Throwable failure) {
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        return failure;
    }

    private static PersonageConfig personageConfig() {
        return new PersonageConfig(Duration.ofHours(8), INITIAL_MONEY, 100);
    }

    private static PlayerCombatConfig combatConfig() {
        final var result = new PlayerCombatConfig();
        result.setVersion(SkillFormulaVersion.SCALING_SKILLS_V3);
        result.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);
        return result;
    }

    private static AnomalyConfig anomalyConfig() {
        final var config = Mockito.mock(AnomalyConfig.class);
        Mockito.when(config.partySize()).thenReturn(5);
        Mockito.when(config.gatheringDuration()).thenReturn(Duration.ofMinutes(10));
        Mockito.when(config.safePveDuration()).thenReturn(Duration.ofMinutes(5));
        Mockito.when(config.dangerousSearchDuration()).thenReturn(Duration.ofMinutes(30));
        Mockito.when(config.initialRating()).thenReturn(1000);
        Mockito.when(config.eloK()).thenReturn(32);
        Mockito.when(config.pveWinReward()).thenReturn(reward(Route.SAFE, true));
        Mockito.when(config.pveLoseReward()).thenReturn(reward(Route.SAFE, false));
        Mockito.when(config.gvgFallbackWinReward()).thenReturn(reward(Route.FALLBACK, true));
        Mockito.when(config.gvgFallbackLoseReward()).thenReturn(reward(Route.FALLBACK, false));
        Mockito.when(config.gvgWinReward()).thenReturn(AnomalyReward.of(30, 10));
        Mockito.when(config.gvgLoseReward()).thenReturn(AnomalyReward.of(25, 8));
        return config;
    }

    private static AnomalyReward reward(Route route, boolean victory) {
        return route == Route.SAFE ? AnomalyReward.of(victory ? 10 : 5, victory ? 2 : 1)
            : AnomalyReward.of(victory ? 20 : 15, victory ? 5 : 3);
    }

    enum Route { SAFE, FALLBACK }

    enum Fault {
        NONE, LOG_AFTER, LOG_INSIDE, MONEY_AFTER, MONEY_INSIDE, SHARDS_AFTER, SHARDS_INSIDE,
        STATUS_AFTER, STATUS_INSIDE, RESULTS_AFTER, RESULTS_INSIDE, END_DATE_AFTER, PHASE_AFTER, END_DATE_INSIDE;

        boolean completion() {
            return this != NONE && this != END_DATE_AFTER && this != PHASE_AFTER && this != END_DATE_INSIDE;
        }
    }

    private record Fixture(Route route, GroupId group, List<PersonageId> members, LaunchedEvent event) { }

    private record AcceptedPair(Fixture host, Fixture guest, boolean linked) { }

    private static final class CompletionGate {
        private final CountDownLatch afterLog = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean used = new AtomicBoolean();

        private void waitAfterLog() throws InterruptedException {
            if (used.compareAndSet(false, true)) {
                afterLog.countDown();
                Assertions.assertTrue(release.await(12, TimeUnit.SECONDS), "Test must release its finite completion barrier");
            }
        }
    }

    /** Branch control is deliberately not evidence about anomaly difficulty or battle correctness. */
    private static final class ControlledBattle extends Battle {
        private final boolean victory;

        private ControlledBattle(boolean victory) {
            super(RANDOM_SEED);
            this.victory = victory;
        }

        @Override
        public BattleResult process(List<BattlePersonage> first, List<BattlePersonage> second, BattleTempoVersion tempo) {
            final var actual = super.process(first, second, 200, tempo);
            return new BattleResult(actual.initState(), actual.actionLog(), actual.rounds(), !victory, actual.personageStats());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class RuntimeConfiguration {
        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            final var result = new DataSourceTransactionManager(source);
            result.setDefaultTimeout(15);
            return result;
        }
    }
}
