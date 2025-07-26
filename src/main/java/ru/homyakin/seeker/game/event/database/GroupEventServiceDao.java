package ru.homyakin.seeker.game.event.database;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;

@Component
public class GroupEventServiceDao {
    private final JdbcClient jdbcClient;

    public GroupEventServiceDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public List<GroupId> getGroupsByLaunchedEventId(long launchedEventId) {
        final var sql = """
            SELECT pgroup_id FROM launched_event_to_pgroup
            WHERE launched_event_id = :launched_event_id
            """;
        return jdbcClient.sql(sql)
            .param("launched_event_id", launchedEventId)
            .query((rs, _) -> GroupId.from(rs.getLong("pgroup_id")))
            .list();
    }
}
