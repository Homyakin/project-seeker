package ru.homyakin.seeker.game.contraband.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandStatus;
import ru.homyakin.seeker.game.contraband.entity.FindReceiver;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ReceiverFinderPostgres implements FindReceiver {
    private final JdbcClient jdbcClient;

    public ReceiverFinderPostgres(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<PersonageId> findReceiver(Contraband contraband, Duration activityDuration) {
        final var since = Timestamp.valueOf(LocalDateTime.now().minus(activityDuration));

        final var sql = """
            SELECT p.id AS personage_id
            FROM personage p
            LEFT JOIN contraband_chest cc ON cc.receiver_personage_id = p.id
            WHERE p.last_energy_change > :since
              AND p.id != :finder_id
              AND NOT EXISTS (
                  SELECT 1 FROM contraband_chest cc2
                  WHERE cc2.receiver_personage_id = p.id
                  AND cc2.status = :waiting_status
              )
            GROUP BY p.id
            ORDER BY MAX(cc.created_at) ASC NULLS FIRST
            LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("since", since)
            .param("finder_id", contraband.finderPersonageId().value())
            .param("waiting_status", ContrabandStatus.WAITING_RECEIVER.id())
            .query((rs, _) -> new PersonageId(rs.getLong("personage_id")))
            .optional();
    }
}
