package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class GroupDao {
    private static final String GET_GROUP_BY_ID = "SELECT * FROM grouptg WHERE id = :id";
    private static final String SAVE_GROUP = """
        INSERT INTO grouptg (id, language_id, pgroup_id)
        VALUES (:id, :language_id, :pgroup_id)
        """;
    private static final String UPDATE = """
        UPDATE grouptg SET language_id = :language_id WHERE id = :id;
        """;

    private final JdbcClient jdbcClient;

    public GroupDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupTg group) {
        jdbcClient.sql(SAVE_GROUP)
            .param("id", group.id().value())
            .param("language_id", group.language().id())
            .update();
    }

    public Optional<GroupTg> getById(GroupTgId groupId) {
        return jdbcClient.sql(GET_GROUP_BY_ID)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    public void update(GroupTg group) {
        jdbcClient.sql(UPDATE)
            .param("id", group.id().value())
            .param("language_id", group.language().id())
            .update();
    }

    public long getActiveGroupsCount() {
        // Считаем <=2 пользователя неактивной группой
        final var sql = """
            SELECT COUNT(*) FROM (
                SELECT g.id
                FROM grouptg g
                JOIN grouptg_to_usertg gu ON g.id = gu.grouptg_id
                WHERE g.is_active = TRUE
                    AND gu.is_active = TRUE
                GROUP BY g.id
                HAVING COUNT(gu.usertg_id) > 2
            ) active_groups;
            """;
        return jdbcClient.sql(sql)
            .query((rs, _) -> rs.getLong(1))
            .single();
    }

    public Optional<GroupTg> get(GroupId groupId) {
        final var sql = """
            SELECT * FROM grouptg
            WHERE pgroup_id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    private GroupTg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupTg(
            GroupTgId.from(rs.getLong("id")),
            Language.getOrDefault(rs.getInt("language_id")),
            GroupId.from(rs.getLong("pgroup_id"))
        );
    }
}
