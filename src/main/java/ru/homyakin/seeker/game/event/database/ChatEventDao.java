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
import ru.homyakin.seeker.game.event.models.ChatEvent;

@Component
public class ChatEventDao {
    private static final String SAVE_CHAT_EVENT = """
        insert into chat_event (launched_event_id, chat_id, message_id)
        values (:launched_event_id, :chat_id, :message_id);
        """;
    private static final String GET_LAUNCHED_EVENT_BY_ID = "SELECT * FROM chat_event WHERE launched_event_id = :launched_event_id";
    private static final ChatEventRowMapper LAUNCHED_EVENT_ROW_MAPPER = new ChatEventRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(ChatEvent chatEvent) {
        final var params = new HashMap<String, Object>() {{
            put("launched_event_id", chatEvent.launchedEventId());
            put("chat_id", chatEvent.chatId());
            put("message_id", chatEvent.messageId());
        }};

        jdbcTemplate.update(
            SAVE_CHAT_EVENT,
            params
        );
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
