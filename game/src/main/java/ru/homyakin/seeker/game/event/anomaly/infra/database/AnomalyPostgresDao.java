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
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.EventParams;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class AnomalyPostgresDao implements AnomalyStorage {
    private static final String NONE_CODE = "none";

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
                pve_template_code, opponent_pgroup_id,
                opponent_owner_personage_id, winner_pgroup_id,
                gvg_rating_at_start, search_end_date
            ) VALUES (
                :launched_event_id, :pgroup_id, :owner_personage_id, :phase, :mode,
                :pve_template_code, :opponent_pgroup_id,
                :opponent_owner_personage_id, :winner_pgroup_id,
                :gvg_rating_at_start, :search_end_date
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
                pve_template_code = :pve_template_code,
                opponent_pgroup_id = :opponent_pgroup_id,
                opponent_owner_personage_id = :opponent_owner_personage_id,
                winner_pgroup_id = :winner_pgroup_id,
                gvg_rating_at_start = :gvg_rating_at_start,
                search_end_date = :search_end_date
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
            WHERE (a.pgroup_id = :pgroup_id OR a.opponent_pgroup_id = :pgroup_id)
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
              AND a.opponent_pgroup_id IS NULL
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
              AND le.start_date::date = :day_date
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("day_date", date)
            .query((rs, _) -> true)
            .optional()
            .isPresent();
    }

    private JdbcClient.StatementSpec bind(Anomaly anomaly, JdbcClient.StatementSpec spec) {
        return spec
            .param("launched_event_id", anomaly.launchedEventId())
            .param("pgroup_id", anomaly.groupId().value())
            .param("owner_personage_id", anomaly.ownerPersonageId().value())
            .param("phase", toPhase(anomaly).name())
            .param("mode", toMode(anomaly).map(Enum::name).orElse(null))
            .param("pve_template_code", toPveTemplateCode(anomaly))
            .param("opponent_pgroup_id", toOpponentGroupId(anomaly).map(GroupId::value).orElse(null))
            .param(
                "opponent_owner_personage_id",
                toOpponentOwnerId(anomaly).map(PersonageId::value).orElse(null)
            )
            .param("winner_pgroup_id", toWinnerGroupId(anomaly).map(GroupId::value).orElse(null))
            .param("gvg_rating_at_start", toGvgRating(anomaly).orElse(null))
            .param("search_end_date", toSearchEndDate(anomaly).orElse(null));
    }

    private Anomaly mapAnomaly(ResultSet rs, int rowNum) throws SQLException {
        final var launchedEventId = rs.getLong("launched_event_id");
        final var groupId = GroupId.from(rs.getLong("pgroup_id"));
        final var owner = PersonageId.from(rs.getLong("owner_personage_id"));
        final var phase = AnomalyPhase.valueOf(rs.getString("phase"));
        final var mode = Optional.ofNullable(rs.getString("mode")).map(AnomalyMode::valueOf);
        final var templateCode = rs.getString("pve_template_code");
        final var opponentGroupId = Optional.ofNullable(rs.getObject("opponent_pgroup_id"))
            .map(id -> GroupId.from(((Number) id).longValue()));
        final var opponentOwner = Optional.ofNullable(rs.getObject("opponent_owner_personage_id"))
            .map(id -> PersonageId.from(((Number) id).longValue()));
        final var winnerGroupId = Optional.ofNullable(rs.getObject("winner_pgroup_id"))
            .map(id -> GroupId.from(((Number) id).longValue()));
        final var gvgRating = Optional.ofNullable(rs.getObject("gvg_rating_at_start"))
            .map(id -> ((Number) id).intValue());
        final var searchEnd = Optional.ofNullable(rs.getTimestamp("search_end_date"))
            .map(java.sql.Timestamp::toLocalDateTime);

        return switch (phase) {
            case GATHERING -> switch (mode.orElseThrow()) {
                case SAFE -> new Anomaly.Safe(
                    launchedEventId,
                    groupId,
                    owner,
                    requireTemplate(templateCode),
                    Anomaly.Safe.Phase.GATHERING
                );
                case DANGEROUS -> new Anomaly.Dangerous.Gathering(
                    launchedEventId,
                    groupId,
                    owner
                );
            };
            case PVE_WAITING -> new Anomaly.Safe(
                launchedEventId,
                groupId,
                owner,
                requireTemplate(templateCode),
                Anomaly.Safe.Phase.PVE_WAITING
            );
            case SEARCHING -> new Anomaly.Dangerous.Searching(
                launchedEventId,
                groupId,
                owner,
                requireGvgRating(gvgRating, launchedEventId),
                searchEnd.orElseThrow(() -> new IllegalStateException(
                    "Searching anomaly without search_end_date: " + launchedEventId
                ))
            );
            case CHALLENGED -> new Anomaly.Dangerous.Challenged(
                launchedEventId,
                groupId,
                owner,
                opponentGroupId.orElseThrow(() -> new IllegalStateException(
                    "Challenged anomaly without opponent: " + launchedEventId
                )),
                requireGvgRating(gvgRating, launchedEventId),
                searchEnd.orElseThrow(() -> new IllegalStateException(
                    "Challenged anomaly without search_end_date: " + launchedEventId
                ))
            );
            case ACCEPTED -> new Anomaly.Dangerous.Accepted(
                launchedEventId,
                groupId,
                owner,
                opponentGroupId.orElseThrow(() -> new IllegalStateException(
                    "Accepted anomaly without opponent: " + launchedEventId
                )),
                opponentOwner.orElseThrow(() -> new IllegalStateException(
                    "Accepted anomaly without opponent owner: " + launchedEventId
                )),
                winnerGroupId,
                requireGvgRating(gvgRating, launchedEventId),
                searchEnd.orElseThrow(() -> new IllegalStateException(
                    "Accepted anomaly without search_end_date: " + launchedEventId
                ))
            );
        };
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

    private static AnomalyPhase toPhase(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Safe safe -> switch (safe.phase()) {
                case GATHERING -> AnomalyPhase.GATHERING;
                case PVE_WAITING -> AnomalyPhase.PVE_WAITING;
            };
            case Anomaly.Dangerous.Gathering _ -> AnomalyPhase.GATHERING;
            case Anomaly.Dangerous.Searching _ -> AnomalyPhase.SEARCHING;
            case Anomaly.Dangerous.Challenged _ -> AnomalyPhase.CHALLENGED;
            case Anomaly.Dangerous.Accepted _ -> AnomalyPhase.ACCEPTED;
        };
    }

    private static Optional<AnomalyMode> toMode(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Safe _ -> Optional.of(AnomalyMode.SAFE);
            case Anomaly.Dangerous _ -> Optional.of(AnomalyMode.DANGEROUS);
        };
    }

    private static String toPveTemplateCode(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Safe safe -> safe.template().code();
            case Anomaly.Dangerous _ -> NONE_CODE;
        };
    }

    private static Optional<GroupId> toOpponentGroupId(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Dangerous.Challenged challenged -> Optional.of(challenged.opponentGroupId());
            case Anomaly.Dangerous.Accepted accepted -> Optional.of(accepted.opponentGroupId());
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _, Anomaly.Dangerous.Searching _ ->
                Optional.empty();
        };
    }

    private static Optional<PersonageId> toOpponentOwnerId(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Dangerous.Accepted accepted -> Optional.of(accepted.opponentOwnerPersonageId());
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _, Anomaly.Dangerous.Searching _,
                 Anomaly.Dangerous.Challenged _ -> Optional.empty();
        };
    }

    private static Optional<GroupId> toWinnerGroupId(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Dangerous.Accepted accepted -> accepted.winnerGroupId();
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _, Anomaly.Dangerous.Searching _,
                 Anomaly.Dangerous.Challenged _ -> Optional.empty();
        };
    }

    private static Optional<Integer> toGvgRating(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Dangerous.Searching searching -> Optional.of(searching.gvgRatingAtStart());
            case Anomaly.Dangerous.Challenged challenged -> Optional.of(challenged.gvgRatingAtStart());
            case Anomaly.Dangerous.Accepted accepted -> Optional.of(accepted.gvgRatingAtStart());
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _ -> Optional.empty();
        };
    }

    private static Optional<java.time.LocalDateTime> toSearchEndDate(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.Dangerous.Searching searching -> Optional.of(searching.searchEndDate());
            case Anomaly.Dangerous.Challenged challenged -> Optional.of(challenged.searchEndDate());
            case Anomaly.Dangerous.Accepted accepted -> Optional.of(accepted.searchEndDate());
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _ -> Optional.empty();
        };
    }

    private static int requireGvgRating(Optional<Integer> gvgRating, long launchedEventId) {
        return gvgRating.orElseThrow(() -> new IllegalStateException(
            "Dangerous anomaly without gvg_rating_at_start: " + launchedEventId
        ));
    }

    private static AnomalyPveTemplate requireTemplate(String code) {
        return AnomalyPveTemplate.findByCode(code)
            .orElseThrow(() -> new IllegalStateException("Unknown safe PvE template: " + code));
    }
}
