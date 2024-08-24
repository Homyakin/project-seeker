package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;

@Component
public class EventDao {
    // На маленьких данных работает быстро. Если понадобится ускорить - https://habr.com/ru/post/242999/
    private static final String GET_RANDOM_EVENT = "SELECT * FROM event WHERE is_enabled = true ORDER BY random() LIMIT 1";
    private static final String GET_EVENT_BY_ID = "SELECT * FROM event WHERE id = :id";
    private static final String SAVE_EVENT = """
        INSERT INTO event (type_id, is_enabled, code)
        VALUES (:type_id, :is_enabled, :code)
        ON CONFLICT (type_id, code)
        DO UPDATE SET is_enabled = :is_enabled
        RETURNING id
        """;
    private final JdbcClient jdbcClient;

    public EventDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<Event> getRandomEvent() {
        return jdbcClient.sql(GET_RANDOM_EVENT)
            .query(this::mapEvent)
            .optional();
    }

    public Optional<Event> getById(Integer eventId) {
        return jdbcClient.sql(GET_EVENT_BY_ID)
            .param("id", eventId)
            .query(this::mapEvent)
            .optional();
    }

    public int save(SavingRaid raid) {
        return jdbcClient.sql(SAVE_EVENT)
            .param("type_id", EventType.RAID.id())
            .param("is_enabled", raid.isEnabled())
            .param("code", raid.code())
            .query((rs, _) -> rs.getInt("id"))
            .single();
    }

    private Event mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new Event(
            rs.getInt("id"),
            EventType.get(rs.getInt("type_id")),
            rs.getString("code")
        );
    }
}
