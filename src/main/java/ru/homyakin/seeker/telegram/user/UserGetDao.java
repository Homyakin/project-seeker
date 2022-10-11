package ru.homyakin.seeker.telegram.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
class UserGetDao {
    private static final String GET_USER_BY_ID = "SELECT * FROM tg_user WHERE id = :id";
    private static final UserRowMapper USER_ROW_MAPPER = new UserRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserGetDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
