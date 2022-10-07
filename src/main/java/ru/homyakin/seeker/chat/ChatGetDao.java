package ru.homyakin.seeker.chat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
class ChatGetDao {
    private static final String GET_CHAT_BY_ID = "SELECT * FROM chat WHERE id = :id";
    private static final String GET_CHAT_WITH_LESS_NEXT_EVENT_DATE = "SELECT * FROM chat WHERE next_event_date  < :next_event_date";
    private static final ChatRowMapper CHAT_ROW_MAPPER = new ChatRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatGetDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
