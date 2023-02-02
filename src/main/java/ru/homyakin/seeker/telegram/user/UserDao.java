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
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class UserDao {
    private static final String SAVE_USER = """
        insert into usertg (id, is_active_private_messages, language_id, init_date, personage_id)
        values (:id, :is_active_private_messages, :language_id, :init_date, :personage_id);
        """;
    private static final String GET_USER_BY_ID = """
        SELECT * FROM usertg
        WHERE id = :id
        """;
    private static final String UPDATE = """
        update usertg
        set is_active_private_messages = :is_active_private_messages, language_id = :language_id
        where id = :id
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
            put("language_id", user.language().id());
            put("init_date", TimeUtils.moscowTime());
            put("personage_id", user.personageId());
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

    public void update(User user) {
        final var params = new HashMap<String, Object>() {{
            put("id", user.id());
            put("language_id", user.language().id());
            put("is_active_private_messages", user.isActivePrivateMessages());
        }};
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    private static class UserRowMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                rs.getLong("id"),
                rs.getBoolean("is_active_private_messages"),
                Language.getOrDefault(rs.getInt("language_id")),
                rs.getLong("personage_id")
            );
        }
    }
}
