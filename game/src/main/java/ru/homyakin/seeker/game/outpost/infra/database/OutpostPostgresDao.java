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
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingProgress;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class OutpostPostgresDao implements OutpostStorage {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public OutpostPostgresDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId) {
        final var sql = """
            SELECT pgroup_id, building_id, level, progress
            FROM pgroup_outpost
            WHERE pgroup_id = :pgroup_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> mapEntry(rs))
            .list();
    }

    @Override
    public Optional<OutpostSlot.BuildingSlot> findBuildingSlot(GroupId groupId, Building building) {
        final var sql = """
            SELECT pgroup_id, building_id, level, progress
            FROM pgroup_outpost
            WHERE pgroup_id = :pgroup_id
              AND building_id = :building_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .query((rs, _) -> mapEntry(rs))
            .optional();
    }

    @Override
    public boolean tryInsertWithProgress(
        GroupId groupId,
        Building building,
        int level,
        OutpostBuildingProgress progress
    ) {
        final var sql = """
            INSERT INTO pgroup_outpost (pgroup_id, building_id, level, progress)
            VALUES (:pgroup_id, :building_id, :level, :progress)
            ON CONFLICT (pgroup_id, building_id) DO NOTHING
            """;
        final var updated = jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("level", level)
            .param("progress", jsonUtils.mapToPostgresJson(progress))
            .update();
        return updated > 0;
    }

    @Override
    public boolean trySetProgress(GroupId groupId, Building building, OutpostBuildingProgress progress) {
        final var sql = """
            UPDATE pgroup_outpost
            SET progress = :progress
            WHERE pgroup_id = :pgroup_id
              AND building_id = :building_id
              AND progress IS NULL
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("progress", jsonUtils.mapToPostgresJson(progress))
            .update() > 0;
    }

    @Override
    public boolean updateBuildingProgress(GroupId groupId, Building building, OutpostBuildingProgress progress) {
        final var sql = """
            UPDATE pgroup_outpost
            SET progress = :progress
            WHERE pgroup_id = :pgroup_id
              AND building_id = :building_id
              AND progress IS NOT NULL
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("progress", jsonUtils.mapToPostgresJson(progress))
            .update() > 0;
    }

    @Override
    public boolean completeInProgressBuilding(GroupId groupId, Building building) {
        final var sql = """
            UPDATE pgroup_outpost
            SET level = level + 1,
                progress = NULL
            WHERE pgroup_id = :pgroup_id
              AND building_id = :building_id
              AND progress IS NOT NULL
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .update() > 0;
    }

    private OutpostSlot.BuildingSlot mapEntry(ResultSet rs) throws SQLException {
        final var pgroupId = GroupId.from(rs.getLong("pgroup_id"));
        final var buildingId = rs.getInt("building_id");
        final var level = rs.getInt("level");
        final var progressRaw = rs.getString("progress");
        final var progress = progressRaw == null
            ? Optional.<OutpostBuildingProgress>empty()
            : Optional.of(jsonUtils.fromString(progressRaw, OutpostBuildingProgress.class));
        return new OutpostSlot.BuildingSlot(pgroupId, Building.fromId(buildingId), level, progress, 0);
    }
}
