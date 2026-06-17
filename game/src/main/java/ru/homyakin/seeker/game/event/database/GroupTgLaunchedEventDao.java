package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Component
public class GroupTgLaunchedEventDao {
    private static final String SAVE_GROUP_LAUNCHED_EVENT = """
        insert into grouptg_to_launched_event (launched_event_id, grouptg_id, message_id)
        values (:launched_event_id, :grouptg_id, :message_id);
        """;
    private static final String GET_GROUP_LAUNCHED_EVENT_BY_ID =
        "SELECT * FROM grouptg_to_launched_event WHERE launched_event_id = :launched_event_id";
    private final JdbcClient jdbcClient;

    public GroupTgLaunchedEventDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupLaunchedEvent groupLaunchedEvent) {
        jdbcClient.sql(SAVE_GROUP_LAUNCHED_EVENT)
            .param("launched_event_id", groupLaunchedEvent.launchedEventId())
            .param("grouptg_id", groupLaunchedEvent.groupId().value())
            .param("message_id", groupLaunchedEvent.messageId())
            .update();
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return jdbcClient.sql(GET_GROUP_LAUNCHED_EVENT_BY_ID)
            .param("launched_event_id", launchedEventId)
            .query(this::mapRow)
            .list();
    }

    public Optional<GroupLaunchedEvent> lastEndedRaidInGroup(GroupTgId groupId) {
        final var sql = """
            SELECT gtle.* FROM grouptg_to_launched_event gtle
            LEFT JOIN launched_event le ON gtle.launched_event_id = le.id
            LEFT JOIN event e ON le.event_id = e.id
            WHERE gtle.grouptg_id = :grouptg_id
            AND le.status_id != :active_status_id
            AND e.type_id = :raid_id
            ORDER BY le.id DESC
            LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .param("active_status_id", EventStatus.LAUNCHED.id())
            .param("raid_id", EventType.RAID.id())
            .query(this::mapRow)
            .optional();
    }

    private GroupLaunchedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupLaunchedEvent(
            rs.getLong("launched_event_id"),
            GroupTgId.from(rs.getLong("grouptg_id")),
            rs.getInt("message_id")
        );
    }
}
