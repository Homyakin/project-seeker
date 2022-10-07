package ru.homyakin.seeker.event.database;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.event.models.Event;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class SaveLaunchedEventDao {
    private final SimpleJdbcInsert jdbcInsert;

    public SaveLaunchedEventDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("launched_event")
            .usingColumns(
                "event_id",
                "start_date",
                "end_date",
                "is_active"
            );
        jdbcInsert.setGeneratedKeyName("id");
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
}
