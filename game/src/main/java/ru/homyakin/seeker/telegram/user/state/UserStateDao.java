package ru.homyakin.seeker.telegram.user.state;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class UserStateDao {
    private final JsonUtils jsonUtils;
    private final JdbcClient jdbcClient;

    public UserStateDao(JsonUtils jsonUtils, DataSource dataSource) {
        this.jsonUtils = jsonUtils;
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<UserState> getUserStateById(UserId userId) {
        final var sql = "SELECT * FROM usertg_state WHERE usertg_id = :usertg_id";
        return jdbcClient.sql(sql)
            .param("usertg_id", userId.value())
            .query(this::mapRow)
            .optional();
    }

    public void setUserState(UserId userId, UserState state) {
        final var sql = """
            INSERT INTO usertg_state (usertg_id, state) VALUES (:usertg_id, :state)
            ON CONFLICT (usertg_id) DO UPDATE SET state = :state
            """;
        jdbcClient.sql(sql)
            .param("usertg_id", userId.value())
            .param("state", jsonUtils.mapToPostgresJson(state))
            .update();
    }

    public void clearUserState(UserId userId) {
        final var sql = "DELETE FROM usertg_state WHERE usertg_id = :usertg_id";
        jdbcClient.sql(sql)
            .param("usertg_id", userId.value())
            .update();
    }

    private UserState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return jsonUtils.fromString(rs.getString("state"), UserState.class);
    }
}
