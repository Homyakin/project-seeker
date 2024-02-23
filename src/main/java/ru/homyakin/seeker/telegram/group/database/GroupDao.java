package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.ActiveTime;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class GroupDao {
    private static final String GET_GROUP_BY_ID = "SELECT * FROM grouptg WHERE groupId = :groupId";
    private static final String GET_GROUP_WITH_LESS_NEXT_EVENT_DATE = """
        SELECT * FROM grouptg WHERE next_event_date  < :next_event_date and is_active = true
        """;
    private static final String GET_GROUP_WITH_LESS_NEXT_RUMOR_DATE = """
        SELECT * FROM grouptg WHERE next_rumor_date < :next_rumor_date and is_active = true
        """;
    private static final String SAVE_GROUP = """
        insert into grouptg (groupId, is_active, language_id, init_date, next_event_date, next_rumor_date)
        values (:groupId, :is_active, :language_id, :init_date, :next_event_date, :next_rumor_date)
        """;
    private static final String UPDATE = """
        update grouptg
        set is_active = :is_active, language_id = :language_id, start_active_hour = :start_active_hour, 
        end_active_hour = :end_active_hour, active_time_zone = :active_time_zone
        where groupId = :groupId;
        """;

    private static final String UPDATE_NEXT_EVENT_DATE = """
        UPDATE grouptg SET next_event_date = :next_event_date WHERE groupId = :groupId
        """;

    private static final String UPDATE_NEXT_RUMOR_DATE = """
        UPDATE grouptg SET next_rumor_date = :next_rumor_date WHERE groupId = :groupId
        """;

    private final JdbcClient jdbcClient;

    public GroupDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(Group group) {
        final var now = TimeUtils.moscowTime();
        jdbcClient.sql(SAVE_GROUP)
            .param("id", group.id().value())
            .param("is_active", group.isActive())
            .param("language_id", group.language().id())
            .param("init_date", now)
            .param("next_event_date", now.plusMinutes(RandomUtils.getInInterval(20, 60)))
            .param("next_rumor_date", now.plusMinutes(RandomUtils.getInInterval(120, 240)))
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
            .param("start_active_hour", group.activeTime().startHour())
            .param("end_active_hour", group.activeTime().endHour())
            .param("active_time_zone", group.activeTime().timeZone())
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

    private Group mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Group(
            GroupId.from(rs.getLong("id")),
            rs.getBoolean("is_active"),
            Language.getOrDefault(rs.getInt("language_id")),
            new ActiveTime(
                rs.getInt("start_active_hour"),
                rs.getInt("end_active_hour"),
                rs.getInt("active_time_zone")
            )
        );
    }
}
