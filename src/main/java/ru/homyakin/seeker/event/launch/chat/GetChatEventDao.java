package ru.homyakin.seeker.event.launch.chat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class GetChatEventDao {
    private static final String GET_LAUNCHED_EVENT_BY_ID = "SELECT * FROM chat_event WHERE launched_event_id = :launched_event_id";
    private static final ChatEventRowMapper LAUNCHED_EVENT_ROW_MAPPER = new ChatEventRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GetChatEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<ChatEvent> getByLaunchedEventId(Long launchedEventId) {
        final var params = Collections.singletonMap("launched_event_id", launchedEventId);
        return jdbcTemplate.query(
            GET_LAUNCHED_EVENT_BY_ID,
            params,
            LAUNCHED_EVENT_ROW_MAPPER
        );
    }

    private static class ChatEventRowMapper implements RowMapper<ChatEvent> {

        @Override
        public ChatEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChatEvent(
                rs.getLong("launched_event_id"),
                rs.getLong("chat_id"),
                rs.getInt("message_id")
            );
        }
    }
}
