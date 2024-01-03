package ru.homyakin.seeker.game.event.raid;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;

@Component
public class RaidDao {
    private static final String GET_RANDOM_EVENT = "SELECT * FROM raid WHERE event_id = :event_id";
    private final JdbcClient jdbcClient;

    public RaidDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<Raid> getByEventId(int eventId) {
        return jdbcClient.sql(GET_RANDOM_EVENT)
            .param("event_id", eventId)
            .query(this::mapRow)
            .optional();
    }

    private Raid mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Raid(
            rs.getInt("event_id"),
            RaidTemplate.get(rs.getInt("template_id"))
        );
    }
}
