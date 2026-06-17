package ru.homyakin.seeker.game.valentine.infra.database;

import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.valentine.entity.ValentineCounts;
import ru.homyakin.seeker.game.valentine.entity.ValentineStorage;

@Repository
public class ValentinePostgresDao implements ValentineStorage {
    private final JdbcClient jdbcClient;

    public ValentinePostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void save(
        PersonageId throwingPersonageId,
        PersonageId targetPersonageId,
        boolean isRandom,
        GroupId throwingGroupId,
        GroupId targetGroupId,
        LocalDateTime date
    ) {
        final var sql = """
            INSERT INTO valentine_stats
            (throwing_personage_id, target_personage_id, is_random, throwing_pgroup_id, target_pgroup_id, date)
            VALUES (:throwing_personage_id, :target_personage_id, :is_random, :throwing_pgroup_id, :target_pgroup_id, :date)
            """;
        jdbcClient.sql(sql)
            .param("throwing_personage_id", throwingPersonageId.value())
            .param("target_personage_id", targetPersonageId.value())
            .param("is_random", isRandom)
            .param("throwing_pgroup_id", throwingGroupId.value())
            .param("target_pgroup_id", targetGroupId.value())
            .param("date", date)
            .update();
    }

    @Override
    public ValentineCounts getCounts(PersonageId personageId) {
        final var sql = """
            SELECT
                COUNT(*) FILTER (WHERE throwing_personage_id = :personage_id) AS sent,
                COUNT(*) FILTER (WHERE target_personage_id = :personage_id) AS received
            FROM valentine_stats
            WHERE throwing_personage_id = :personage_id OR target_personage_id = :personage_id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, rowNum) -> new ValentineCounts(rs.getInt("sent"), rs.getInt("received")))
            .optional()
            .orElse(ValentineCounts.ZERO);
    }
}
