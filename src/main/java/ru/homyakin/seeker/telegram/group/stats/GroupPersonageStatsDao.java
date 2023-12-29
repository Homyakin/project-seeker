package ru.homyakin.seeker.telegram.group.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Repository
public class GroupPersonageStatsDao {
    private final JdbcClient jdbcClient;

    public GroupPersonageStatsDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public void create(GroupId groupId, PersonageId personageId) {
        final var sql = "INSERT INTO grouptg_personage_stats (grouptg_id, personage_id) VALUES (:grouptg_id, :personage_id)";
        jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    public Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId) {
        final var sql = "SELECT * FROM grouptg_personage_stats WHERE grouptg_id = :grouptg_id and personage_id = :personage_id";
        return jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional();
    }

    public void update(GroupPersonageStats stats) {
        jdbcClient.sql(UPDATE)
            .param("grouptg_id", stats.groupId().value())
            .param("personage_id", stats.personageId().value())
            .param("raids_success", stats.raidsSuccess())
            .param("raids_total", stats.raidsTotal())
            .param("duels_wins", stats.duelsWins())
            .param("duels_total", stats.duelsTotal())
            .param("tavern_money_spent", stats.tavernMoneySpent())
            .param("spin_wins_count", stats.spinWinsCount())
            .update();
    }

    private GroupPersonageStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupPersonageStats(
            GroupId.from(rs.getLong("grouptg_id")),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count")
        );
    }

    private static String UPDATE = """
        UPDATE grouptg_personage_stats
        SET
            raids_success = :raids_success,
            raids_total = :raids_total,
            duels_wins = :duels_wins,
            duels_total = :duels_total,
            tavern_money_spent = :tavern_money_spent,
            spin_wins_count = :spin_wins_count
        WHERE grouptg_id = :grouptg_id AND personage_id = :personage_id;
        """;
}
