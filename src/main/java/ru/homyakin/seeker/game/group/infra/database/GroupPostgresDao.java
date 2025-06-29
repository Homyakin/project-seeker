package ru.homyakin.seeker.game.group.infra.database;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.EventIntervals;
import ru.homyakin.seeker.game.group.entity.CreateGroupRequest;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GroupPostgresDao implements GroupStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public GroupPostgresDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.jsonUtils = jsonUtils;
    }

    @Override
    @Transactional
    public GroupId create(CreateGroupRequest request) {
        final var badgeSql = "SELECT id FROM badge WHERE code = :code";
        int badgeId = jdbcClient.sql(badgeSql)
            .param("code", BadgeView.STANDARD.code())
            .query((rs, _) -> rs.getInt("id"))
            .single();

        final var groupSql = """
            INSERT INTO pgroup (is_active, init_date, next_event_date, next_rumor_date,
                    event_intervals_setting, time_zone_setting, name, is_hidden, settings, active_badge_id)
            VALUES (:is_active, :init_date, :next_event_date, :next_rumor_date, :event_intervals_setting, :time_zone_setting,
                    :name, :is_hidden, :settings, :active_badge_id)
            RETURNING id
            """;
        GroupId groupId = jdbcClient.sql(groupSql)
            .param("is_active", request.isActive())
            .param("init_date", request.initDate())
            .param("next_event_date", request.nextEventDate())
            .param("next_rumor_date", request.nextRumorDate())
            .param("event_intervals_setting", jsonUtils.mapToPostgresJson(request.settings().eventIntervals()))
            .param("time_zone_setting", request.settings().timeZone().getId())
            .param("name", request.name())
            .param("is_hidden", request.settings().isHidden())
            .param("settings", jsonUtils.mapToPostgresJson(GroupSettingsPostgresJson.from(request.settings())))
            .param("active_badge_id", badgeId)
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .single();

        final var groupToBadgeSql = """
            INSERT INTO pgroup_to_badge (pgroup_id, badge_id)
            VALUES (:pgroup_id, :badge_id)
            """;
        jdbcClient.sql(groupToBadgeSql)
            .param("pgroup_id", groupId.value())
            .param("badge_id", badgeId)
            .update();

        return groupId;
    }

    @Override
    public long countActiveRegisteredGroups() {
        final var sql = """
            SELECT COUNT(*) as active_groups
            FROM pgroup p
            WHERE is_active = true AND is_hidden = false AND tag IS NOT NULL
            """;
        return jdbcClient.sql(sql)
            .query((rs, _) -> rs.getLong("active_groups"))
            .single();
    }

    @Override
    public Optional<Group> get(GroupId groupId) {
        final var sql = """
            SELECT p.*, b.code badge_code
            FROM pgroup p
            LEFT JOIN badge b ON p.active_badge_id = b.id
            WHERE p.id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        final var sql = """
            SELECT p.*, b.code badge_code
            FROM pgroup p
            LEFT JOIN badge b ON p.active_badge_id = b.id
            WHERE next_event_date < :next_event_date and is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("next_event_date", maxNextEventDate)
            .query(this::mapRow)
            .list();
    }

    @Override
    public List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate) {
        final var sql = """
            SELECT p.*, b.code badge_code
            FROM pgroup p
            LEFT JOIN badge b ON p.active_badge_id = b.id
            WHERE next_rumor_date < :next_rumor_date and is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("next_rumor_date", maxNextRumorDate)
            .query(this::mapRow)
            .list();
    }

    @Override
    public void update(Group group) {
        final var sql = """
            UPDATE pgroup
            SET is_active = :is_active, event_intervals_setting = :event_intervals_setting,
            time_zone_setting = :time_zone_setting WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", group.id().value())
            .param("is_active", group.isActive())
            .param("event_intervals_setting", jsonUtils.mapToPostgresJson(group.settings().eventIntervals()))
            .param("time_zone_setting", group.settings().timeZone().getId())
            .update();
    }

    @Override
    public void updateNextEventDate(GroupId groupId, LocalDateTime nextEventDate) {
        final var sql = """
            UPDATE pgroup SET next_event_date = :next_event_date WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("next_event_date", nextEventDate)
            .update();
    }

    @Override
    public void updateNextRumorDate(GroupId groupId, LocalDateTime nextRumorDate) {
        final var sql = """
            UPDATE pgroup SET next_rumor_date = :next_rumor_date WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("next_rumor_date", nextRumorDate)
            .update();
    }

    @Override
    public void changeGroupName(GroupId groupId, String name) {
        final var sql = """
            UPDATE pgroup SET name = :name WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("name", name)
            .update();
    }

    @Override
    public boolean toggleIsHidden(GroupId groupId) {
        final var sql = """
            UPDATE pgroup SET is_hidden = NOT is_hidden WHERE id = :id RETURNING is_hidden
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> rs.getBoolean("is_hidden"))
            .single();
    }

    @Override
    public void setTagAndTakeMoney(GroupId groupId, String tag, Money money) {
        final var sql = """
            UPDATE pgroup SET tag = :tag, money = money - :money WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("tag", tag)
            .param("money", money.value())
            .update();
    }

    @Override
    public void addMoney(GroupId groupId, Money money) {
        final var sql = """
            UPDATE pgroup SET money = money + :money WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("money", money.value())
            .update();
    }

    @Override
    public void takeMoney(GroupId groupId, Money money) {
        final var sql = """
            UPDATE pgroup SET money = money - :money WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("money", money.value())
            .update();
    }

    @Override
    public int memberCount(GroupId groupId) {
        final var sql = """
            SELECT COUNT(*) FROM personage WHERE member_pgroup_id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    @Override
    public void deleteTag(GroupId groupId) {
        final var sql = """
            UPDATE pgroup SET tag = NULL WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .update();
    }

    @Override
    public boolean isTagExists(String tag) {
        final var sql = """
            SELECT EXISTS(SELECT 1 FROM pgroup WHERE tag = :tag)
            """;
        return jdbcClient.sql(sql)
            .param("tag", tag)
            .query((rs, _) -> rs.getBoolean(1))
            .single();
    }

    @Override
    public Optional<GroupProfile> getProfile(GroupId groupId) {
        final var sql = """
        WITH member_count AS (
            SELECT COUNT(*) FROM personage WHERE member_pgroup_id = :id
        )
        SELECT
            pgroup.id,
            pgroup.name,
            pgroup.tag,
            pgroup.money,
            member_count.count as member_count,
            b.code badge_code
        FROM pgroup
        JOIN member_count ON true
        LEFT JOIN badge b ON pgroup.active_badge_id = b.id
        WHERE pgroup.id = :id
        """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapProfileRow)
            .optional();
    }

    @Override
    public Optional<Group> getByTag(String tag) {
        return getByTags(Collections.singletonList(tag)).stream().findFirst();
    }

    @Override
    public List<Group> getByTags(List<String> tags) {
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        final var sql = """
            SELECT p.*, b.code badge_code
            FROM pgroup p
            LEFT JOIN badge b ON p.active_badge_id = b.id
            WHERE tag in (:tags)
            """;
        return jdbcClient.sql(sql)
            .param("tags", tags)
            .query(this::mapRow)
            .list();
    }

    @Override
    public void addMoney(Map<GroupId, Money> moneyMap) {
        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var entry : moneyMap.entrySet()) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("id", entry.getKey().value())
                .addValue("money", entry.getValue().value());
            parameters.add(paramSource);
        }
        final var sql = """
            UPDATE pgroup
            SET money = money + :money
            WHERE id = :id
            """;
        jdbcTemplate.batchUpdate(sql, parameters.toArray(new SqlParameterSource[0]));
    }

    private Group mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var postgresSettings = jsonUtils.fromString(rs.getString("settings"), GroupSettingsPostgresJson.class);
        return new Group(
            GroupId.from(rs.getLong("id")),
            Optional.ofNullable(rs.getString("tag")),
            rs.getString("name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            rs.getBoolean("is_active"),
            new GroupSettings(
                ZoneOffset.of(rs.getString("time_zone_setting")),
                jsonUtils.fromString(rs.getString("event_intervals_setting"), EventIntervals.class),
                rs.getBoolean("is_hidden"),
                postgresSettings.enableToggleHide() == null
                    ? GroupSettings.DEFAULT_ENABLE_TOGGLE_HIDE
                    : postgresSettings.enableToggleHide()
            )
        );
    }

    private GroupProfile mapProfileRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupProfile(
            GroupId.from(rs.getLong("id")),
            rs.getString("name"),
            Optional.ofNullable(rs.getString("tag")),
            BadgeView.findByCode(rs.getString("badge_code")),
            Money.from(rs.getInt("money")),
            rs.getInt("member_count")
        );
    }

    private record GroupSettingsPostgresJson(
        Boolean enableToggleHide
    ) {
        public static GroupSettingsPostgresJson from(GroupSettings settings) {
            return new GroupSettingsPostgresJson(settings.enableToggleHide());
        }
    }
}
