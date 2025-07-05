package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStats;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class GroupPersonageStatsPostgresDao implements GroupPersonageStatsStorage {
    private final JdbcClient jdbcClient;

    public GroupPersonageStatsPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void add(GroupPersonageStats groupPersonageStats) {
        final var sql = """
            INSERT INTO season_pgroup_personage_stats (
                season_number,
                pgroup_id,
                personage_id,
                raids_success,
                raids_total,
                duels_wins,
                duels_total,
                tavern_money_spent,
                spin_wins_count,
                donate_money
            ) VALUES (
                :season_number,
                :pgroup_id,
                :personage_id,
                :raids_success,
                :raids_total,
                :duels_wins,
                :duels_total,
                :tavern_money_spent,
                :spin_wins_count,
                :donate_money
            )
            ON CONFLICT (season_number, pgroup_id, personage_id) DO UPDATE SET
                raids_success = season_pgroup_personage_stats.raids_success + :raids_success,
                raids_total = season_pgroup_personage_stats.raids_total + :raids_total,
                duels_wins = season_pgroup_personage_stats.duels_wins + :duels_wins,
                duels_total = season_pgroup_personage_stats.duels_total + :duels_total,
                tavern_money_spent = season_pgroup_personage_stats.tavern_money_spent + :tavern_money_spent,
                spin_wins_count = season_pgroup_personage_stats.spin_wins_count + :spin_wins_count,
                donate_money = season_pgroup_personage_stats.donate_money + :donate_money
            """;
        jdbcClient.sql(sql)
            .param("season_number", groupPersonageStats.seasonNumber().value())
            .param("pgroup_id", groupPersonageStats.groupId().value())
            .param("personage_id", groupPersonageStats.personageId().value())
            .param("raids_success", groupPersonageStats.raidsSuccess())
            .param("raids_total", groupPersonageStats.raidsTotal())
            .param("duels_wins", groupPersonageStats.duelsWins())
            .param("duels_total", groupPersonageStats.duelsTotal())
            .param("tavern_money_spent", groupPersonageStats.tavernMoneySpent())
            .param("spin_wins_count", groupPersonageStats.workerOfDayCount())
            .param("donate_money", groupPersonageStats.donateMoney())
            .update();
    }

    @Override
    public Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId, SeasonNumber seasonNumber) {
        final var sql = """
            SELECT * FROM season_pgroup_personage_stats
            WHERE pgroup_id = :pgroup_id
            AND personage_id = :personage_id
            AND season_number = :season_number
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .param("season_number", seasonNumber.value())
            .query(this::mapRow)
            .optional();
    }

    private GroupPersonageStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupPersonageStats(
            SeasonNumber.of(rs.getInt("season_number")),
            GroupId.from(rs.getLong("pgroup_id")),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count"),
            rs.getLong("donate_money")
        );
    }
}
