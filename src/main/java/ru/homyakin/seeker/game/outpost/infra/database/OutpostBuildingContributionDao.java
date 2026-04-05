package ru.homyakin.seeker.game.outpost.infra.database;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingContributionStorage;
import ru.homyakin.seeker.game.outpost.entity.OutpostContributor;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Repository
public class OutpostBuildingContributionDao implements OutpostBuildingContributionStorage {

    private final JdbcClient jdbcClient;

    public OutpostBuildingContributionDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void add(GroupId groupId, Building building, PersonageId personageId, int materialsDelta) {
        final var sql = """
            INSERT INTO pgroup_outpost_building_contribution
            (pgroup_id, building_id, personage_id, materials)
            VALUES (:pgroup_id, :building_id, :personage_id, :materials)
            ON CONFLICT (pgroup_id, building_id, personage_id)
            DO UPDATE SET materials = pgroup_outpost_building_contribution.materials + :materials
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("personage_id", personageId.value())
            .param("materials", materialsDelta)
            .update();
    }

    @Override
    public List<OutpostContributor> listTop(GroupId groupId, Building building) {
        final var sql = """
            SELECT personage_id, materials
            FROM pgroup_outpost_building_contribution
            WHERE pgroup_id = :pgroup_id AND building_id = :building_id
            ORDER BY materials DESC, personage_id ASC
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .query((rs, _) -> new OutpostContributor(
                PersonageId.from(rs.getLong("personage_id")),
                rs.getInt("materials")
            ))
            .list();
    }

    @Override
    public void clear(GroupId groupId, Building building) {
        final var sql = """
            DELETE FROM pgroup_outpost_building_contribution
            WHERE pgroup_id = :pgroup_id AND building_id = :building_id
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .update();
    }
}
