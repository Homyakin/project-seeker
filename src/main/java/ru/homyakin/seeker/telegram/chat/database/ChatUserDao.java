package ru.homyakin.seeker.telegram.chat.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.chat.model.ChatUser;

@Component
public class ChatUserDao {
    private static final String GET_CHAT_BY_KEY = """
        SELECT * FROM chat_to_tg_user
        WHERE chat_id = :chat_id and tg_user_id = :tg_user_id
        """;
    private static final String SAVE_CHAT_USER = """
        insert into chat_to_tg_user (chat_id, tg_user_id, is_active)
        values (:chat_id, :tg_user_id, :is_active)
        """;
    private static final String UPDATE = """
        update chat_to_tg_user
        set is_active = :is_active
        where chat_id = :chat_id and tg_user_id = :tg_user_id
        """;
    private static final ChatUserRowMapper CHAT_USER_ROW_MAPPER = new ChatUserRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatUserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(ChatUser chatUser) {
        final var params = new HashMap<String, Object>() {{
            put("chat_id", chatUser.chatId());
            put("tg_user_id", chatUser.userId());
            put("is_active", chatUser.isActive());
        }};
        jdbcTemplate.update(
            SAVE_CHAT_USER,
            params
        );
    }

    public Optional<ChatUser> getByChatIdAndUserId(long chatId, long userId) {
        final var params = new HashMap<String, Object>() {{
            put("chat_id", chatId);
            put("tg_user_id", userId);
        }};
        final var result = jdbcTemplate.query(
            GET_CHAT_BY_KEY,
            params,
            CHAT_USER_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public void update(ChatUser chatUser) {
        final var params = new HashMap<String, Object>() {{
            put("chat_id", chatUser.chatId());
            put("tg_user_id", chatUser.userId());
            put("is_active", chatUser.isActive());
        }};
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    private static class ChatUserRowMapper implements RowMapper<ChatUser> {

        @Override
        public ChatUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChatUser(
                rs.getLong("chat_id"),
                rs.getLong("tg_user_id"),
                rs.getBoolean("is_active")
            );
        }
    }
}
