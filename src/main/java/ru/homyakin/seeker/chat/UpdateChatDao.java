package ru.homyakin.seeker.chat;

import java.time.LocalDateTime;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
class UpdateChatDao {
    private static final String UPDATE_ACTIVE = """
        update chat
        set is_active = :is_active
        where id = :id;
        """;

    private static final String UPDATE_LANGUAGE = """
        update chat
        set lang = :lang
        where id = :id;
        """;

    private static final String UPDATE_NEXT_EVENT_DATE = """
        update chat
        set next_event_date = :next_event_date
        where id = :id;
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UpdateChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void updateIsActive(Long chatId, boolean isActive) {
        final var params = new HashMap<String, Object>();
        params.put("id", chatId);
        params.put("is_active", isActive);
        jdbcTemplate.update(
            UPDATE_ACTIVE,
            params
        );
    }

    public void updateLanguage(Long chatId, Language language) {
        final var params = new HashMap<String, Object>();
        params.put("id", chatId);
        params.put("lang", language.id());
        jdbcTemplate.update(
            UPDATE_LANGUAGE,
            params
        );
    }

    public void updateNextEventDate(Long chatId, LocalDateTime nextEventDate) {
        final var params = new HashMap<String, Object>();
        params.put("id", chatId);
        params.put("next_event_date", nextEventDate);
        jdbcTemplate.update(
            UPDATE_NEXT_EVENT_DATE,
            params
        );
    }
}
