package ru.homyakin.seeker.game.top;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidStatus;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.GroupTopRaidLevelPosition;
import ru.homyakin.seeker.game.top.models.GroupTopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopDonatePosition;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopWorkerOfDayPosition;
import ru.homyakin.seeker.game.top.models.TopWorldRaidResearchPosition;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.game.top.models.TopTavernSpentPosition;

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

    public List<TopWorkerOfDayPosition> getUnsortedTopWorkerGroup(GroupId groupId) {
        return jdbcClient.sql(TOP_SPIN_GROUP)
            .param("pgroup_id", groupId.value())
            .query(this::mapWorkerPosition)
            .list();
    }

    public List<TopDonatePosition> getUnsortedTopDonateGroup(GroupId groupId, int seasonNumber) {
        return jdbcClient.sql(TOP_DONATE_GROUP)
            .param("pgroup_id", groupId.value())
            .param("season_number", seasonNumber)
            .query(this::mapDonatePosition)
            .list();
    }

    public List<GroupTopRaidPosition> getUnsortedGroupTopRaid(LocalDate start, LocalDate end) {
        final var sql = """
            WITH event_points AS (
                SELECT
                    letp.pgroup_id,
                    SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count,
                    SUM(COALESCE((le.event_params->>'raidPoints')::int, 0)) AS raid_points
                FROM launched_event_to_pgroup letp
                INNER JOIN launched_event le on le.id = letp.launched_event_id
                INNER JOIN event e on le.event_id = e.id AND e.type_id = :raid_id
                LEFT JOIN pgroup pg on letp.pgroup_id = pg.id
                WHERE le.start_date::date >= :start_date AND le.start_date::date <= :end_date AND pg.is_hidden = false
                GROUP BY letp.pgroup_id
            )
            SELECT p.id, p.name, p.tag, ep.success_count, ep.fail_count, ep.raid_points, b.code badge_code FROM event_points ep
            INNER JOIN pgroup p ON ep.pgroup_id = p.id
            LEFT JOIN badge b ON p.active_badge_id = b.id
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

    public List<TopWorldRaidResearchPosition> getUnsortedTopWorldRaidResearch() {
        final var sql = """
            SELECT
                p.id as personage_id,
                p.name as personage_name,
                pg.tag as pgroup_member_tag,
                b.code as badge_code,
                wrr.contribution,
                wrr.reward
            FROM world_raid_research wrr
            LEFT JOIN personage p ON wrr.personage_id = p.id
            LEFT JOIN personage_available_badge pab on p.id = pab.personage_id and pab.is_active
            LEFT JOIN badge b ON pab.badge_id = b.id
            LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
            WHERE world_raid_id = (
                SELECT id FROM world_raid_launched
                WHERE status_id in (:research_status_id, :battle_status_id)
                LIMIT 1
            )
            """;
        return jdbcClient.sql(sql)
            .param("research_status_id", ActiveWorldRaidStatus.RESEARCH.id())
            .param("battle_status_id", ActiveWorldRaidStatus.BATTLE.id())
            .query(this::mapWorldRaidResearchPosition)
            .list();
    }

    public List<TopTavernSpentPosition> getUnsortedTopTavernSpentGroup(GroupId groupId, int seasonNumber) {
        return jdbcClient.sql(TOP_TAVERN_SPENT_GROUP)
            .param("pgroup_id", groupId.value())
            .param("season_number", seasonNumber)
            .query(this::mapTavernSpentPosition)
            .list();
    }

    public List<GroupTopRaidLevelPosition> getUnsortedGroupTopRaidLevel() {
        return jdbcClient.sql(TOP_GROUP_RAID_LEVEL)
            .query(this::mapGroupTopRaidLevelPosition)
            .list();
    }

    private TopWorldRaidResearchPosition mapWorldRaidResearchPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopWorldRaidResearchPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            rs.getInt("contribution"),
            DatabaseUtils.getIntOrEmpty(rs, "reward").map(Money::from)
        );
    }

    private TopRaidPosition mapRaidPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopRaidPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            rs.getInt("success_count"),
            rs.getInt("fail_count")
        );
    }

    private TopWorkerOfDayPosition mapWorkerPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopWorkerOfDayPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            rs.getInt("count")
        );
    }

    private TopDonatePosition mapDonatePosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopDonatePosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            rs.getLong("donate_money")
        );
    }

    private GroupTopRaidPosition mapGroupTopRaidPosition(ResultSet rs, int rowNum) throws SQLException {
        return new GroupTopRaidPosition(
            GroupId.from(rs.getLong("id")),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("tag")),
            rs.getString("name"),
            rs.getInt("success_count"),
            rs.getInt("fail_count"),
            rs.getInt("raid_points")
        );
    }

    private TopTavernSpentPosition mapTavernSpentPosition(ResultSet rs, int rowNum) throws SQLException {
        return new TopTavernSpentPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            rs.getLong("tavern_money_spent")
        );
    }

    private GroupTopRaidLevelPosition mapGroupTopRaidLevelPosition(ResultSet rs, int rowNum) throws SQLException {
        return new GroupTopRaidLevelPosition(
            GroupId.from(rs.getLong("id")),
            BadgeView.findByCode(rs.getString("badge_code")),
            Optional.ofNullable(rs.getString("tag")),
            rs.getString("name"),
            rs.getInt("raid_level")
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
        SELECT
            p.id as personage_id,
            p.name personage_name,
            b.code badge_code,
            success_count,
            fail_count,
            pg.tag as pgroup_member_tag
        FROM personage p
        INNER JOIN event_points ep ON p.id = ep.personage_id
        LEFT JOIN personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN badge b on b.id = pab.badge_id
        LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
        WHERE pab.is_active = true""";

    private static final String TOP_SPIN_GROUP = """
        WITH personage_count
        AS (
            SELECT personage_id, COUNT(*) as count
            FROM everyday_spin
            WHERE pgroup_id = :pgroup_id
            GROUP BY personage_id
        )
        SELECT 
            p.id as personage_id,
            p.name personage_name,
            b.code badge_code,
            pc.count,
            pg.tag as pgroup_member_tag    
        FROM personage p
        INNER JOIN personage_count pc ON p.id = pc.personage_id
        LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN public.badge b on b.id = pab.badge_id
        LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
        WHERE pab.is_active = true""";

    private static final String TOP_DONATE_GROUP = """
        WITH personage_donate
        AS (
            SELECT personage_id, donate_money
            FROM season_pgroup_personage_stats
            WHERE pgroup_id = :pgroup_id AND season_number = :season_number
        )
        SELECT
            p.id as personage_id,
            p.name personage_name,
            b.code badge_code,
            pd.donate_money,
            pg.tag as pgroup_member_tag
        FROM personage p
        INNER JOIN personage_donate pd ON p.id = pd.personage_id
        LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN public.badge b on b.id = pab.badge_id
        LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
        WHERE pab.is_active = true
        """;

    private static final String TOP_TAVERN_SPENT_GROUP = """
        WITH personage_tavern_spent AS (
            SELECT personage_id, tavern_money_spent
            FROM season_pgroup_personage_stats
            WHERE pgroup_id = :pgroup_id AND season_number = :season_number
        )
        SELECT
            p.id as personage_id,
            p.name personage_name,
            b.code badge_code,
            pts.tavern_money_spent,
            pg.tag as pgroup_member_tag
        FROM personage p
        INNER JOIN personage_tavern_spent pts ON p.id = pts.personage_id
        LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
        LEFT JOIN public.badge b on b.id = pab.badge_id
        LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
        WHERE pab.is_active = true
    """;

    private static final String TOP_GROUP_RAID_LEVEL = """
        SELECT p.id, p.name, p.tag, p.raid_level, b.code badge_code
        FROM pgroup p
        LEFT JOIN badge b ON p.active_badge_id = b.id
        WHERE p.is_hidden = false AND p.raid_level > 0
        ORDER BY p.raid_level DESC
    """;
}
