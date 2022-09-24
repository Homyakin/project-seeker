package ru.homyakin.seeker.chat;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SaveChatDao {
    private static final String SAVE_CHAT = """
        insert into chat (id, is_active, lang, init_date, next_event_date)
        values (:id, :is_active, :lang, :init_date, :next_event_date)
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SaveChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(Chat chat) {
        final var params = new HashMap<String, Object>();
        params.put("id", chat.id());
        params.put("is_active", chat.isActive());
        params.put("lang", chat.language().id());
        params.put("init_date", chat.nextEventDate());
        params.put("next_event_date", chat.nextEventDate());
        jdbcTemplate.update(
            SAVE_CHAT,
            params
        );
    }
}
