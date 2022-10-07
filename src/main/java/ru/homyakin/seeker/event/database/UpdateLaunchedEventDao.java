package ru.homyakin.seeker.event.database;

import java.time.LocalDateTime;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
public class UpdateLaunchedEventDao {
    private static final String UPDATE_ACTIVE = """
        update launched_event
        set is_active = :is_active
        where id = :id;
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UpdateLaunchedEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void updateIsActive(Long launchedEventId, boolean isActive) {
        final var params = new HashMap<String, Object>();
        params.put("id", launchedEventId);
        params.put("is_active", isActive);
        jdbcTemplate.update(
            UPDATE_ACTIVE,
            params
        );
    }
}
