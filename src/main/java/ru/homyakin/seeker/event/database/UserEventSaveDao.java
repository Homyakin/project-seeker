package ru.homyakin.seeker.event.database;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventSaveDao {
    private static final String SAVE_USER_EVENT = """
        insert into user_event (user_id, launched_event_id)
        values (:user_id, :launched_event_id);
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserEventSaveDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(Long userId, Long launchedEventId) {
        final var params = new HashMap<String, Object>() {{
            put("user_id", userId);
            put("launched_event_id", launchedEventId);
        }};

        jdbcTemplate.update(
            SAVE_USER_EVENT,
            params
        );
    }
}
