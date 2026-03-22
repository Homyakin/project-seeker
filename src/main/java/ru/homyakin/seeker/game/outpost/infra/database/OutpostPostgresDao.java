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

    private Optional<OutpostSlot.BuildingSlot> mapEntry(ResultSet rs) throws SQLException {
        final var pgroupId = GroupId.from(rs.getLong("pgroup_id"));
        final var buildingId = rs.getInt("building_id");
        final var level = rs.getInt("level");
        return Building.fromId(buildingId)
            .map(building -> new OutpostSlot.BuildingSlot(pgroupId, building, level));
    }
}
