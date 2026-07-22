package ru.homyakin.seeker.game.event.anomaly.infra.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPhase;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.EventParams;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class AnomalyPostgresDao implements AnomalyStorage {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public AnomalyPostgresDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void save(Anomaly anomaly) {
        final var sql = """
            INSERT INTO anomaly (
                launched_event_id, pgroup_id, owner_personage_id, phase, mode,
                modifier_code, roster_locked, opponent_launched_event_id,
                gvg_rating_at_start, is_challenge
            ) VALUES (
                :launched_event_id, :pgroup_id, :owner_personage_id, :phase, :mode,
                :modifier_code, :roster_locked, :opponent_launched_event_id,
                :gvg_rating_at_start, :is_challenge
            )
            """;
        bind(anomaly, jdbcClient.sql(sql)).update();
    }

    @Override
    public void update(Anomaly anomaly) {
        final var sql = """
            UPDATE anomaly
            SET owner_personage_id = :owner_personage_id,
                phase = :phase,
                mode = :mode,
                modifier_code = :modifier_code,
                roster_locked = :roster_locked,
                opponent_launched_event_id = :opponent_launched_event_id,
                gvg_rating_at_start = :gvg_rating_at_start,
                is_challenge = :is_challenge
            WHERE launched_event_id = :launched_event_id
            """;
        bind(anomaly, jdbcClient.sql(sql)).update();
    }

    @Override
    public Optional<Anomaly> findByLaunchedEventId(long launchedEventId) {
        final var sql = "SELECT * FROM anomaly WHERE launched_event_id = :launched_event_id";
        return jdbcClient.sql(sql)
            .param("launched_event_id", launchedEventId)
            .query(this::mapAnomaly)
            .optional();
    }

    @Override
    public Optional<LaunchedEvent> findActiveLaunchedEventByGroupId(GroupId groupId) {
        final var sql = """
            SELECT le.*
            FROM anomaly a
            INNER JOIN launched_event le ON le.id = a.launched_event_id
            WHERE a.pgroup_id = :pgroup_id
              AND le.status_id = :launched_status
            ORDER BY le.id DESC
            LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("launched_status", EventStatus.LAUNCHED.id())
            .query(this::mapLaunchedEvent)
            .optional();
    }

    @Override
    public List<LaunchedEvent> findActiveSearchingWithoutOpponent() {
        final var sql = """
            SELECT le.*
            FROM anomaly a
            INNER JOIN launched_event le ON le.id = a.launched_event_id
            WHERE le.status_id = :launched_status
              AND a.phase = :phase
              AND a.opponent_launched_event_id IS NULL
            """;
        return jdbcClient.sql(sql)
            .param("launched_status", EventStatus.LAUNCHED.id())
            .param("phase", AnomalyPhase.SEARCHING.name())
            .query(this::mapLaunchedEvent)
            .list();
    }

    @Override
    public boolean hasActiveAnomaly(GroupId groupId) {
        return findActiveLaunchedEventByGroupId(groupId).isPresent();
    }

    @Override
    public boolean hasStartOnDate(GroupId groupId, LocalDate date) {
        final var sql = """
            SELECT 1
            FROM anomaly a
            INNER JOIN launched_event le ON le.id = a.launched_event_id
            WHERE a.pgroup_id = :pgroup_id
              AND a.is_challenge = false
              AND le.start_date::date = :day_date
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("day_date", date)
            .query((rs, _) -> true)
            .optional()
            .isPresent();
    }

    private org.springframework.jdbc.core.simple.JdbcClient.StatementSpec bind(
        Anomaly anomaly,
        org.springframework.jdbc.core.simple.JdbcClient.StatementSpec spec
    ) {
        return spec
            .param("launched_event_id", anomaly.launchedEventId())
            .param("pgroup_id", anomaly.groupId().value())
            .param("owner_personage_id", anomaly.ownerPersonageId().map(PersonageId::value).orElse(null))
            .param("phase", anomaly.phase().name())
            .param("mode", anomaly.mode().map(Enum::name).orElse(null))
            .param("modifier_code", anomaly.modifierCode())
            .param("roster_locked", anomaly.rosterLocked())
            .param("opponent_launched_event_id", anomaly.opponentLaunchedEventId().orElse(null))
            .param("gvg_rating_at_start", anomaly.gvgRatingAtStart().orElse(null))
            .param("is_challenge", anomaly.isChallenge());
    }

    private Anomaly mapAnomaly(ResultSet rs, int rowNum) throws SQLException {
        final var ownerId = rs.getObject("owner_personage_id");
        final var mode = rs.getString("mode");
        final var opponentId = rs.getObject("opponent_launched_event_id");
        final var gvgRating = rs.getObject("gvg_rating_at_start");
        return new Anomaly(
            rs.getLong("launched_event_id"),
            GroupId.from(rs.getLong("pgroup_id")),
            Optional.ofNullable(ownerId).map(id -> PersonageId.from(((Number) id).longValue())),
            AnomalyPhase.valueOf(rs.getString("phase")),
            Optional.ofNullable(mode).map(AnomalyMode::valueOf),
            rs.getString("modifier_code"),
            rs.getBoolean("roster_locked"),
            Optional.ofNullable(opponentId).map(id -> ((Number) id).longValue()),
            Optional.ofNullable(gvgRating).map(id -> ((Number) id).intValue()),
            rs.getBoolean("is_challenge")
        );
    }

    private LaunchedEvent mapLaunchedEvent(ResultSet rs, int rowNum) throws SQLException {
        return new LaunchedEvent(
            rs.getLong("id"),
            rs.getInt("event_id"),
            rs.getTimestamp("start_date").toLocalDateTime(),
            rs.getTimestamp("end_date").toLocalDateTime(),
            EventStatus.findById(rs.getInt("status_id")),
            Optional.ofNullable(rs.getString("event_params"))
                .map(it -> jsonUtils.fromString(it, EventParams.class))
        );
    }
}
