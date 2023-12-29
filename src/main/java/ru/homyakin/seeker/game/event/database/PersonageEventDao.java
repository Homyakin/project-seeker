package ru.homyakin.seeker.game.event.database;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class PersonageEventDao {
    private static final String SAVE_USER_EVENT = """
        insert into personage_to_event (personage_id, launched_event_id)
        values (:personage_id, :launched_event_id);
        """;

    private final JdbcClient jdbcClient;

    public PersonageEventDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(PersonageId personageId, long launchedEventId) {
        jdbcClient.sql(SAVE_USER_EVENT)
            .param("personage_id", personageId.value())
            .param("launched_event_id", launchedEventId)
            .update();
    }
}
