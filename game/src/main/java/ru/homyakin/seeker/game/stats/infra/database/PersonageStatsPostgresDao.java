package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.stats.entity.AddPersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Component
public class PersonageStatsPostgresDao implements PersonageStatsStorage {
    private final JdbcClient jdbcClient;

    public PersonageStatsPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<PersonageStats> get(PersonageId personageId, SeasonNumber seasonNumber) {
        final var sql = """
            SELECT
                sps.season_number,
                sps.personage_id,
                quests_success,
                quests_total,
                world_raids_success,
                world_raids_total,
                COALESCE(spps.raids_success, 0) AS raids_success,
                COALESCE(spps.raids_total, 0) AS raids_total,
                COALESCE(spps.duels_wins, 0) AS duels_wins,
                COALESCE(spps.duels_total, 0) AS duels_total,
                COALESCE(spps.tavern_money_spent, 0) AS tavern_money_spent,
                COALESCE(spps.spin_wins_count, 0) AS spin_wins_count
            FROM season_personage_stats sps
            LEFT JOIN (
                SELECT
                    season_number,
                    personage_id,
                    SUM(raids_success) AS raids_success,
                    SUM(raids_total) AS raids_total,
                    SUM(duels_wins) AS duels_wins,
                    SUM(duels_total) AS duels_total,
                    SUM(tavern_money_spent) AS tavern_money_spent,
                    SUM(spin_wins_count) AS spin_wins_count
                FROM season_pgroup_personage_stats
                WHERE personage_id = :personage_id AND season_number = :season_number
                GROUP BY season_number, personage_id
            ) spps ON sps.season_number = spps.season_number AND sps.personage_id = spps.personage_id
            WHERE sps.personage_id = :personage_id AND sps.season_number = :season_number
            """;
        
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("season_number", seasonNumber.value())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public void add(AddPersonageStats stats) {
        final var mergeSql = """
            INSERT INTO season_personage_stats
            (season_number, personage_id, quests_success, quests_total, world_raids_success, world_raids_total)
            VALUES (:season_number, :personage_id, :quests_success, :quests_total, :world_raids_success, :world_raids_total)
            ON CONFLICT (season_number, personage_id)
            DO UPDATE SET
                quests_success = season_personage_stats.quests_success + :quests_success,
                quests_total = season_personage_stats.quests_total + :quests_total,
                world_raids_success = season_personage_stats.world_raids_success + :world_raids_success,
                world_raids_total = season_personage_stats.world_raids_total + :world_raids_total
            """;
        
        jdbcClient.sql(mergeSql)
            .param("season_number", stats.seasonNumber().value())
            .param("personage_id", stats.personageId().value())
            .param("quests_success", stats.questsSuccess())
            .param("quests_total", stats.questsTotal())
            .param("world_raids_success", stats.worldRaidsSuccess())
            .param("world_raids_total", stats.worldRaidsTotal())
            .update();
    }

    private PersonageStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PersonageStats(
            SeasonNumber.of(rs.getInt("season_number")),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count"),
            rs.getInt("quests_success"),
            rs.getInt("quests_total"),
            rs.getInt("world_raids_success"),
            rs.getInt("world_raids_total")
        );
    }
}
