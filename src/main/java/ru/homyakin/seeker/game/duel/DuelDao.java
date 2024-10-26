package ru.homyakin.seeker.game.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class DuelDao {
    private static final String GET_WAITING_BY_INITIATING_PERSONAGE = """
        SELECT * FROM duel WHERE status_id = :status_id and initiating_personage_id = :initiating_personage_id
        """;
    private static final String GET_BY_ID = """
        SELECT * FROM duel WHERE id = :id
        """;

    private static final String ADD_WINNER_ID = """
        UPDATE duel
        SET winner_personage_id = :winner_personage_id
        WHERE id = :id
        """;

    private static final String UPDATE_STATUS = """
        UPDATE duel
        SET status_id = :status_id
        WHERE id = :id
        """;

    private final JdbcClient jdbcClient;

    public DuelDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public long create(
        PersonageId initiatingPersonageId,
        PersonageId acceptingPersonageId,
        GroupId groupId,
        Duration lifeTime
    ) {
        final var sql = """
            INSERT INTO duel (initiating_personage_id, accepting_personage_id, pgroupd_id, expiring_date, status_id)
            VALUES (:initiating_personage_id, :accepting_personage_id, :pgroupd_id, :expiring_date, :status_id)
            RETURNING id
            """;
        return jdbcClient.sql(sql)
            .param("initiating_personage_id", initiatingPersonageId.value())
            .param("accepting_personage_id", acceptingPersonageId.value())
            .param("pgroupd_id", groupId.value())
            .param("expiring_date", TimeUtils.moscowTime().plus(lifeTime))
            .param("status_id", DuelStatus.WAITING.id())
            .query((rs, _) -> rs.getLong(1))
            .single();
    }

    public Optional<Duel> getById(long id) {
        return jdbcClient.sql(GET_BY_ID)
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    public Optional<Duel> getWaitingDuelByInitiatingPersonage(PersonageId initiatingPersonageId) {
        return jdbcClient.sql(GET_WAITING_BY_INITIATING_PERSONAGE)
            .param("initiating_personage_id", initiatingPersonageId.value())
            .param("status_id", DuelStatus.WAITING.id())
            .query(this::mapRow)
            .optional();
    }

    public void addWinnerIdToDuel(long duelId, PersonageId personageId) {
        jdbcClient.sql(ADD_WINNER_ID)
            .param("id", duelId)
            .param("winner_personage_id", personageId.value())
            .update();
    }

    public void updateStatus(long duelId, DuelStatus status) {
        final var params = new HashMap<String, Object>();
        params.put("id", duelId);
        params.put("status_id", status.id());
        jdbcClient.sql(UPDATE_STATUS)
            .param("id", duelId)
            .param("status_id", status.id())
            .update();
    }

    private Duel mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Duel(
            rs.getLong("id"),
            PersonageId.from(rs.getLong("initiating_personage_id")),
            PersonageId.from(rs.getLong("accepting_personage_id")),
            rs.getTimestamp("expiring_date").toLocalDateTime(),
            DuelStatus.getById(rs.getInt("status_id"))
        );
    }
}
