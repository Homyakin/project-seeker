package ru.homyakin.seeker.game.event.launched;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;

import javax.sql.DataSource;

@Repository
public class LaunchedEventGroupDao {
    private final JdbcClient jdbcClient;

    public LaunchedEventGroupDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(long launchedEventId, GroupId groupId) {
        final var sql = """
            INSERT INTO public.launched_event_to_pgroup (launched_event_id, pgroup_id)
            VALUES (:launched_event_id, :pgroup_id);
            """;

        jdbcClient.sql(sql)
            .param("launched_event_id", launchedEventId)
            .param("pgroup_id", groupId.value())
            .update();
    }
}
