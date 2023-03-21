package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.models.GroupUser;

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

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupUserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(GroupUser groupUser) {
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupUser.groupId());
        params.put("usertg_id", groupUser.userId());
        params.put("is_active", groupUser.isActive());
        jdbcTemplate.update(
            SAVE_GROUP_USER,
            params
        );
    }

    public Optional<GroupUser> getByGroupIdAndUserId(long groupId, long userId) {
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId);
        params.put("usertg_id", userId);
        final var result = jdbcTemplate.query(
            GET_GROUP_USER_BY_KEY,
            params,
            this::mapRow
        );
        return result.stream().findFirst();

    }

    public int countUsersInGroup(long groupId) {
        final var sql = """
                    SELECT count(*) as count FROM grouptg_to_usertg
                    WHERE grouptg_id = :grouptg_id and is_active = true""";
        final var param = Collections.singletonMap("grouptg_id", groupId);
        return jdbcTemplate.query(
            sql,
            param,
            (rs, rowNum) -> rs.getInt("count")
        ).get(0);
    }

    public Optional<GroupUser> getRandomUserByGroup(long groupId) {
        final var sql = """
                    SELECT * FROM grouptg_to_usertg
                    WHERE grouptg_id = :grouptg_id and is_active = true
                    ORDER BY random() LIMIT 1""";
        final var param = Collections.singletonMap("grouptg_id", groupId);
        return jdbcTemplate.query(
            sql,
            param,
            this::mapRow
        ).stream().findFirst();
    }

    public void update(GroupUser groupUser) {
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupUser.groupId());
        params.put("usertg_id", groupUser.userId());
        params.put("is_active", groupUser.isActive());
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    private GroupUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupUser(
            rs.getLong("grouptg_id"),
            rs.getLong("usertg_id"),
            rs.getBoolean("is_active")
        );
    }
}
