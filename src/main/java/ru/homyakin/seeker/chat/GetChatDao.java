package ru.homyakin.seeker.chat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.models.Chat;
import ru.homyakin.seeker.models.Language;

@Component
class GetChatDao {
    private static final String GET_CHAT_BY_ID = "SELECT * FROM chat WHERE id = :id";
    private static final ChatRowMapper chatRowMapper = new ChatRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GetChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Optional<Chat> getById(Long chatId) {
        final var params = Collections.singletonMap("id", chatId);
        final var result = jdbcTemplate.query(
            GET_CHAT_BY_ID,
            params,
            chatRowMapper
        );
        return result.stream().findFirst();
    }

    private static class ChatRowMapper implements RowMapper<Chat> {

        @Override
        public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Chat(
                rs.getLong("id"),
                rs.getBoolean("is_active"),
                Language.getOrDefault(rs.getInt("lang")),
                rs.getTimestamp("last_event_date").toLocalDateTime()
            );
        }
    }
}
