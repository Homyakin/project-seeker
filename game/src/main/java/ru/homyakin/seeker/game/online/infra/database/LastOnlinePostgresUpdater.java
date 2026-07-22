package ru.homyakin.seeker.game.online.infra.database;

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
        final var today = at.toLocalDate();
        jdbcClient.sql("""
            UPDATE personage SET
                online_streak = CASE
                    WHEN last_online::date = :today THEN online_streak
                    WHEN last_online::date = :yesterday THEN online_streak + 1
                    ELSE 1
                END,
                last_online = :at
            WHERE id = :id
            """)
            .param("at", at)
            .param("today", today)
            .param("yesterday", today.minusDays(1))
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
        final var today = at.toLocalDate();
        jdbcClient.sql("""
            UPDATE pgroup_to_personage SET
                online_streak = CASE
                    WHEN last_online::date = :today THEN online_streak
                    WHEN last_online::date = :yesterday THEN online_streak + 1
                    ELSE 1
                END,
                last_online = :at
            WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id AND is_active = true
            """)
            .param("at", at)
            .param("today", today)
            .param("yesterday", today.minusDays(1))
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }
}
