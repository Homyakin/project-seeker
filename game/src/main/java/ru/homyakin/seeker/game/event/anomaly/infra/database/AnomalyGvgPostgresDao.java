package ru.homyakin.seeker.game.event.anomaly.infra.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Repository
public class AnomalyGvgPostgresDao implements AnomalyGvgStorage {
    private final JdbcClient jdbcClient;

    public AnomalyGvgPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public int getRating(GroupId groupId) {
        final var sql = "SELECT gvg_rating FROM pgroup WHERE id = :id";
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> rs.getInt("gvg_rating"))
            .single();
    }

    @Override
    public void updateRating(GroupId groupId, int newRating) {
        final var sql = "UPDATE pgroup SET gvg_rating = :gvg_rating WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("gvg_rating", newRating)
            .update();
    }

    @Override
    public void saveRecentOpponent(GroupId groupA, GroupId groupB, LocalDateTime foughtAt) {
        final var low = Math.min(groupA.value(), groupB.value());
        final var high = Math.max(groupA.value(), groupB.value());
        final var sql = """
            INSERT INTO anomaly_recent_opponent (group_id_low, group_id_high, fought_at)
            VALUES (:low, :high, :fought_at)
            ON CONFLICT (group_id_low, group_id_high)
            DO UPDATE SET fought_at = EXCLUDED.fought_at
            """;
        jdbcClient.sql(sql)
            .param("low", low)
            .param("high", high)
            .param("fought_at", foughtAt)
            .update();
    }

    @Override
    public Optional<LocalDateTime> findRecentOpponentFoughtAt(GroupId groupA, GroupId groupB) {
        final var low = Math.min(groupA.value(), groupB.value());
        final var high = Math.max(groupA.value(), groupB.value());
        final var sql = """
            SELECT fought_at FROM anomaly_recent_opponent
            WHERE group_id_low = :low AND group_id_high = :high
            """;
        return jdbcClient.sql(sql)
            .param("low", low)
            .param("high", high)
            .query((rs, _) -> rs.getTimestamp("fought_at").toLocalDateTime())
            .optional();
    }

    @Override
    public List<GroupId> findEligibleChallengeTargets(GroupId excludeGroupId) {
        final var sql = """
            SELECT p.id
            FROM pgroup p
            INNER JOIN pgroup_outpost po
                ON po.pgroup_id = p.id
                AND po.building_id = :storm_scanner_id
                AND po.level > 0
            WHERE p.tag IS NOT NULL
              AND p.is_active = true
              AND p.id <> :exclude_id
              AND NOT EXISTS (
                  SELECT 1
                  FROM anomaly a
                  INNER JOIN launched_event le ON le.id = a.launched_event_id
                  WHERE a.pgroup_id = p.id
                    AND le.status_id = :launched_status
              )
            """;
        return jdbcClient.sql(sql)
            .param("storm_scanner_id", Building.STORM_SCANNER.id())
            .param("exclude_id", excludeGroupId.value())
            .param("launched_status", EventStatus.LAUNCHED.id())
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .list();
    }
}
