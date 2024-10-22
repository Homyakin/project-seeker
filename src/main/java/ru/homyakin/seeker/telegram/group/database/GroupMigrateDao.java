package ru.homyakin.seeker.telegram.group.database;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.utils.DatabaseUtils;

@Component
public class GroupMigrateDao {
    private final JdbcClient jdbcClient;

    public GroupMigrateDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Transactional
    public void migrate(GroupTgId from, GroupTgId to) {
        final var newDomainGroup = """
            SELECT pgroup_id FROM grouptg WHERE id = :id
            """;
        jdbcClient.sql(newDomainGroup)
            .param("id", to.value())
            .query((rs, _) -> GroupId.from(DatabaseUtils.getLongOrNull(rs, "pgroup_id")))
            .optional()
            .ifPresent(this::removeGroupData);

        final var fromToParams = new HashMap<String, Object>();
        fromToParams.put("from_grouptg_id", from.value());
        fromToParams.put("to_grouptg_id", to.value());
        final var updateNewGroup = """
            UPDATE grouptg AS target
            SET
                migrated_from_grouptg_id = :from_grouptg_id,
                language_id = source.language_id,
                pgroup_id = source.pgroup_id
            FROM (
                SELECT
                    language_id,
                    pgroup_id
                FROM grouptg
                WHERE id = :from_grouptg_id
            ) AS source
            WHERE target.id = :to_grouptg_id;
            """;
        jdbcClient.sql(updateNewGroup)
            .params(fromToParams)
            .update();

        final var deactivateOldGroup = """
            UPDATE grouptg SET pgroup_id = NULL WHERE id = :from_grouptg_id
            """;
        jdbcClient.sql(deactivateOldGroup)
            .param("from_grouptg_id", from.value())
            .update();
    }

    private void removeGroupData(GroupId groupId) {
        final var deleteGroup = """
            DELETE FROM everyday_spin WHERE pgroup_id = :id;
            DELETE FROM pgroup_to_personage WHERE pgroup_id = :id;
            UPDATE grouptg SET pgroup_id = NULL WHERE pgroup_id = :id;
            DELETE FROM pgroup WHERE id = :id;
            """;
        jdbcClient.sql(deleteGroup)
            .param("id", groupId.value())
            .update();
    }
}
