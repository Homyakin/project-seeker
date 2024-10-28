package ru.homyakin.seeker.game.group.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.EventIntervals;
import ru.homyakin.seeker.game.group.entity.CreateGroupRequest;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupPostgresDao implements GroupStorage {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public GroupPostgresDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public GroupId create(CreateGroupRequest request) {
        final var sql = """
            INSERT INTO pgroup (is_active, init_date, next_event_date, next_rumor_date,
                    event_intervals_setting, time_zone_setting, name, is_hidden, settings)
            VALUES (:is_active, :init_date, :next_event_date, :next_rumor_date, :event_intervals_setting, :time_zone_setting,
                    :name, :is_hidden, :settings)
            RETURNING id
            """;
        return jdbcClient.sql(sql)
            .param("is_active", request.isActive())
            .param("init_date", request.initDate())
            .param("next_event_date", request.nextEventDate())
            .param("next_rumor_date", request.nextRumorDate())
            .param("event_intervals_setting", jsonUtils.mapToPostgresJson(request.settings().eventIntervals()))
            .param("time_zone_setting", request.settings().timeZone().getId())
            .param("name", request.name())
            .param("is_hidden", request.settings().isHidden())
            .param("settings", jsonUtils.mapToPostgresJson(GroupSettingsPostgresJson.from(request.settings())))
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .single();
    }

    @Override
    public long countActiveGroups(int requiredActivePersonages) {
        final var sql = """
            SELECT COUNT(*) as active_groups
            FROM pgroup p
            WHERE is_active = true AND is_hidden = false AND
            (
                SELECT COUNT(*) FROM pgroup_to_personage WHERE pgroup_id = p.id AND is_active = true
            ) >= :required_active_personages
            """;
        return jdbcClient.sql(sql)
            .param("required_active_personages", requiredActivePersonages)
            .query((rs, _) -> rs.getLong("active_groups"))
            .single();
    }

    @Override
    public Optional<Group> get(GroupId groupId) {
        final var sql = "SELECT * FROM pgroup WHERE id = :id";
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        final var sql = """
            SELECT * FROM pgroup WHERE next_event_date < :next_event_date and is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("next_event_date", maxNextEventDate)
            .query(this::mapRow)
            .list();
    }

    @Override
    public List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate) {
        final var sql = """
            SELECT * FROM pgroup WHERE next_rumor_date < :next_rumor_date and is_active = true
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

    private Group mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var postgresSettings = jsonUtils.fromString(rs.getString("settings"), GroupSettingsPostgresJson.class);
        return new Group(
            GroupId.from(rs.getLong("id")),
            rs.getString("name"),
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

    private record GroupSettingsPostgresJson(
        Boolean enableToggleHide
    ) {
        public static GroupSettingsPostgresJson from(GroupSettings settings) {
            return new GroupSettingsPostgresJson(settings.enableToggleHide());
        }
    }
}
