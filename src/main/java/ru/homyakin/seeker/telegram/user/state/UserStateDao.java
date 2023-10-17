package ru.homyakin.seeker.telegram.user.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

@Repository
public class UserStateDao {
    private static final Logger logger = LoggerFactory.getLogger(UserStateDao.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserStateDao(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Optional<UserState> getUserStateById(long userId) {
        final var sql = "SELECT * FROM usertg_state WHERE usertg_id = :usertg_id";
        return jdbcTemplate
            .query(sql, Collections.singletonMap("usertg_id", userId), this::mapRow)
            .stream()
            .findFirst();
    }

    public void setUserState(long userId, UserState state) {
        final var sql = """
        INSERT INTO usertg_state (usertg_id, state) VALUES (:usertg_id, :state)
        ON CONFLICT (usertg_id) DO UPDATE SET state = :state
        """;
        final var params = new HashMap<String, Object>();
        params.put("usertg_id", userId);
        params.put("state", mapUserStateToJson(state));
        jdbcTemplate.update(sql, params);
    }

    public void clearUserState(long userId) {
        final var sql = "DELETE FROM usertg_state WHERE usertg_id = :usertg_id";
        jdbcTemplate.update(sql, Collections.singletonMap("usertg_id", userId));
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
