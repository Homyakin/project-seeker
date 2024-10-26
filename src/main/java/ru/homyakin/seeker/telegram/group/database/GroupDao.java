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
import ru.homyakin.seeker.utils.DatabaseUtils;

@Component
public class GroupDao {
    private static final String SAVE_GROUP = """
        INSERT INTO grouptg (id, language_id, pgroup_id)
        VALUES (:id, :language_id, :pgroup_id)
        """;
    private static final String UPDATE = """
        UPDATE grouptg SET language_id = :language_id WHERE id = :id;
        """;

    private final JdbcClient jdbcClient;

    public GroupDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupTg group) {
        jdbcClient.sql(SAVE_GROUP)
            .param("id", group.id().value())
            .param("language_id", group.language().id())
            .param("pgroup_id", group.domainGroupId().value())
            .update();
    }

    public Optional<GroupTg> getById(GroupTgId groupId) {
        final var getByDirectId = "SELECT * FROM grouptg WHERE id = :id";
        final var result = jdbcClient.sql(getByDirectId)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (result.get().isPresent()) {
            return result.get();
        }

        final var getByMigratedId = """
            SELECT
                migrated_from_grouptg_id as id,
                language_id,
                pgroup_id
            FROM grouptg WHERE migrated_from_grouptg_id = :group_id
        """;
        return jdbcClient.sql(getByMigratedId)
            .param("group_id", groupId.value())
            .query(this::mapRow)
            .optional()
            .flatMap(it -> it);
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
                INNER JOIN pgroup p ON g.pgroup_id = p.id
                JOIN pgroup_to_personage ptp ON p.id = ptp.pgroup_id
                WHERE p.is_active = TRUE
                    AND ptp.is_active = TRUE
                GROUP BY g.id
                HAVING COUNT(ptp.personage_id) > 2
            ) active_groups;
            """;
        return jdbcClient.sql(sql)
            .query((rs, _) -> rs.getLong(1))
            .single();
    }

    public Optional<GroupTg> getByDomainId(GroupId groupId) {
        final var sql = """
            SELECT * FROM grouptg
            WHERE pgroup_id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional()
            .flatMap(it -> it);
    }

    private Optional<GroupTg> mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var domainGroupId = DatabaseUtils.getLongOrNull(rs, "pgroup_id");
        if (domainGroupId == null) {
            return Optional.empty();
        }
        return Optional.of(
            new GroupTg(
                GroupTgId.from(rs.getLong("id")),
                Language.getOrDefault(rs.getInt("language_id")),
                GroupId.from(rs.getLong("pgroup_id"))
            )
        );
    }
}
