package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.stats.entity.GroupStats;
import ru.homyakin.seeker.game.stats.entity.GroupStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class GroupStatsPostgresDao implements GroupStatsStorage {
    private final JdbcClient jdbcClient;

    public GroupStatsPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<GroupStats> get(GroupId groupId, SeasonNumber seasonNumber) {
        final var sql = """
            SELECT * FROM season_pgroup_stats
            WHERE pgroup_id = :pgroup_id AND season_number = :season_number
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("season_number", seasonNumber.value())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public void add(GroupStats stats) {
        final var sql = """
            INSERT INTO season_pgroup_stats
            (season_number,
             pgroup_id,
             raids_success,
             raids_total,
             duels_complete,
             tavern_money_spent,
             world_raids_success,
             world_raids_total,
             raid_points
            )
            VALUES (
            :season_number,
            :pgroup_id,
            :raids_success,
            :raids_total,
            :duels_complete,
            :tavern_money_spent,
            :world_raids_success,
            :world_raids_total,
            :raid_points
            )
            ON CONFLICT (season_number, pgroup_id) DO UPDATE SET
            raids_success = season_pgroup_stats.raids_success + :raids_success,
            raids_total = season_pgroup_stats.raids_total + :raids_total,
            duels_complete = season_pgroup_stats.duels_complete + :duels_complete,
            tavern_money_spent = season_pgroup_stats.tavern_money_spent + :tavern_money_spent,
            world_raids_success = season_pgroup_stats.world_raids_success + :world_raids_success,
            world_raids_total = season_pgroup_stats.world_raids_total + :world_raids_total,
            raid_points = season_pgroup_stats.raid_points + :raid_points
            """;
        jdbcClient.sql(sql)
            .param("season_number", stats.seasonNumber().value())
            .param("pgroup_id", stats.groupId().value())
            .param("raids_success", stats.raidsSuccess())
            .param("raids_total", stats.raidsTotal())
            .param("duels_complete", stats.duelsComplete())
            .param("tavern_money_spent", stats.tavernMoneySpent())
            .param("world_raids_success", stats.worldRaidsSuccess())
            .param("world_raids_total", stats.worldRaidsTotal())
            .param("raid_points", stats.raidPoints())
            .update();
    }

    private GroupStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupStats(
            SeasonNumber.of(rs.getInt("season_number")),
            GroupId.from(rs.getLong("pgroup_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_complete"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("world_raids_success"),
            rs.getInt("world_raids_total"),
            rs.getInt("raid_points")
        );
    }
}
