package ru.homyakin.seeker.telegram.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class UserDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(User user) {
        final var params = new HashMap<String, Object>();
        params.put("id", user.id());
        params.put("is_active_private_messages", user.isActivePrivateMessages());
        params.put("language_id", user.language().id());
        params.put("init_date", TimeUtils.moscowTime());
        params.put("personage_id", user.personageId().value());

        jdbcTemplate.update(
            SAVE_USER,
            params
        );
    }

    public Optional<User> getById(UserId userId) {
        final var result = jdbcTemplate.query(
            GET_USER_BY_ID,
            Collections.singletonMap("id", userId.value()),
            this::mapRow
        );
        return result.stream().findFirst();
    }

    public Optional<User> getByPersonageId(PersonageId personageId) {
        final var result = jdbcTemplate.query(
            GET_BY_PERSONAGE_ID,
            Collections.singletonMap("personage_id", personageId.value()),
            this::mapRow
        );
        return result.stream().findFirst();
    }

    public Optional<User> getByUsernameInGroup(String username, GroupId groupId) {
        //TODO багфикс
        final var params = new MapSqlParameterSource()
            .addValue("username", username);
        return jdbcTemplate.query(GET_BY_USERNAME, params, this::mapRow).stream().findFirst();
    }

    public void updateUsername(UserId userId, String newUsername) {
        final var params = new MapSqlParameterSource()
            .addValue("username", newUsername)
            .addValue("id", userId.value());
        jdbcTemplate.update(UPDATE_USERNAME, params);
    }

    public void update(User user) {
        final var params = new MapSqlParameterSource()
            .addValue("id", user.id())
            .addValue("language_id", user.language().id())
            .addValue("is_active_private_messages", user.isActivePrivateMessages());
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new User(
            new UserId(rs.getLong("id")),
            rs.getBoolean("is_active_private_messages"),
            Language.getOrDefault(rs.getInt("language_id")),
            PersonageId.from(rs.getLong("personage_id")),
            Optional.ofNullable(rs.getString("username"))
        );
    }

    private static final String SAVE_USER = """
        insert into usertg (id, is_active_private_messages, language_id, init_date, personage_id)
        values (:id, :is_active_private_messages, :language_id, :init_date, :personage_id);
        """;
    private static final String GET_USER_BY_ID = """
        SELECT * FROM usertg
        WHERE id = :id
        """;
    private static final String GET_BY_PERSONAGE_ID = "SELECT * FROM usertg WHERE personage_id = :personage_id";
    private static final String UPDATE = """
        update usertg
        set is_active_private_messages = :is_active_private_messages, language_id = :language_id
        where id = :id
        """;
    private static final String GET_BY_USERNAME = """
        SELECT u.* FROM usertg u
        LEFT JOIN grouptg_to_usertg gtu on u.id = gtu.usertg_id
        WHERE u.username = :username
        AND gtu.is_active = true
        """;
    private static final String UPDATE_USERNAME = "UPDATE usertg SET username = :username WHERE id = :id";
}
