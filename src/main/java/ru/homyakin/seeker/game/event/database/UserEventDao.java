package ru.homyakin.seeker.game.event.database;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventDao {
    private static final String SAVE_USER_EVENT = """
        insert into personage_event (personage_id, launched_event_id)
        values (:personage_id, :launched_event_id);
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(Long personageId, Long launchedEventId) {
        final var params = new HashMap<String, Object>() {{
            put("personage_id", personageId);
            put("launched_event_id", launchedEventId);
        }};

        jdbcTemplate.update(
            SAVE_USER_EVENT,
            params
        );
    }
}
