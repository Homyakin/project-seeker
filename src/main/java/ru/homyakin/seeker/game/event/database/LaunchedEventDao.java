package ru.homyakin.seeker.game.event.database;

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
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Component
public class LaunchedEventDao {
    private static final String GET_LAUNCHED_EVENT_BY_ID = """
        SELECT * FROM launched_event WHERE id = :id
        """;
    private static final String GET_ACTIVE_EVENTS_BY_PERSONAGE_ID = """
        SELECT * FROM personage_to_event pe
         LEFT JOIN launched_event le on pe.launched_event_id = le.id
         WHERE pe.personage_id = :personage_id
         AND le.status_id = :status_id
        """;
    private static final String GET_ACTIVE_EVENTS_WITH_LESS_END_DATE = """
        SELECT * FROM launched_event WHERE end_date <= :end_date AND status_id = :status_id;
        """;
    private static final String UPDATE_STATUS = """
        update launched_event
        set status_id = :status_id
        where id = :id;
        """;
    private static final String LAST_ENDED_EVENT_IN_GROUP = """
        SELECT le.* FROM launched_event le
        LEFT JOIN public.grouptg_to_launched_event gtle on le.id = gtle.launched_event_id
        WHERE gtle.grouptg_id = :grouptg_id
        AND le.status_id != :active_status_id
        ORDER BY le.id DESC
        LIMIT 1
        """;
    private final JdbcClient jdbcClient;
    private final SimpleJdbcInsert jdbcInsert;

    public LaunchedEventDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("launched_event")
            .usingColumns(
                "event_id",
                "start_date",
                "end_date",
                "status_id"
            )
            .usingGeneratedKeyColumns("id");
        jdbcClient = JdbcClient.create(dataSource);
    }

    public long save(int eventId, LocalDateTime start, LocalDateTime end) {
        final var params = new HashMap<String, Object>();
        params.put("event_id", eventId);
        params.put("start_date", start);
        params.put("end_date", end);
        params.put("status_id", EventStatus.LAUNCHED.id());
        return jdbcInsert.executeAndReturnKey(
            params
        ).longValue();
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return jdbcClient.sql(GET_LAUNCHED_EVENT_BY_ID)
            .param("id", launchedEventId)
            .query(this::mapRow)
            .optional();
    }

    public Optional<LaunchedEvent> getActiveByPersonageId(PersonageId personageId) {
        return jdbcClient.sql(GET_ACTIVE_EVENTS_BY_PERSONAGE_ID)
            .param("personage_id", personageId.value())
            .param("status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .optional();
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

    public Optional<LaunchedEvent> lastEndedEventInGroup(GroupId groupId) {
        return jdbcClient.sql(LAST_ENDED_EVENT_IN_GROUP)
            .param("grouptg_id", groupId.value())
            .param("active_status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .optional();
    }

    private LaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LaunchedEvent(
            rs.getLong("id"),
            rs.getInt("event_id"),
            rs.getTimestamp("start_date").toLocalDateTime(),
            rs.getTimestamp("end_date").toLocalDateTime(),
            EventStatus.findById(rs.getInt("status_id"))
        );
    }
}
