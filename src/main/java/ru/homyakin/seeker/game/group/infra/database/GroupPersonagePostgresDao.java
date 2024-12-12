package ru.homyakin.seeker.game.group.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.Set;

@Repository
public class GroupPersonagePostgresDao implements GroupPersonageStorage {
    private final JdbcClient jdbcClient;

    public GroupPersonagePostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<PersonageId> randomPersonage(GroupId groupId) {
        final var sql = """
            SELECT * FROM pgroup_to_personage WHERE pgroup_id = :pgroup_id AND is_active = true
            ORDER BY random() LIMIT 1;
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> PersonageId.from(rs.getLong("personage_id")))
            .optional();
    }

    @Override
    public int countPersonages(GroupId groupId) {
        final var sql = """
            SELECT COUNT(*) FROM pgroup_to_personage WHERE pgroup_id = :pgroup_id AND is_active = true;
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    @Override
    public void deactivatePersonageInGroup(GroupId groupId, PersonageId personageId) {
        final var sql = """
            UPDATE pgroup_to_personage SET is_active = false WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public void createOrActivate(GroupId groupId, PersonageId personageId) {
        final var sql = """
            INSERT INTO pgroup_to_personage (pgroup_id, personage_id, is_active) VALUES (:pgroup_id, :personage_id, true)
            ON CONFLICT (pgroup_id, personage_id) DO UPDATE SET is_active = true
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public void create(GroupId groupId, PersonageId personageId) {
        final var sql = """
            INSERT INTO pgroup_to_personage (pgroup_id, personage_id, is_active) VALUES (:pgroup_id, :personage_id, false)
            ON CONFLICT (pgroup_id, personage_id) DO NOTHING
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public Set<PersonageId> getActiveGroupPersonages(GroupId groupId) {
        final var sql = """
            SELECT personage_id FROM pgroup_to_personage WHERE pgroup_id = :pgroup_id AND is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> PersonageId.from(rs.getLong("personage_id")))
            .set();
    }
}
