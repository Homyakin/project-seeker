package ru.homyakin.seeker.game.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
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

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public DuelDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("duel")
            .usingColumns(
                "initiating_personage_id",
                "accepting_personage_id",
                "expiring_date",
                "status_id"
            )
            .usingGeneratedKeyColumns("id");
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long create(
        PersonageId initiatingPersonageId,
        PersonageId acceptingPersonageId,
        Duration lifeTime
    ) {
        final var params = new HashMap<String, Object>();
        params.put("initiating_personage_id", initiatingPersonageId.value());
        params.put("accepting_personage_id", acceptingPersonageId.value());
        params.put("expiring_date", TimeUtils.moscowTime().plus(lifeTime));
        params.put("status_id", DuelStatus.WAITING.id());
        return jdbcInsert.executeAndReturnKey(params).longValue();
    }

    public Optional<Duel> getById(long id) {
        return jdbcTemplate.query(
            GET_BY_ID,
            Collections.singletonMap("id", id),
            this::mapRow
        ).stream().findFirst();
    }

    public Optional<Duel> getWaitingDuelByInitiatingPersonage(PersonageId initiatingPersonageId) {
        final var params = new HashMap<String, Object>();
        params.put("initiating_personage_id", initiatingPersonageId.value());
        params.put("status_id", DuelStatus.WAITING.id());
        return jdbcTemplate.query(
            GET_WAITING_BY_INITIATING_PERSONAGE,
            params,
            this::mapRow
        ).stream().findFirst();
    }

    public void addWinnerIdToDuel(long duelId, PersonageId personageId) {
        final var params = new HashMap<String, Object>();
        params.put("id", duelId);
        params.put("winner_personage_id", personageId.value());
        jdbcTemplate.update(
            ADD_WINNER_ID,
            params
        );
    }

    public void updateStatus(long duelId, DuelStatus status) {
        final var params = new HashMap<String, Object>();
        params.put("id", duelId);
        params.put("status_id", status.id());
        jdbcTemplate.update(
            UPDATE_STATUS,
            params
        );
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
