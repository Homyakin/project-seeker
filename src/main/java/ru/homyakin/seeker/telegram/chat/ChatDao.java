package ru.homyakin.seeker.telegram.chat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
class ChatDao {
    private static final String GET_CHAT_BY_ID = "SELECT * FROM chat WHERE id = :id";
    private static final String GET_CHAT_WITH_LESS_NEXT_EVENT_DATE = "SELECT * FROM chat WHERE next_event_date  < :next_event_date";
    private static final String SAVE_CHAT = """
        insert into chat (id, is_active, lang, init_date, next_event_date)
        values (:id, :is_active, :lang, :init_date, :next_event_date)
        """;
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

    private static final ChatRowMapper CHAT_ROW_MAPPER = new ChatRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(Chat chat) {
        final var params = new HashMap<String, Object>() {{

            put("id", chat.id());
            put("is_active", chat.isActive());
            put("lang", chat.language().id());
            put("init_date", chat.nextEventDate());
            put("next_event_date", chat.nextEventDate());
        }};
        jdbcTemplate.update(
            SAVE_CHAT,
            params
        );
    }

    public Optional<Chat> getById(Long chatId) {
        final var params = Collections.singletonMap("id", chatId);
        final var result = jdbcTemplate.query(
            GET_CHAT_BY_ID,
            params,
            CHAT_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public List<Chat> getGetChatsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        final var params = Collections.singletonMap("next_event_date", maxNextEventDate);
        return jdbcTemplate.query(
            GET_CHAT_WITH_LESS_NEXT_EVENT_DATE,
            params,
            CHAT_ROW_MAPPER
        );
    }

    public void updateIsActive(Long chatId, boolean isActive) {
        final var params = new HashMap<String, Object>() {{
            put("id", chatId);
            put("is_active", isActive);
        }};
        jdbcTemplate.update(
            UPDATE_ACTIVE,
            params
        );
    }

    public void updateLanguage(Long chatId, Language language) {
        final var params = new HashMap<String, Object>() {{
            put("id", chatId);
            put("lang", language.id());
        }};
        jdbcTemplate.update(
            UPDATE_LANGUAGE,
            params
        );
    }

    public void updateNextEventDate(Long chatId, LocalDateTime nextEventDate) {
        final var params = new HashMap<String, Object>() {{
            put("id", chatId);
            put("next_event_date", nextEventDate);
        }};
        jdbcTemplate.update(
            UPDATE_NEXT_EVENT_DATE,
            params
        );
    }

    private static class ChatRowMapper implements RowMapper<Chat> {

        @Override
        public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Chat(
                rs.getLong("id"),
                rs.getBoolean("is_active"),
                Language.getOrDefault(rs.getInt("lang")),
                rs.getTimestamp("next_event_date").toLocalDateTime()
            );
        }
    }
}
