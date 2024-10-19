package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.group.models.GroupUser;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Component
public class GroupUserDao {
    private static final String GET_GROUP_USER_BY_KEY = """
        SELECT * FROM grouptg_to_usertg
        WHERE grouptg_id = :grouptg_id and usertg_id = :usertg_id
        """;
    private static final String SAVE_GROUP_USER = """
        insert into grouptg_to_usertg (grouptg_id, usertg_id, is_active)
        values (:grouptg_id, :usertg_id, :is_active)
        """;
    private static final String UPDATE = """
        update grouptg_to_usertg
        set is_active = :is_active
        where grouptg_id = :grouptg_id and usertg_id = :usertg_id
        """;

    private final JdbcClient jdbcClient;

    public GroupUserDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupUser groupUser) {
        jdbcClient.sql(SAVE_GROUP_USER)
            .param("grouptg_id", groupUser.groupId().value())
            .param("usertg_id", groupUser.userId().value())
            .param("is_active", groupUser.isActive())
            .update();
    }

    public Optional<GroupUser> getByGroupIdAndUserId(GroupTgId groupId, UserId userId) {
        return jdbcClient.sql(GET_GROUP_USER_BY_KEY)
            .param("grouptg_id", groupId.value())
            .param("usertg_id", userId.value())
            .query(this::mapRow)
            .optional();

    }

    public int countUsersInGroup(GroupTgId groupId) {
        final var sql = """
                    SELECT count(*) as count FROM grouptg_to_usertg
                    WHERE grouptg_id = :grouptg_id and is_active = true""";
        return jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .query(Integer.class)
            .optional()
            .orElseThrow();
    }

    public Optional<GroupUser> getRandomUserByGroup(GroupTgId groupId) {
        final var sql = """
                    SELECT * FROM grouptg_to_usertg
                    WHERE grouptg_id = :grouptg_id and is_active = true
                    ORDER BY random() LIMIT 1""";
        return jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    public void update(GroupUser groupUser) {
        jdbcClient.sql(UPDATE)
            .param("grouptg_id", groupUser.groupId().value())
            .param("usertg_id", groupUser.userId().value())
            .param("is_active", groupUser.isActive())
            .update();
    }

    private GroupUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupUser(
            GroupTgId.from(rs.getLong("grouptg_id")),
            UserId.from(rs.getLong("usertg_id")),
            rs.getBoolean("is_active")
        );
    }
}
