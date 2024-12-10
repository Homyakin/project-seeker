package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.PersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PersonageStatsPostgresDao implements PersonageStatsStorage {
    private final JdbcClient jdbcClient;

    public PersonageStatsPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public PersonageStats get(PersonageId personageId) {
        final var selectPersonageStatsFromGroups = """
            SELECT
                SUM(raids_success) AS raids_success,
                SUM(raids_total) AS raids_total,
                SUM(duels_wins) AS duels_wins,
                SUM(duels_total) AS duels_total,
                SUM(tavern_money_spent) AS tavern_money_spent,
                SUM(spin_wins_count) AS spin_wins_count
            FROM pgroup_to_personage
            WHERE personage_id = :personage_id
            """;
        final var personageStatsFromGroups = jdbcClient.sql(selectPersonageStatsFromGroups)
            .param("personage_id", personageId.value())
            .query(this::mapGroupsRow)
            .single();

        final var selectQuestStats = """
            SELECT
                SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
                SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count
            FROM personage_to_event pte
            INNER JOIN launched_event le on le.id = pte.launched_event_id
            INNER JOIN event e on le.event_id = e.id AND e.type_id = :quest_id
            WHERE pte.personage_id = :personage_id
            """;
        final var questStats = jdbcClient.sql(selectQuestStats)
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("personage_id", personageId.value())
            .param("quest_id", EventType.PERSONAL_QUEST.id())
            .query((rs, _) -> new QuestStats(rs.getInt("success_count"), rs.getInt("fail_count")))
            .single();

        return new PersonageStats(
            personageStatsFromGroups.raidsSuccess,
            personageStatsFromGroups.raidsTotal,
            personageStatsFromGroups.duelsWins,
            personageStatsFromGroups.duelsTotal,
            personageStatsFromGroups.tavernMoneySpent,
            personageStatsFromGroups.spinWinsCount,
            questStats.questsSuccess,
            questStats.questsFailed + questStats.questsSuccess
        );
    }

    private PersonageStatsFromGroups mapGroupsRow(ResultSet rs, int rowNum) throws SQLException {
        return new PersonageStatsFromGroups(
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count")
        );
    }

    private record PersonageStatsFromGroups(
        int raidsSuccess,
        int raidsTotal,
        int duelsWins,
        int duelsTotal,
        long tavernMoneySpent,
        int spinWinsCount
    ) {
    }

    private record QuestStats(
        int questsSuccess,
        int questsFailed
    ) {
    }
}
