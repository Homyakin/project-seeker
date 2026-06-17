package ru.homyakin.seeker.game.contraband.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandStatus;
import ru.homyakin.seeker.game.contraband.entity.ContrabandStorage;
import ru.homyakin.seeker.game.contraband.entity.ContrabandTier;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ContrabandPostgresDao implements ContrabandStorage {
    private final JdbcClient jdbcClient;

    public ContrabandPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public long create(Contraband contraband) {
        return jdbcClient.sql("""
                INSERT INTO contraband_chest (tier, finder_personage_id, status, created_at, expires_at)
                VALUES (:tier, :finder_personage_id, :status, :created_at, :expires_at)
                RETURNING id
                """)
            .param("tier", contraband.tier().id())
            .param("finder_personage_id", contraband.finderPersonageId().value())
            .param("status", contraband.status().id())
            .param("created_at", Timestamp.valueOf(contraband.createdAt()))
            .param("expires_at", Timestamp.valueOf(contraband.expiresAt()))
            .query((rs, _) -> rs.getLong("id"))
            .single();
    }

    @Override
    public Optional<Contraband> getById(long id) {
        return jdbcClient.sql("SELECT * FROM contraband_chest WHERE id = :id")
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    @Override
    public void update(Contraband contraband) {
        jdbcClient.sql("""
                UPDATE contraband_chest
                SET status = :status,
                    receiver_personage_id = :receiver_personage_id,
                    processed_at = :processed_at,
                    expires_at = :expires_at
                WHERE id = :id
                """)
            .param("status", contraband.status().id())
            .param("receiver_personage_id", contraband.receiverPersonageId().map(PersonageId::value).orElse(null))
            .param("processed_at", contraband.processedAt().map(Timestamp::valueOf).orElse(null))
            .param("expires_at", Timestamp.valueOf(contraband.expiresAt()))
            .param("id", contraband.id())
            .update();
    }

    @Override
    public Optional<Contraband> findActiveForPersonage(PersonageId personageId) {
        return jdbcClient.sql("""
                SELECT * FROM contraband_chest
                WHERE (finder_personage_id = :personage_id AND status = :found_status)
                   OR (receiver_personage_id = :personage_id AND status = :waiting_status)
                LIMIT 1
                """)
            .param("personage_id", personageId.value())
            .param("found_status", ContrabandStatus.FOUND.id())
            .param("waiting_status", ContrabandStatus.WAITING_RECEIVER.id())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public List<Contraband> findPendingForBlackMarket() {
        return jdbcClient.sql("""
                SELECT * FROM contraband_chest
                WHERE status = :status AND receiver_personage_id IS NULL
                """)
            .param("status", ContrabandStatus.SOLD_TO_MARKET.id())
            .query(this::mapRow)
            .list();
    }

    @Override
    public List<Contraband> findExpired(LocalDateTime now) {
        return jdbcClient.sql("""
                SELECT * FROM contraband_chest
                WHERE expires_at < :now AND status NOT IN (:final_statuses)
                """)
            .param("now", Timestamp.valueOf(now))
            .param("final_statuses", List.of(
                ContrabandStatus.OPENED_SUCCESS.id(),
                ContrabandStatus.OPENED_FAILURE.id(),
                ContrabandStatus.EXPIRED.id()
            ))
            .query(this::mapRow)
            .list();
    }

    @Override
    public int countFinderFailedOpensSinceLastSuccess(PersonageId personageId) {
        return jdbcClient.sql("""
                SELECT COUNT(*) FROM contraband_chest
                WHERE finder_personage_id = :personage_id
                  AND receiver_personage_id IS NULL
                  AND status = :failure_status
                  AND processed_at > COALESCE(
                      (SELECT MAX(processed_at) FROM contraband_chest
                       WHERE finder_personage_id = :personage_id
                         AND receiver_personage_id IS NULL
                         AND status = :success_status),
                      '1970-01-01'::timestamp
                  )
                """)
            .param("personage_id", personageId.value())
            .param("failure_status", ContrabandStatus.OPENED_FAILURE.id())
            .param("success_status", ContrabandStatus.OPENED_SUCCESS.id())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    @Override
    public int countReceiverFailedOpensSinceLastSuccess(PersonageId personageId) {
        return jdbcClient.sql("""
                SELECT COUNT(*) FROM contraband_chest
                WHERE receiver_personage_id = :personage_id
                  AND status = :failure_status
                  AND processed_at > COALESCE(
                      (SELECT MAX(processed_at) FROM contraband_chest
                       WHERE receiver_personage_id = :personage_id
                         AND status = :success_status),
                      '1970-01-01'::timestamp
                  )
                """)
            .param("personage_id", personageId.value())
            .param("failure_status", ContrabandStatus.OPENED_FAILURE.id())
            .param("success_status", ContrabandStatus.OPENED_SUCCESS.id())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    private Contraband mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var receiverId = rs.getObject("receiver_personage_id");
        final var processedAt = rs.getTimestamp("processed_at");
        return new Contraband(
            rs.getLong("id"),
            ContrabandTier.findById(rs.getInt("tier")),
            new PersonageId(rs.getLong("finder_personage_id")),
            receiverId == null ? Optional.empty() : Optional.of(new PersonageId(((Number) receiverId).longValue())),
            ContrabandStatus.findById(rs.getInt("status")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("expires_at").toLocalDateTime(),
            processedAt == null ? Optional.empty() : Optional.of(processedAt.toLocalDateTime())
        );
    }
}
