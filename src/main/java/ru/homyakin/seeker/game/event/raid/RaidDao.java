package ru.homyakin.seeker.game.event.raid;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RaidDao {
    private static final String GET_RANDOM_EVENT = "SELECT * FROM raid WHERE event_id = :event_id";
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RaidDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Optional<Raid> getByEventId(int eventId) {
        return jdbcTemplate.query(
            GET_RANDOM_EVENT,
            Collections.singletonMap("event_id", eventId),
            this::mapRow
        ).stream().findFirst();
    }

    private Raid mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Raid(
            rs.getInt("event_id"),
            RaidTemplate.get(rs.getInt("template_id"))
        );
    }
}
