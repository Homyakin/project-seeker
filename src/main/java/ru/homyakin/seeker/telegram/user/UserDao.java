package ru.homyakin.seeker.telegram.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class UserDao {
    private final JdbcClient jdbcClient;

    public UserDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(User user) {
        jdbcClient.sql(SAVE_USER)
            .param("id", user.id().value())
            .param("is_active_private_messages", user.isActivePrivateMessages())
            .param("language_id", user.language().id())
            .param("init_date", TimeUtils.moscowTime())
            .param("personage_id", user.personageId().value())
            .update();
    }

    public Optional<User> getById(UserId userId) {
        return jdbcClient.sql(GET_USER_BY_ID)
            .param("id", userId.value())
            .query(this::mapRow)
            .optional();
    }

    public Optional<User> getByPersonageId(PersonageId personageId) {
        return jdbcClient.sql(GET_BY_PERSONAGE_ID)
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional();
    }

    public Optional<User> getByUsernameInGroup(String username, GroupId groupId) {
        return jdbcClient.sql(GET_BY_USERNAME)
            .param("username", username)
            .param("grouptg_id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    public void updateUsername(UserId userId, String newUsername) {
        jdbcClient.sql(UPDATE_USERNAME)
            .param("username", newUsername)
            .param("id", userId.value())
            .update();
    }

    public void update(User user) {
        jdbcClient.sql(UPDATE)
            .param("id", user.id().value())
            .param("language_id", user.language().id())
            .param("is_active_private_messages", user.isActivePrivateMessages())
            .update();
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
        AND gtu.grouptg_id = :grouptg_id
        AND gtu.is_active = true
        """;
    private static final String UPDATE_USERNAME = "UPDATE usertg SET username = :username WHERE id = :id";
}
