package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.models.GroupUser;

@Component
public class GroupUserDao {
    private static final String GET_GROUP_USER_BY_KEY = """
        SELECT * FROM group_to_user
        WHERE group_id = :group_id and user_id = :user_id
        """;
    private static final String SAVE_GROUP_USER = """
        insert into group_to_user (group_id, user_id, is_active)
        values (:group_id, :user_id, :is_active)
        """;
    private static final String UPDATE = """
        update group_to_user
        set is_active = :is_active
        where group_id = :group_id and user_id = :user_id
        """;
    private static final GroupUserRowMapper GROUP_USER_ROW_MAPPER = new GroupUserRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupUserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(GroupUser groupUser) {
        final var params = new HashMap<String, Object>() {{
            put("group_id", groupUser.groupId());
            put("user_id", groupUser.userId());
            put("is_active", groupUser.isActive());
        }};
        jdbcTemplate.update(
            SAVE_GROUP_USER,
            params
        );
    }

    public Optional<GroupUser> getByGroupIdAndUserId(long groupId, long userId) {
        final var params = new HashMap<String, Object>() {{
            put("group_id", groupId);
            put("user_id", userId);
        }};
        final var result = jdbcTemplate.query(
            GET_GROUP_USER_BY_KEY,
            params,
            GROUP_USER_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public void update(GroupUser groupUser) {
        final var params = new HashMap<String, Object>() {{
            put("group_id", groupUser.groupId());
            put("user_id", groupUser.userId());
            put("is_active", groupUser.isActive());
        }};
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    private static class GroupUserRowMapper implements RowMapper<GroupUser> {

        @Override
        public GroupUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GroupUser(
                rs.getLong("group_id"),
                rs.getLong("user_id"),
                rs.getBoolean("is_active")
            );
        }
    }
}
