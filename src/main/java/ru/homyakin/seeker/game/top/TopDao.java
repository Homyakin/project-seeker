package ru.homyakin.seeker.game.top;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.GroupTopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopSpinPosition;

@Component
public class TopDao {
    private final JdbcClient jdbcClient;

    public TopDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public List<TopRaidPosition> getUnsortedTopRaid(LocalDate start, LocalDate end) {
        final var sql = GLOBAL_RAIDS_COUNT + RAIDS_PERSONAGE_INFO + " AND p.is_hidden = false";
        return jdbcClient.sql(sql)
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("start_date", start)
            .param("end_date", end)
            .param("raid_id", EventType.RAID.id())
            .query(this::mapRaidPosition)
            .list();
    }

    public List<TopRaidPosition> getUnsortedTopRaidGroup(LocalDate start, LocalDate end, GroupId groupId) {
        final var sql = GROUP_RAIDS_COUNT + RAIDS_PERSONAGE_INFO;
        return jdbcClient.sql(sql)
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("start_date", start)
            .param("end_date", end)
            .param("pgroup_id", groupId.value())
            .param("raid_id", EventType.RAID.id())
            .query(this::mapRaidPosition)
            .list();
    }

    public List<TopSpinPosition> getUnsortedTopSpinGroup(GroupId groupId) {
        return jdbcClient.sql(TOP_SPIN_GROUP)
            .param("pgroup_id", groupId.value())
            .query(this::mapSpinPosition)
            .list();
    }

    public List<GroupTopRaidPosition> getUnsortedGroupTopRaid(LocalDate start, LocalDate end) {
        final var sql = """
            WITH event_points AS (
                SELECT
                    letp.pgroup_id,
                    SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count
                FROM launched_event_to_pgroup letp
                INNER JOIN launched_event le on le.id = letp.launched_event_id
                INNER JOIN event e on le.event_id = e.id AND e.type_id = :raid_id
                LEFT JOIN pgroup pg on letp.pgroup_id = pg.id
                WHERE le.start_date::date >= :start_date AND le.start_date::date <= :end_date AND pg.is_hidden = false
                GROUP BY letp.pgroup_id
            )
            SELECT p.id, p.name, ep.success_count, ep.fail_count FROM event_points ep
            INNER JOIN pgroup p ON ep.pgroup_id = p.id
            WHERE success_count > 0 OR fail_count > 0
            """;
        return jdbcClient.sql(sql)
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("start_date", start)
            .param("end_date", end)
            .param("raid_id", EventType.RAID.id())
            .query(this::mapGroupTopRaidPosition)
            .list();
    }

    private TopRaidPosition mapRaidPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopRaidPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            rs.getInt("success_count"),
            rs.getInt("fail_count")
        );
    }

    private TopSpinPosition mapSpinPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopSpinPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            rs.getInt("count")
        );
    }

    private GroupTopRaidPosition mapGroupTopRaidPosition(ResultSet rs, int rowNum) throws SQLException {
        return new GroupTopRaidPosition(
            GroupId.from(rs.getLong("id")),
            rs.getString("name"),
            rs.getInt("success_count"),
            rs.getInt("fail_count")
        );
    }

    private static final String GLOBAL_RAIDS_COUNT = """
        WITH event_points AS (
        SELECT
            pte.personage_id,
            SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
            SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count
        FROM personage_to_event pte
        LEFT JOIN launched_event le on le.id = pte.launched_event_id
        INNER JOIN event e on le.event_id = e.id AND e.type_id = :raid_id
        LEFT JOIN launched_event_to_pgroup letp on le.id = letp.launched_event_id
        LEFT JOIN pgroup pg on letp.pgroup_id = pg.id
        WHERE le.start_date::date >= :start_date AND le.start_date::date <= :end_date AND pg.is_hidden = false
        GROUP BY pte.personage_id
        )""";

    private static final String GROUP_RAIDS_COUNT = """
        WITH event_points AS (
        SELECT
            pte.personage_id,
            SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
            SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count
        FROM personage_to_event pte
        LEFT JOIN launched_event le on le.id = pte.launched_event_id
        LEFT JOIN launched_event_to_pgroup letp on le.id = letp.launched_event_id
        INNER JOIN event e on le.event_id = e.id AND e.type_id = :raid_id
        WHERE le.start_date::date >= :start_date AND le.start_date::date <= :end_date
        AND letp.pgroup_id = :pgroup_id
        GROUP BY pte.personage_id
        )""";

    private static final String RAIDS_PERSONAGE_INFO = """
        SELECT p.id as personage_id, p.name personage_name, b.code badge_code, success_count, fail_count FROM personage p
        INNER JOIN event_points ep ON p.id = ep.personage_id
        LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN public.badge b on b.id = pab.badge_id
        WHERE pab.is_active = true""";

    private static final String TOP_SPIN_GROUP = """
        WITH personage_count
        AS (
            SELECT personage_id, COUNT(*) as count
            FROM everyday_spin
            WHERE pgroup_id = :pgroup_id
            GROUP BY personage_id
        )
        SELECT p.id as personage_id, p.name personage_name, b.code badge_code, pc.count FROM personage p
        INNER JOIN personage_count pc ON p.id = pc.personage_id
        LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN public.badge b on b.id = pab.badge_id
        WHERE pab.is_active = true""";
}
