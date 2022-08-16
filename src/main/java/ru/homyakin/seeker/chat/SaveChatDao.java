package ru.homyakin.seeker.chat;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.models.Chat;

@Component
class SaveChatDao {
    private static final String SAVE_CHAT = """
        insert into chat (id, is_active, lang)
        values (:id, :is_active, :lang)
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SaveChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(Chat chat) {
        final var params = new HashMap<String, Object>();
        params.put("id", chat.id());
        params.put("is_active", chat.isActive());
        params.put("lang", chat.language().value());
        jdbcTemplate.update(
            SAVE_CHAT,
            params
        );
    }
}
