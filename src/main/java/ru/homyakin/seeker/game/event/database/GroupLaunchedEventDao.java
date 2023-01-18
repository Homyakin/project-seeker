package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;

@Component
public class GroupLaunchedEventDao {
    private static final String SAVE_GROUP_LAUNCHED_EVENT = """
        insert into group_to_launched_event (launched_event_id, group_id, message_id)
        values (:launched_event_id, :group_id, :message_id);
        """;
    private static final String GET_GROUP_LAUNCHED_EVENT_BY_ID =
        "SELECT * FROM group_to_launched_event WHERE launched_event_id = :launched_event_id";
    private static final GroupEventRowMapper GROUP_LAUNCHED_EVENT_ROW_MAPPER = new GroupEventRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupLaunchedEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(GroupLaunchedEvent groupLaunchedEvent) {
        final var params = new HashMap<String, Object>() {{
            put("launched_event_id", groupLaunchedEvent.launchedEventId());
            put("group_id", groupLaunchedEvent.groupId());
            put("message_id", groupLaunchedEvent.messageId());
        }};

        jdbcTemplate.update(
            SAVE_GROUP_LAUNCHED_EVENT,
            params
        );
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        final var params = Collections.singletonMap("launched_event_id", launchedEventId);
        return jdbcTemplate.query(
            GET_GROUP_LAUNCHED_EVENT_BY_ID,
            params,
            GROUP_LAUNCHED_EVENT_ROW_MAPPER
        );
    }

    private static class GroupEventRowMapper implements RowMapper<GroupLaunchedEvent> {
        @Override
        public GroupLaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GroupLaunchedEvent(
                rs.getLong("launched_event_id"),
                rs.getLong("group_id"),
                rs.getInt("message_id")
            );
        }
    }
}
