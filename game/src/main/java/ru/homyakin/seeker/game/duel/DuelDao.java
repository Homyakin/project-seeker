package ru.homyakin.seeker.game.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class DuelDao {
    private static final String GET_WAITING_BY_INITIATING_PERSONAGE = """
        SELECT d.launched_event_id, d.initiating_personage_id, d.accepting_personage_id, d.winner_personage_id,
               le.end_date, le.status_id
        FROM duel d
        INNER JOIN launched_event le ON le.id = d.launched_event_id
        WHERE le.status_id = :status_id AND d.initiating_personage_id = :initiating_personage_id
        """;
    private static final String GET_BY_ID = """
        SELECT d.launched_event_id, d.initiating_personage_id, d.accepting_personage_id, d.winner_personage_id,
               le.end_date, le.status_id
        FROM duel d
        INNER JOIN launched_event le ON le.id = d.launched_event_id
        WHERE d.launched_event_id = :id
        """;

    private static final String ADD_WINNER_ID = """
        UPDATE duel
        SET winner_personage_id = :winner_personage_id
        WHERE launched_event_id = :id
        """;

    private final JdbcClient jdbcClient;

    public DuelDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void create(
        long launchedEventId,
        PersonageId initiatingPersonageId,
        PersonageId acceptingPersonageId
    ) {
        final var sql = """
            INSERT INTO duel (launched_event_id, initiating_personage_id, accepting_personage_id)
            VALUES (:launched_event_id, :initiating_personage_id, :accepting_personage_id)
            """;
        jdbcClient.sql(sql)
            .param("launched_event_id", launchedEventId)
            .param("initiating_personage_id", initiatingPersonageId.value())
            .param("accepting_personage_id", acceptingPersonageId.value())
            .update();
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
            .param("status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .optional();
    }

    public void addWinnerIdToDuel(long duelId, PersonageId personageId) {
        jdbcClient.sql(ADD_WINNER_ID)
            .param("id", duelId)
            .param("winner_personage_id", personageId.value())
            .update();
    }

    private Duel mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var winnerId = rs.getLong("winner_personage_id");
        final Optional<PersonageId> winner = rs.wasNull()
            ? Optional.empty()
            : Optional.of(PersonageId.from(winnerId));
        return new Duel(
            rs.getLong("launched_event_id"),
            PersonageId.from(rs.getLong("initiating_personage_id")),
            PersonageId.from(rs.getLong("accepting_personage_id")),
            winner,
            rs.getTimestamp("end_date").toLocalDateTime(),
            EventStatus.findById(rs.getInt("status_id"))
        );
    }
}
