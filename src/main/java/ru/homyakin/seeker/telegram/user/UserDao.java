package ru.homyakin.seeker.telegram.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.user.model.User;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class UserDao {
    private static final String SAVE_USER = """
        insert into tg_user (id, is_active_private_messages, lang, init_date, character_id)
        values (:id, :is_active_private_messages, :lang, :init_date, :character_id);
        """;
    private static final String GET_USER_BY_ID = "SELECT * FROM tg_user WHERE id = :id";
    private static final String UPDATE_ACTIVE_PRIVATE_MESSAGES = """
        update tg_user
        set is_active_private_messages = :is_active_private_messages
        where id = :id;
        """;
    private static final String UPDATE_LANGUAGE = """
        update tg_user
        set lang = :lang
        where id = :id;
        """;
    private static final UserRowMapper USER_ROW_MAPPER = new UserRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(User user) {
        final var params = new HashMap<String, Object>() {{
            put("id", user.id());
            put("is_active_private_messages", user.isActivePrivateMessages());
            put("lang", user.language().id());
            put("init_date", TimeUtils.moscowTime());
            put("character_id", user.characterId());
        }};
        jdbcTemplate.update(
            SAVE_USER,
            params
        );
    }

    public Optional<User> getById(Long userId) {
        final var params = Collections.singletonMap("id", userId);
        final var result = jdbcTemplate.query(
            GET_USER_BY_ID,
            params,
            USER_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public void updateIsActivePrivateMessages(Long userId, boolean isActivePrivateMessages) {
        final var params = new HashMap<String, Object>() {{
            put("id", userId);
            put("is_active", isActivePrivateMessages);
        }};
        jdbcTemplate.update(
            UPDATE_ACTIVE_PRIVATE_MESSAGES,
            params
        );
    }

    public void updateLanguage(Long userId, Language language) {
        final var params = new HashMap<String, Object>() {{
            put("id", userId);
            put("lang", language.id());
        }};
        jdbcTemplate.update(
            UPDATE_LANGUAGE,
            params
        );
    }

    private static class UserRowMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                rs.getLong("id"),
                rs.getBoolean("is_active_private_messages"),
                Language.getOrDefault(rs.getInt("lang")),
                rs.getLong("character_id")
            );
        }
    }
}
