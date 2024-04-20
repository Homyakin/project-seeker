package ru.homyakin.seeker.telegram.group.database;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Component
public class GroupMigrateDao {
    private final JdbcClient jdbcClient;

    public GroupMigrateDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Transactional
    public void migrate(GroupId from, GroupId to) {
        final var fromToParams = new HashMap<String, Object>();
        fromToParams.put("from_grouptg_id", from.value());
        fromToParams.put("to_grouptg_id", to.value());
        final var deleteNewGroupStats = """
            DELETE FROM grouptg_stats WHERE grouptg_id = :to_grouptg_id
            """;
        jdbcClient.sql(deleteNewGroupStats)
            .param("to_grouptg_id", to.value())
            .update();
        final var migrateGroupStats = """
            UPDATE grouptg_stats SET grouptg_id = :to_grouptg_id WHERE grouptg_id = :from_grouptg_id
            """;
        jdbcClient.sql(migrateGroupStats)
            .params(fromToParams)
            .update();
        final var deleteNewGroupUsers = """
            DELETE FROM grouptg_to_usertg WHERE grouptg_id = :to_grouptg_id
            """;
        jdbcClient.sql(deleteNewGroupUsers)
            .param("to_grouptg_id", to.value())
            .update();
        final var migrateGroupUsers = """
            UPDATE grouptg_to_usertg SET grouptg_id = :to_grouptg_id WHERE grouptg_id = :from_grouptg_id
            """;
        jdbcClient.sql(migrateGroupUsers)
            .params(fromToParams)
            .update();
        final var deleteNewGroupPersonageStats = """
            DELETE FROM grouptg_personage_stats WHERE grouptg_id = :to_grouptg_id
            """;
        jdbcClient.sql(deleteNewGroupPersonageStats)
            .param("to_grouptg_id", to.value())
            .update();
        final var migrateGroupPersonageStats = """
            UPDATE grouptg_personage_stats SET grouptg_id = :to_grouptg_id WHERE grouptg_id = :from_grouptg_id
            """;
        jdbcClient.sql(migrateGroupPersonageStats)
            .params(fromToParams)
            .update();
        final var selectOldLanguage = """
            SELECT language_id FROM grouptg WHERE id = :from_grouptg_id
            """;
        final var language = jdbcClient.sql(selectOldLanguage)
            .param("from_grouptg_id", from.value())
            .query((rs, _) -> rs.getInt("language_id"))
            .single();
        final var updateNewGroup = """
            UPDATE grouptg SET migrated_from_grouptg_id = :from_grouptg_id,
            language_id = :language_id
            WHERE id = :to_grouptg_id
            """;
        jdbcClient.sql(updateNewGroup)
            .params(fromToParams)
            .param("language_id", language)
            .update();
    }
}
