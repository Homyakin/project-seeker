package ru.homyakin.seeker.game.event.anomaly.infra.database;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Repository
public class AnomalyGvgPostgresDao implements AnomalyGvgStorage {
    private final JdbcClient jdbcClient;
    private final AnomalyConfig config;

    public AnomalyGvgPostgresDao(DataSource dataSource, AnomalyConfig config) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.config = config;
    }

    @Override
    public int getRating(GroupId groupId) {
        ensureRatingRow(groupId);
        final var sql = "SELECT rating FROM anomaly_rating WHERE pgroup_id = :id";
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> rs.getInt("rating"))
            .single();
    }

    @Override
    public void updateRating(GroupId groupId, int newRating) {
        final var sql = """
            INSERT INTO anomaly_rating (pgroup_id, rating)
            VALUES (:id, :rating)
            ON CONFLICT (pgroup_id)
            DO UPDATE SET rating = EXCLUDED.rating
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("rating", newRating)
            .update();
    }

    @Override
    public List<LocalDateTime> findOpponentFoughtAtList(GroupId groupA, GroupId groupB) {
        final var sql = """
            SELECT le.end_date AS fought_at
            FROM anomaly a
            INNER JOIN launched_event le ON le.id = a.launched_event_id
            WHERE a.opponent_pgroup_id IS NOT NULL
              AND a.winner_pgroup_id IS NOT NULL
              AND le.status_id = :success_status
              AND (
                    (a.pgroup_id = :group_a AND a.opponent_pgroup_id = :group_b)
                 OR (a.pgroup_id = :group_b AND a.opponent_pgroup_id = :group_a)
              )
            ORDER BY le.end_date DESC
            """;
        return jdbcClient.sql(sql)
            .param("group_a", groupA.value())
            .param("group_b", groupB.value())
            .param("success_status", EventStatus.SUCCESS.id())
            .query((rs, _) -> rs.getTimestamp("fought_at").toLocalDateTime())
            .list();
    }

    @Override
    public List<GroupId> findEligibleChallengeTargets(GroupId excludeGroupId, LocalDate day) {
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
              AND (
                  SELECT COUNT(*)
                  FROM personage pe
                  WHERE pe.member_pgroup_id = p.id
              ) >= :min_members
              AND NOT EXISTS (
                  SELECT 1
                  FROM anomaly a
                  INNER JOIN launched_event le ON le.id = a.launched_event_id
                  WHERE (a.pgroup_id = p.id OR a.opponent_pgroup_id = p.id)
                    AND le.status_id = :launched_status
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM anomaly_challenge_day d
                  WHERE d.pgroup_id = p.id
                    AND d.day_date = :day_date
              )
            """;
        return jdbcClient.sql(sql)
            .param("storm_scanner_id", Building.STORM_SCANNER.id())
            .param("exclude_id", excludeGroupId.value())
            .param("min_members", config.partySize())
            .param("launched_status", EventStatus.LAUNCHED.id())
            .param("day_date", day)
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .list();
    }

    private void ensureRatingRow(GroupId groupId) {
        final var sql = """
            INSERT INTO anomaly_rating (pgroup_id, rating)
            VALUES (:id, :rating)
            ON CONFLICT (pgroup_id) DO NOTHING
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("rating", config.initialRating())
            .update();
    }
}
