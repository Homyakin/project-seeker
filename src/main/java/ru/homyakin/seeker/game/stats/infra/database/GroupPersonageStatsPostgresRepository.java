package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStats;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class GroupPersonageStatsPostgresRepository implements GroupPersonageStatsStorage {
    private final JdbcClient jdbcClient;

    public GroupPersonageStatsPostgresRepository(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void update(GroupPersonageStats groupPersonageStats) {
        final var sql = """
            UPDATE pgroup_to_personage SET
                raids_success = :raids_success,
                raids_total = :raids_total,
                duels_wins = :duels_wins,
                duels_total = :duels_total,
                tavern_money_spent = :tavern_money_spent,
                spin_wins_count = :spin_wins_count
            WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupPersonageStats.groupId().value())
            .param("personage_id", groupPersonageStats.personageId().value())
            .param("raids_success", groupPersonageStats.raidsSuccess())
            .param("raids_total", groupPersonageStats.raidsTotal())
            .param("duels_wins", groupPersonageStats.duelsWins())
            .param("duels_total", groupPersonageStats.duelsTotal())
            .param("tavern_money_spent", groupPersonageStats.tavernMoneySpent())
            .param("spin_wins_count", groupPersonageStats.spinWinsCount())
            .update();
    }

    @Override
    public Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId) {
        final var sql = """
            SELECT * FROM pgroup_to_personage WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional();
    }

    private GroupPersonageStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupPersonageStats(
            GroupId.from(rs.getLong("pgroup_id")),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count")
        );
    }
}
