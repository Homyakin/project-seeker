package ru.homyakin.seeker.game.event.anomaly;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.infra.database.AnomalyPostgresDao;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.PersonageConfig;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.utils.JsonUtils;

/** Real migrations and separate physical connections; the database must be explicitly supplied. */
@EnabledIfEnvironmentVariable(named = "SEEKER_ANOMALY_POSTGRES_TEST_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(30)
class AnomalyMergeCompletionPostgresIntegrationTest {
    private String schema;
    private PGSimpleDataSource adminSource;
    private PGSimpleDataSource source;
    private JdbcTemplate jdbc;

    @BeforeAll
    void setUp() throws Exception {
        schema = "anomaly_merge_194_" + UUID.randomUUID().toString().replace("-", "");
        adminSource = source(false);
        new JdbcTemplate(adminSource).execute("CREATE SCHEMA " + schema);
        source = source(true);
        jdbc = new JdbcTemplate(source);
        try (final var connection = source.getConnection()) {
            final var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            try (final var migration = new Liquibase(
                "migrations/main-changelog.xml", new ClassLoaderResourceAccessor(), database
            )) {
                migration.update(new Contexts(), new LabelExpression());
            }
        }
        jdbc.update("INSERT INTO badge (code, locale) VALUES ('standard', '{}'::jsonb) ON CONFLICT DO NOTHING");
    }

    @AfterAll
    void tearDown() {
        if (adminSource != null && schema != null) {
            new JdbcTemplate(adminSource).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = "LAUNCHED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAHostWhoseCompletionWasCommittedAfterCandidateSelection(EventStatus finalStatus) {
        final var pair = pair();
        jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?", finalStatus.id(), pair.host().launchedEventId());
        final var before = snapshot(pair);

        try (final var context = runtime()) {
            Assertions.assertFalse(dao(context).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId()));
        }

        Assertions.assertEquals(before, snapshot(pair), "A stale candidate must not mutate either expedition");
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = "LAUNCHED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsACompletedGuestWithoutChangingTheHost(EventStatus finalStatus) {
        final var pair = pair();
        jdbc.update("UPDATE launched_event SET status_id = ? WHERE id = ?", finalStatus.id(), pair.guest().launchedEventId());
        final var before = snapshot(pair);

        try (final var context = runtime()) {
            Assertions.assertFalse(dao(context).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId()));
        }

        Assertions.assertEquals(before, snapshot(pair));
    }

    @Test
    void waitsForHostCompletionBeforeTouchingEitherAnomaly() throws Exception {
        final var pair = pair();
        final var started = new CountDownLatch(1);
        try (final var completion = source.getConnection();
             final var context = runtime();
             final var executor = Executors.newSingleThreadExecutor()) {
            completion.setAutoCommit(false);
            lock(completion, pair.host().launchedEventId());
            final var match = executor.submit(() -> {
                started.countDown();
                return dao(context).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId());
            });
            try {
                Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
                Assertions.assertThrows(TimeoutException.class, () -> match.get(300, TimeUnit.MILLISECONDS),
                    "The matcher must acquire the host parent lock before changing its anomaly");
                assertAnomalyIsNotLockedByMatcher(completion, pair.host().launchedEventId());
            } finally {
                setStatus(completion, pair.host().launchedEventId(), EventStatus.SUCCESS);
                completion.commit();
            }
            Assertions.assertFalse(match.get(5, TimeUnit.SECONDS));
        }
        assertSearching(pair.host());
        assertSearching(pair.guest());
        Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(pair.guest()));
        assertSeparateParticipants(pair);
    }

    @Test
    void waitsForGuestCompletionAndRechecksItsCommittedStatus() throws Exception {
        final var pair = pair();
        final var started = new CountDownLatch(1);
        try (final var completion = source.getConnection();
             final var context = runtime();
             final var executor = Executors.newSingleThreadExecutor()) {
            completion.setAutoCommit(false);
            lock(completion, pair.guest().launchedEventId());
            final var match = executor.submit(() -> {
                started.countDown();
                return dao(context).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId());
            });
            try {
                Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
                Assertions.assertThrows(TimeoutException.class, () -> match.get(300, TimeUnit.MILLISECONDS));
                assertAnomalyIsNotLockedByMatcher(completion, pair.host().launchedEventId());
            } finally {
                setStatus(completion, pair.guest().launchedEventId(), EventStatus.FAILED);
                completion.commit();
            }
            Assertions.assertFalse(match.get(5, TimeUnit.SECONDS));
        }
        assertSearching(pair.host());
        assertSearching(pair.guest());
        Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(pair.host()));
        assertSeparateParticipants(pair);
    }

    @Test
    void competingOppositeMatchesUseOneParentLockOrderWithoutDeadlock() throws Exception {
        final var pair = pair();
        final var reverse = pair.guest().matchWith(
            pair.host().groupId(), pair.host().ownerPersonageId(), pair.host().launchedEventId()
        );
        final var ready = new CountDownLatch(2);
        final var start = new CountDownLatch(1);
        try (final var first = runtime();
             final var second = runtime();
             final var executor = Executors.newFixedThreadPool(2)) {
            final var forward = executor.submit(() -> {
                ready.countDown();
                Assertions.assertTrue(start.await(5, TimeUnit.SECONDS));
                return dao(first).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId());
            });
            final var backward = executor.submit(() -> {
                ready.countDown();
                Assertions.assertTrue(start.await(5, TimeUnit.SECONDS));
                return dao(second).tryMergeSearchingInto(reverse, pair.host().launchedEventId());
            });
            Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            final var firstWon = forward.get(10, TimeUnit.SECONDS);
            final var secondWon = backward.get(10, TimeUnit.SECONDS);
            Assertions.assertNotEquals(firstWon, secondWon, "Exactly one merge can reserve a pair");
        }
        Assertions.assertEquals(1, List.of(status(pair.host()), status(pair.guest())).stream()
            .filter(value -> value == EventStatus.CANCELED.id()).count());
        Assertions.assertEquals(1, jdbc.queryForObject("""
            SELECT count(DISTINCT launched_event_id) FROM personage_to_event
            WHERE personage_id IN (?, ?)
            """, Integer.class, pair.host().ownerPersonageId().value(), pair.guest().ownerPersonageId().value()));
    }

    @Test
    void successfulMergeAndItsReplayPreserveExistingParticipantTransfer() {
        final var pair = pair();
        try (final var context = runtime()) {
            Assertions.assertTrue(dao(context).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId()));
        }
        final var merged = snapshot(pair);
        try (final var restarted = runtime()) {
            Assertions.assertFalse(dao(restarted).tryMergeSearchingInto(pair.accepted(), pair.guest().launchedEventId()));
        }
        Assertions.assertEquals(merged, snapshot(pair));
        Assertions.assertEquals(EventStatus.LAUNCHED.id(), status(pair.host()));
        Assertions.assertEquals(EventStatus.CANCELED.id(), status(pair.guest()));
        Assertions.assertEquals(2, jdbc.queryForObject("""
            SELECT count(*) FROM personage_to_event WHERE launched_event_id = ?
            """, Integer.class, pair.host().launchedEventId()));
        Assertions.assertEquals(2, jdbc.queryForObject("""
            SELECT count(*) FROM launched_event_to_pgroup WHERE launched_event_id = ?
            """, Integer.class, pair.host().launchedEventId()));
    }

    private Pair pair() {
        final var host = searching();
        final var guest = searching();
        return new Pair(host, guest, host.matchWith(guest.groupId(), guest.ownerPersonageId(), guest.launchedEventId()));
    }

    private Anomaly.Dangerous.Searching searching() {
        final var groupId = jdbc.queryForObject("""
            INSERT INTO pgroup (is_active, init_date, next_event_date, next_rumor_date,
                event_intervals_setting, time_zone_setting, name, active_badge_id)
            VALUES (true, now(), now(), now(), '{}'::json, '+03:00', 'Merge test',
                (SELECT id FROM badge WHERE code = 'standard')) RETURNING id
            """, Long.class);
        final var owner = new PersonageDao(
            source, new JsonUtils(), new PersonageConfig(Duration.ofHours(8), 200, 5)
        ).createDefault("Anomaly Merge");
        final var eventId = jdbc.queryForObject("""
            INSERT INTO launched_event (event_id, start_date, end_date, status_id)
            SELECT id, now() - interval '1 hour', now() - interval '1 minute', ?
            FROM event WHERE code = 'anomaly-expedition' RETURNING id
            """, Long.class, EventStatus.LAUNCHED.id());
        jdbc.update("INSERT INTO personage_to_event (personage_id, launched_event_id) VALUES (?, ?)", owner.value(), eventId);
        jdbc.update("INSERT INTO launched_event_to_pgroup (launched_event_id, pgroup_id) VALUES (?, ?)", eventId, groupId);
        final var searching = new Anomaly.Dangerous.Searching(eventId, GroupId.from(groupId), owner, 1000);
        new AnomalyPostgresDao(source, new JsonUtils()).save(searching);
        return searching;
    }

    private List<List<Map<String, Object>>> snapshot(Pair pair) {
        return List.of(
            jdbc.queryForList("SELECT * FROM anomaly WHERE launched_event_id IN (?, ?) ORDER BY launched_event_id",
                pair.host().launchedEventId(), pair.guest().launchedEventId()),
            jdbc.queryForList("SELECT * FROM launched_event WHERE id IN (?, ?) ORDER BY id",
                pair.host().launchedEventId(), pair.guest().launchedEventId()),
            jdbc.queryForList("SELECT * FROM personage_to_event WHERE personage_id IN (?, ?) ORDER BY personage_id",
                pair.host().ownerPersonageId().value(), pair.guest().ownerPersonageId().value()),
            jdbc.queryForList("SELECT * FROM launched_event_to_pgroup WHERE pgroup_id IN (?, ?) ORDER BY pgroup_id",
                pair.host().groupId().value(), pair.guest().groupId().value())
        );
    }

    private void assertSearching(Anomaly.Dangerous.Searching searching) {
        Assertions.assertEquals(searching, new AnomalyPostgresDao(source, new JsonUtils())
            .findByLaunchedEventId(searching.launchedEventId()).orElseThrow());
    }

    private int status(Anomaly.Dangerous.Searching searching) {
        return jdbc.queryForObject("SELECT status_id FROM launched_event WHERE id = ?", Integer.class, searching.launchedEventId());
    }

    private void assertSeparateParticipants(Pair pair) {
        for (final var expedition : List.of(pair.host(), pair.guest())) {
            Assertions.assertEquals(expedition.launchedEventId(), jdbc.queryForObject(
                "SELECT launched_event_id FROM personage_to_event WHERE personage_id = ?",
                Long.class, expedition.ownerPersonageId().value()
            ));
        }
    }

    private static void lock(Connection connection, long eventId) throws Exception {
        try (final var statement = connection.prepareStatement("SELECT id FROM launched_event WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, eventId);
            try (final var result = statement.executeQuery()) {
                Assertions.assertTrue(result.next());
            }
        }
    }

    private static void setStatus(Connection connection, long eventId, EventStatus status) throws Exception {
        try (final var statement = connection.prepareStatement("UPDATE launched_event SET status_id = ? WHERE id = ?")) {
            statement.setInt(1, status.id());
            statement.setLong(2, eventId);
            Assertions.assertEquals(1, statement.executeUpdate());
        }
    }

    private static void assertAnomalyIsNotLockedByMatcher(Connection connection, long eventId) throws Exception {
        final var savepoint = connection.setSavepoint();
        try (final var statement = connection.prepareStatement(
            "SELECT launched_event_id FROM anomaly WHERE launched_event_id = ? FOR UPDATE NOWAIT"
        )) {
            statement.setLong(1, eventId);
            try (final var result = statement.executeQuery()) {
                Assertions.assertTrue(result.next());
            }
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }

    private AnnotationConfigApplicationContext runtime() {
        final var context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> source(true));
        context.register(RuntimeConfiguration.class);
        context.refresh();
        Assertions.assertTrue(AopUtils.isAopProxy(dao(context)));
        return context;
    }

    private static AnomalyPostgresDao dao(AnnotationConfigApplicationContext context) {
        return context.getBean(AnomalyPostgresDao.class);
    }

    private PGSimpleDataSource source(boolean useSchema) {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(System.getenv("SEEKER_ANOMALY_POSTGRES_TEST_URL"));
        dataSource.setUser(System.getenv("SEEKER_ANOMALY_POSTGRES_TEST_USER"));
        dataSource.setPassword(System.getenv("SEEKER_ANOMALY_POSTGRES_TEST_PASSWORD"));
        dataSource.setConnectTimeout(5);
        dataSource.setSocketTimeout(20);
        dataSource.setOptions("-c lock_timeout=5000 -c statement_timeout=15000");
        if (useSchema) {
            dataSource.setCurrentSchema(schema);
        }
        dataSource.setApplicationName("anomaly_merge_194_" + UUID.randomUUID());
        return dataSource;
    }

    private record Pair(
        Anomaly.Dangerous.Searching host,
        Anomaly.Dangerous.Searching guest,
        Anomaly.Dangerous.Accepted accepted
    ) {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import(AnomalyPostgresDao.class)
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
