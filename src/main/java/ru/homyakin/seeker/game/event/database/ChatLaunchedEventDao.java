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
import ru.homyakin.seeker.game.event.models.ChatLaunchedEvent;

@Component
public class ChatLaunchedEventDao {
    private static final String SAVE_CHAT_LAUNCHED_EVENT = """
        insert into chat_to_launched_event (launched_event_id, chat_id, message_id)
        values (:launched_event_id, :chat_id, :message_id);
        """;
    private static final String GET_CHAT_LAUNCHED_EVENT_BY_ID = "SELECT * FROM chat_to_launched_event WHERE launched_event_id = :launched_event_id";
    private static final ChatEventRowMapper CHAT_LAUNCHED_EVENT_ROW_MAPPER = new ChatEventRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatLaunchedEventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(ChatLaunchedEvent chatLaunchedEvent) {
        final var params = new HashMap<String, Object>() {{
            put("launched_event_id", chatLaunchedEvent.launchedEventId());
            put("chat_id", chatLaunchedEvent.chatId());
            put("message_id", chatLaunchedEvent.messageId());
        }};

        jdbcTemplate.update(
            SAVE_CHAT_LAUNCHED_EVENT,
            params
        );
    }

    public List<ChatLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        final var params = Collections.singletonMap("launched_event_id", launchedEventId);
        return jdbcTemplate.query(
            GET_CHAT_LAUNCHED_EVENT_BY_ID,
            params,
            CHAT_LAUNCHED_EVENT_ROW_MAPPER
        );
    }

    private static class ChatEventRowMapper implements RowMapper<ChatLaunchedEvent> {

        @Override
        public ChatLaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChatLaunchedEvent(
                rs.getLong("launched_event_id"),
                rs.getLong("chat_id"),
                rs.getInt("message_id")
            );
        }
    }
}
