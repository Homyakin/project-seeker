package ru.homyakin.seeker.game.online.infra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.online.LastOnlineUpdater;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import javax.sql.DataSource;
import java.time.LocalDateTime;

@Component
public class LastOnlinePostgresUpdater implements LastOnlineUpdater {
    private final JdbcClient jdbcClient;

    public LastOnlinePostgresUpdater(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void touchPersonage(PersonageId personageId, LocalDateTime at) {
        jdbcClient.sql("UPDATE personage SET last_online = :at WHERE id = :id")
            .param("at", at)
            .param("id", personageId.value())
            .update();
    }

    @Override
    public void touchGroup(GroupId groupId, LocalDateTime at) {
        jdbcClient.sql("UPDATE pgroup SET last_online = :at WHERE id = :id")
            .param("at", at)
            .param("id", groupId.value())
            .update();
    }

    @Override
    public void touchActiveMembership(GroupId groupId, PersonageId personageId, LocalDateTime at) {
        jdbcClient.sql("""
            UPDATE pgroup_to_personage SET last_online = :at
            WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id AND is_active = true
            """)
            .param("at", at)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }
}
