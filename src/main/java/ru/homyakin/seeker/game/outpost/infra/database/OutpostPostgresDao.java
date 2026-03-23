package ru.homyakin.seeker.game.outpost.infra.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;

@Repository
public class OutpostPostgresDao implements OutpostStorage {
    private final JdbcClient jdbcClient;

    public OutpostPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId) {
        final var sql = """
            SELECT pgroup_id, building_id, level
            FROM pgroup_outpost
            WHERE pgroup_id = :pgroup_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> mapEntry(rs))
            .list()
            .stream()
            .flatMap(Optional::stream)
            .toList();
    }

    @Override
    public boolean tryInsert(GroupId groupId, Building building, int level) {
        final var sql = """
            INSERT INTO pgroup_outpost (pgroup_id, building_id, level)
            VALUES (:pgroup_id, :building_id, :level)
            ON CONFLICT (pgroup_id, building_id) DO NOTHING
            """;
        final var updated = jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("level", level)
            .update();
        return updated > 0;
    }

    @Override
    public boolean incrementLevel(GroupId groupId, Building building) {
        final var sql = """
            UPDATE pgroup_outpost
            SET level = level + 1
            WHERE pgroup_id = :pgroup_id
              AND building_id = :building_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .update() > 0;
    }

    private Optional<OutpostSlot.BuildingSlot> mapEntry(ResultSet rs) throws SQLException {
        final var pgroupId = GroupId.from(rs.getLong("pgroup_id"));
        final var buildingId = rs.getInt("building_id");
        final var level = rs.getInt("level");
        return Building.fromId(buildingId)
            .map(building -> new OutpostSlot.BuildingSlot(pgroupId, building, level));
    }
}
