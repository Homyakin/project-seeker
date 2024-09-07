package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventIntervals;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.GroupSettings;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class GroupDao {
    private static final String GET_GROUP_BY_ID = "SELECT * FROM grouptg WHERE id = :id";
    private static final String GET_GROUP_WITH_LESS_NEXT_EVENT_DATE = """
        SELECT * FROM grouptg WHERE next_event_date  < :next_event_date and is_active = true
        """;
    private static final String GET_GROUP_WITH_LESS_NEXT_RUMOR_DATE = """
        SELECT * FROM grouptg WHERE next_rumor_date < :next_rumor_date and is_active = true
        """;
    private static final String SAVE_GROUP = """
        insert into grouptg (id, is_active, language_id, init_date, next_event_date, next_rumor_date,
            event_intervals_setting, time_zone_setting)
        values (:id, :is_active, :language_id, :init_date, :next_event_date, :next_rumor_date,
            :event_intervals_setting, :time_zone_setting)
        """;
    private static final String UPDATE = """
        update grouptg
        set is_active = :is_active, language_id = :language_id, time_zone_setting = :time_zone_setting,
        event_intervals_setting = :event_intervals_setting
        where id = :id;
        """;

    private static final String UPDATE_NEXT_EVENT_DATE = """
        UPDATE grouptg SET next_event_date = :next_event_date WHERE id = :id
        """;

    private static final String UPDATE_NEXT_RUMOR_DATE = """
        UPDATE grouptg SET next_rumor_date = :next_rumor_date WHERE id = :id
        """;

    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public GroupDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void save(Group group) {
        final var now = TimeUtils.moscowTime();
        jdbcClient.sql(SAVE_GROUP)
            .param("id", group.id().value())
            .param("is_active", group.isActive())
            .param("language_id", group.language().id())
            .param("init_date", now)
            .param("next_event_date", now)
            .param("next_rumor_date", now.plusMinutes(RandomUtils.getInInterval(120, 240)))
            .param("event_intervals_setting", jsonUtils.mapToPostgresJson(group.settings().eventIntervals()))
            .param("time_zone_setting", group.settings().timeZone().getId())
            .update();
    }

    public Optional<Group> getById(GroupId groupId) {
        return jdbcClient.sql(GET_GROUP_BY_ID)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return jdbcClient.sql(GET_GROUP_WITH_LESS_NEXT_EVENT_DATE)
            .param("next_event_date", maxNextEventDate)
            .query(this::mapRow)
            .list();
    }

    public List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate) {
        return jdbcClient.sql(GET_GROUP_WITH_LESS_NEXT_RUMOR_DATE)
            .param("next_rumor_date", maxNextRumorDate)
            .query(this::mapRow)
            .list();
    }

    public void update(Group group) {
        jdbcClient.sql(UPDATE)
            .param("id", group.id().value())
            .param("is_active", group.isActive())
            .param("language_id", group.language().id())
            .param("time_zone_setting", group.settings().timeZone().getId())
            .param("event_intervals_setting", jsonUtils.mapToPostgresJson(group.settings().eventIntervals()))
            .update();
    }

    public void updateNextEventDate(GroupId groupId, LocalDateTime nextEventDate) {
        jdbcClient.sql(UPDATE_NEXT_EVENT_DATE)
            .param("id", groupId.value())
            .param("next_event_date", nextEventDate)
            .update();
    }

    public void updateNextRumorDate(GroupId groupId, LocalDateTime nextRumorDate) {
        jdbcClient.sql(UPDATE_NEXT_RUMOR_DATE)
            .param("id", groupId.value())
            .param("next_rumor_date", nextRumorDate)
            .update();
    }

    public long getActiveGroupsCount() {
        // Считаем <=2 пользователя неактивной группой
        final var sql = """
            SELECT COUNT(*) FROM (
                SELECT g.id
                FROM grouptg g
                JOIN grouptg_to_usertg gu ON g.id = gu.grouptg_id
                WHERE g.is_active = true
                    AND gu.is_active = true
                GROUP BY g.id
                HAVING COUNT(gu.usertg_id) > 2
            ) active_groups;
            """;
        return jdbcClient.sql(sql)
            .query((rs, _) -> rs.getLong(1))
            .single();
    }

    private Group mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Group(
            GroupId.from(rs.getLong("id")),
            rs.getBoolean("is_active"),
            Language.getOrDefault(rs.getInt("language_id")),
            new GroupSettings(
                ZoneOffset.of(rs.getString("time_zone_setting")),
                jsonUtils.fromString(rs.getString("event_intervals_setting"), EventIntervals.class)
            )
        );
    }
}
