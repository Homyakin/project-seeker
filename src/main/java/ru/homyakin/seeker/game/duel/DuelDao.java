package ru.homyakin.seeker.game.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class DuelDao {
    private static final String GET_WAITING_BY_INITIATING_PERSONAGE = """
        SELECT * FROM duel WHERE status = :status and initiating_personage_id = :initiating_personage_id
        """;
    private static final String GET_BY_ID = """
        SELECT * FROM duel WHERE id = :id
        """;

    private static final String ADD_MESSAGE_ID = """
        UPDATE duel
        SET message_id = :message_id
        WHERE id = :id
        """;

    private static final String ADD_WINNER_ID = """
        UPDATE duel
        SET winner_personage_id = :winner_personage_id
        WHERE id = :id
        """;

    private static final String GET_WAITING_DUELS_WITH_LESS_EXPIRE_DATE = """
        SELECT * FROM duel
        WHERE status = :status AND expiring_date <= :expiring_date;
        """;

    private static final String UPDATE_STATUS = """
        UPDATE duel
        SET status = :status
        WHERE id = :id
        """;

    private static final DuelRowMapper ROW_MAPPER = new DuelRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public DuelDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("duel")
            .usingColumns(
                "initiating_personage_id",
                "accepting_personage_id",
                "grouptg_id",
                "expiring_date",
                "status"
            )
            .usingGeneratedKeyColumns("id");
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long create(
        long initiatingPersonageId,
        long acceptingPersonageId,
        long groupId,
        Duration lifeTime
    ) {
        final var params = new HashMap<String, Object>() {{
            put("initiating_personage_id", initiatingPersonageId);
            put("accepting_personage_id", acceptingPersonageId);
            put("grouptg_id", groupId);
            put("expiring_date", TimeUtils.moscowTime().plus(lifeTime));
            put("status", DuelStatus.WAITING.id());
        }};
        return jdbcInsert.executeAndReturnKey(params).longValue();
    }

    public Optional<Duel> getById(long id) {
        return jdbcTemplate.query(
            GET_BY_ID,
            Collections.singletonMap("id", id),
            ROW_MAPPER
        ).stream().findFirst();
    }

    public Optional<Duel> getWaitingDuelByInitiatingPersonage(long initiatingPersonageId) {
        final var params = new HashMap<String, Object>() {{
            put("initiating_personage_id", initiatingPersonageId);
            put("status", DuelStatus.WAITING.id());
        }};
        return jdbcTemplate.query(
            GET_WAITING_BY_INITIATING_PERSONAGE,
            params,
            ROW_MAPPER
        ).stream().findFirst();
    }

    public List<Duel> getWaitingDuelsWithLessExpireDate(LocalDateTime expiringDate) {
        final var params = new HashMap<String, Object>() {{
            put("status", DuelStatus.WAITING.id());
            put("expiring_date", expiringDate);
        }};
        return jdbcTemplate.query(
            GET_WAITING_DUELS_WITH_LESS_EXPIRE_DATE,
            params,
            ROW_MAPPER
        );
    }

    public void addMessageIdToDuel(long duelId, int messageId) {
        final var params = new HashMap<String, Object>() {{
            put("id", duelId);
            put("message_id", messageId);
        }};
        jdbcTemplate.update(
            ADD_MESSAGE_ID,
            params
        );
    }

    public void addWinnerIdToDuel(long duelId, long personageId) {
        final var params = new HashMap<String, Object>() {{
            put("id", duelId);
            put("winner_personage_id", personageId);
        }};
        jdbcTemplate.update(
            ADD_WINNER_ID,
            params
        );
    }

    public void updateStatus(long duelId, DuelStatus status) {
        final var params = new HashMap<String, Object>() {{
            put("id", duelId);
            put("status", status.id());
        }};
        jdbcTemplate.update(
            UPDATE_STATUS,
            params
        );
    }

    private static class DuelRowMapper implements RowMapper<Duel> {

        @Override
        public Duel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Duel(
                rs.getLong("id"),
                rs.getLong("initiating_personage_id"),
                rs.getLong("accepting_personage_id"),
                rs.getLong("grouptg_id"),
                rs.getTimestamp("expiring_date").toLocalDateTime(),
                DuelStatus.getById(rs.getInt("status")),
                Optional.ofNullable(DatabaseUtils.getNullableInt(rs, "message_id"))
            );
        }
    }
}
