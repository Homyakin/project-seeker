package ru.homyakin.seeker.event.launch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class GetLaunchedEventDao {
    private static final String GET_CHAT_EVENT_BY_ID = "SELECT * FROM launched_event WHERE id = :id";
    private static final String GET_ACTIVE_CHAT_EVENTS_BY_USER_ID = """
        SELECT * FROM user_event ue
         LEFT JOIN launched_event le on ue.launched_event_id = le.id
         WHERE ue.user_id = :user_id
         AND le.is_active = true
        """;
    private static final String GET_ACTIVE_EVENTS_WITH_LESS_END_DATE = """
        SELECT * FROM launched_event WHERE end_date <= :end_date AND is_active = true;
        """;
    private static final LaunchedEventRowMapper LAUNCHED_EVENT_ROW_MAPPER = new LaunchedEventRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GetLaunchedEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Optional<LaunchedEvent> getById(Long chatEventId) {
        final var params = Collections.singletonMap("id", chatEventId);
        final var result = jdbcTemplate.query(
            GET_CHAT_EVENT_BY_ID,
            params,
            LAUNCHED_EVENT_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public Optional<LaunchedEvent> getActiveByUserId(Long userId) {
        final var params = Collections.singletonMap("user_id", userId);
        final var result = jdbcTemplate.query(
            GET_ACTIVE_CHAT_EVENTS_BY_USER_ID,
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
