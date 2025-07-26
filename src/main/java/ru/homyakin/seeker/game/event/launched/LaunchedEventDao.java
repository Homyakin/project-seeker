package ru.homyakin.seeker.game.event.launched;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class LaunchedEventDao {
    private static final String GET_LAUNCHED_EVENT_BY_ID = """
        SELECT * FROM launched_event WHERE id = :id
        """;
    private static final String GET_ACTIVE_EVENTS_WITH_LESS_END_DATE = """
        SELECT * FROM launched_event WHERE end_date <= :end_date AND status_id = :status_id;
        """;
    private static final String UPDATE_STATUS = """
        update launched_event
        set status_id = :status_id
        where id = :id;
        """;
    private static final String UPDATE_STATUS_AND_EVENT_PARAMS = """
        update launched_event
        set status_id = :status_id, event_params = :event_params
        where id = :id;
        """;
    private final JdbcClient jdbcClient;
    private final SimpleJdbcInsert jdbcInsert;
    private final JsonUtils jsonUtils;

    public LaunchedEventDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("launched_event")
            .usingColumns(
                "event_id",
                "start_date",
                "end_date",
                "status_id",
                "event_params"
            )
            .usingGeneratedKeyColumns("id");
        jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public long save(int eventId, LocalDateTime start, LocalDateTime end) {
        return save(eventId, start, end, EventStatus.LAUNCHED, Optional.empty());
    }

    public long save(int eventId, LocalDateTime start, LocalDateTime end, EventStatus status) {
        return save(eventId, start, end, status, Optional.empty());
    }

    public long save(int eventId, LocalDateTime start, LocalDateTime end, Optional<EventParams> eventParams) {
        return save(eventId, start, end, EventStatus.LAUNCHED, eventParams);
    }

    public long save(
        int eventId,
        LocalDateTime start,
        LocalDateTime end,
        EventStatus status,
        Optional<EventParams> eventParams
    ) {
        final var params = new HashMap<String, Object>();
        params.put("event_id", eventId);
        params.put("start_date", start);
        params.put("end_date", end);
        params.put("status_id", status.id());
        params.put("event_params", eventParams.map(jsonUtils::mapToPostgresJson).orElse(null));
        return jdbcInsert.executeAndReturnKey(params).longValue();
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return jdbcClient.sql(GET_LAUNCHED_EVENT_BY_ID)
            .param("id", launchedEventId)
            .query(this::mapRow)
            .optional();
    }

    public List<CurrentEvent> getActiveEventsByPersonageId(PersonageId personageId) {
        final var sql = """
            SELECT
                le.id,
                le.end_date,
                e.type_id
             FROM personage_to_event pe
             LEFT JOIN launched_event le on pe.launched_event_id = le.id
             LEFT JOIN event e on le.event_id = e.id
             WHERE pe.personage_id = :personage_id
             AND le.status_id = :status_id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("status_id", EventStatus.LAUNCHED.id())
            .query((rs, _) -> new CurrentEvent(
                rs.getLong("id"),
                EventType.get(rs.getInt("type_id")),
                rs.getTimestamp("end_date").toLocalDateTime()
            ))
            .list();
    }

    public List<LaunchedEvent> getActiveEventsWithLessEndDate(LocalDateTime maxEndDate) {
        return jdbcClient.sql(GET_ACTIVE_EVENTS_WITH_LESS_END_DATE)
            .param("end_date", maxEndDate)
            .param("status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .list();
    }

    public void updateStatus(Long launchedEventId, EventStatus status) {
        jdbcClient.sql(UPDATE_STATUS)
            .param("id", launchedEventId)
            .param("status_id", status.id())
            .update();
    }

    public void updateStatusAndEventParams(Long launchedEventId, EventStatus status, EventParams eventParams) {
        jdbcClient.sql(UPDATE_STATUS_AND_EVENT_PARAMS)
            .param("id", launchedEventId)
            .param("status_id", status.id())
            .param("event_params", jsonUtils.mapToPostgresJson(eventParams))
            .update();
    }

    public int countFailedPersonalQuestsRowForPersonage(PersonageId personageId) {
        final var sql = """
            WITH last_success AS (
                SELECT MAX(launched_event_id) AS last_success_raid -- could be null
                FROM personage_to_event pte
                LEFT JOIN public.launched_event le on pte.launched_event_id = le.id
                LEFT JOIN event e on le.event_id = e.id
                WHERE pte.personage_id = :personage_id
                AND le.status_id = :success_id
                AND e.type_id = :quest_id
            )
            SELECT COUNT(*) AS failed_quests_count
            FROM personage_to_event pte
            LEFT JOIN public.launched_event le on pte.launched_event_id = le.id
            LEFT JOIN event e on le.event_id = e.id
            WHERE pte.personage_id = :personage_id
            AND le.status_id = :fail_id
            AND e.type_id = :quest_id
            AND le.id > -- more id => newer event
                COALESCE((SELECT last_success_raid FROM last_success), -1) -- all events id > 0
        """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("quest_id", EventType.PERSONAL_QUEST.id())
            .query((rs, _) -> rs.getInt("failed_quests_count"))
            .single();
    }

    private LaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
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
