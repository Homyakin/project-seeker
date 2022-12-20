package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class LaunchedEventDao {
    private static final String GET_LAUNCHED_EVENT_BY_ID = """
        SELECT * FROM launched_event WHERE id = :id
        """;
    private static final String GET_ACTIVE_EVENTS_BY_PERSONAGE_ID = """
        SELECT * FROM personage_to_event pe
         LEFT JOIN launched_event le on pe.launched_event_id = le.id
         WHERE pe.personage_id = :personage_id
         AND le.is_active = true
        """;
    private static final String GET_ACTIVE_EVENTS_WITH_LESS_END_DATE = """
        SELECT * FROM launched_event WHERE end_date <= :end_date AND is_active = true;
        """;
    private static final String UPDATE_ACTIVE = """
        update launched_event
        set is_active = :is_active
        where id = :id;
        """;
    private static final LaunchedEventRowMapper LAUNCHED_EVENT_ROW_MAPPER = new LaunchedEventRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public LaunchedEventDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("launched_event")
            .usingColumns(
                "event_id",
                "start_date",
                "end_date",
                "is_active"
            )
            .usingGeneratedKeyColumns("id");
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long save(Event event) {
        final var startDate = TimeUtils.moscowTime();
        final var params = new HashMap<String, Object>() {{
            put("event_id", event.id());
            put("start_date", startDate);
            put("end_date", startDate.plus(event.duration()).plus(event.period()));
            put("is_active", true);
        }};
        return jdbcInsert.executeAndReturnKey(
            params
        ).longValue();
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        final var params = Collections.singletonMap("id", launchedEventId);
        final var result = jdbcTemplate.query(
            GET_LAUNCHED_EVENT_BY_ID,
            params,
            LAUNCHED_EVENT_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public Optional<LaunchedEvent> getActiveByPersonageId(Long personageId) {
        final var params = Collections.singletonMap("personage_id", personageId);
        final var result = jdbcTemplate.query(
            GET_ACTIVE_EVENTS_BY_PERSONAGE_ID,
            params,
            LAUNCHED_EVENT_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public List<LaunchedEvent> getActiveEventsWithLessEndDate(LocalDateTime maxEndDate) {
        final var params = Collections.singletonMap("end_date", maxEndDate);
        return jdbcTemplate.query(
            GET_ACTIVE_EVENTS_WITH_LESS_END_DATE,
            params,
            LAUNCHED_EVENT_ROW_MAPPER
        );
    }

    public void updateIsActive(Long launchedEventId, boolean isActive) {
        final var params = new HashMap<String, Object>() {{
            put("id", launchedEventId);
            put("is_active", isActive);
        }};
        jdbcTemplate.update(
            UPDATE_ACTIVE,
            params
        );
    }

    private static class LaunchedEventRowMapper implements RowMapper<LaunchedEvent> {

        @Override
        public LaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new LaunchedEvent(
                rs.getLong("id"),
                rs.getInt("event_id"),
                rs.getTimestamp("start_date").toLocalDateTime(),
                rs.getTimestamp("end_date").toLocalDateTime(),
                rs.getBoolean("is_active")
            );
        }
    }
}
