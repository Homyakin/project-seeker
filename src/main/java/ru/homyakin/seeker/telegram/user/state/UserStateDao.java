package ru.homyakin.seeker.telegram.user.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Repository
public class UserStateDao {
    private static final Logger logger = LoggerFactory.getLogger(UserStateDao.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JdbcClient jdbcClient;

    public UserStateDao(DataSource dataSource) {
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
            .param("state", mapUserStateToJson(state))
            .update();
    }

    public void clearUserState(UserId userId) {
        final var sql = "DELETE FROM usertg_state WHERE usertg_id = :usertg_id";
        jdbcClient.sql(sql)
            .param("usertg_id", userId.value())
            .update();
    }

    private UserState mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            return objectMapper.readValue(rs.getString("state"), UserState.class);
        } catch (JsonProcessingException e) {
            logger.error("Can't parse user state", e);
            throw new IllegalStateException(e);
        }
    }

    private PGobject mapUserStateToJson(UserState state) {
        try {
            PGobject jsonObject = new PGobject();
            final var jsonStr = objectMapper.writeValueAsString(state);
            jsonObject.setType("json");
            jsonObject.setValue(jsonStr);
            return jsonObject;
        } catch (JsonProcessingException | SQLException e) {
            logger.error("Can't deserialize user state", e);
            throw new IllegalStateException(e);
        }
    }
}
