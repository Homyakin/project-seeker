package ru.homyakin.seeker.event.launch.chat;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SaveChatEventDao {
    private static final String SAVE_CHAT_EVENT = """
        insert into chat_event (launched_event_id, chat_id, message_id)
        values (:launched_event_id, :chat_id, :message_id);
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SaveChatEventDao(DataSource dataSource) {
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
}
