package ru.homyakin.seeker.telegram.group.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Repository
public class DuelTgDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DuelTgDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public DuelTg insert(DuelTg duelTg) {
        final var params = new MapSqlParameterSource()
            .addValue("duel_id", duelTg.duelId())
            .addValue("grouptg_id", duelTg.groupTgId().value())
            .addValue("message_id", duelTg.messageId());

        jdbcTemplate.update(INSERT, params);

        return duelTg;
    }

    public List<DuelTg> findNotFinalWithLessExpireDateTime(LocalDateTime expiringDateTime) {
        final var params = new MapSqlParameterSource()
            .addValue("status_id", DuelStatus.WAITING.id())
            .addValue("expiring_date", expiringDateTime);

        return jdbcTemplate.query(GET_WITH_LESS_EXPIRE_DATE_AND_STATUS, params, this::mapRow);
    }

    private DuelTg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DuelTg(
            rs.getLong("duel_id"),
            GroupId.from(rs.getLong("grouptg_id")),
            rs.getInt("message_id")
        );
    }

    private static final String INSERT = """
        INSERT INTO duel_tg (duel_id, grouptg_id, message_id)
        VALUES (:duel_id, :grouptg_id, :message_id)
        """;
    private static final String GET_WITH_LESS_EXPIRE_DATE_AND_STATUS = """
        SELECT dt.* FROM duel_tg dt
        LEFT JOIN duel d on d.id = dt.duel_id
        WHERE d.status_id = :status_id AND d.expiring_date <= :expiring_date;
        """;
}

