package ru.homyakin.seeker.telegram.group.duel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Repository
public class DuelTgDao {
    private final JdbcClient jdbcClient;

    public DuelTgDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public DuelTg insert(DuelTg duelTg) {
        jdbcClient.sql(INSERT)
            .param("duel_id", duelTg.duelId())
            .param("grouptg_id", duelTg.groupTgId().value())
            .param("message_id", duelTg.messageId())
            .update();

        return duelTg;
    }

    public List<DuelTg> findNotFinalWithLessExpireDateTime(LocalDateTime expiringDateTime) {
        return jdbcClient.sql(GET_WITH_LESS_EXPIRE_DATE_AND_STATUS)
            .param("status_id", DuelStatus.WAITING.id())
            .param("expiring_date", expiringDateTime)
            .query(this::mapRow)
            .list();
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

