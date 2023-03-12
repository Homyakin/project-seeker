package ru.homyakin.seeker.telegram.group.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.telegram.group.models.GroupStats;

@Repository
public class GroupStatsDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupStatsDao(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void create(long groupId) {
        final var sql = "INSERT INTO grouptg_stats (grouptg_id) VALUES (:grouptg_id)";
        final var param = Collections.singletonMap("grouptg_id", groupId);
        jdbcTemplate.update(sql, param);
    }

    public Optional<GroupStats> getById(long groupId) {
        final var sql = "SELECT * FROM grouptg_stats WHERE grouptg_id = :grouptg_id";
        final var param = Collections.singletonMap("grouptg_id", groupId);
        return jdbcTemplate.query(sql, param, this::mapRow).stream().findFirst();
    }

    public void increaseRaidsComplete(long groupId, int amount) {
        final var sql = "UPDATE grouptg_stats SET raids_complete = raids_complete + :amount WHERE grouptg_id = :grouptg_id";
        final var params = new HashMap<String, Object>();
        params.put("amount", amount);
        params.put("grouptg_id", groupId);
        jdbcTemplate.update(sql, params);
    }

    public void increaseDuelsComplete(long groupId, int amount) {
        final var sql = "UPDATE grouptg_stats SET duels_complete = duels_complete + :amount WHERE grouptg_id = :grouptg_id";
        final var params = new HashMap<String, Object>();
        params.put("amount", amount);
        params.put("grouptg_id", groupId);
        jdbcTemplate.update(sql, params);
    }

    public void increaseTavernMoneySpent(long groupId, long amount) {
        final var sql = "UPDATE grouptg_stats SET tavern_money_spent = tavern_money_spent + :amount WHERE grouptg_id = :grouptg_id";
        final var params = new HashMap<String, Object>();
        params.put("amount", amount);
        params.put("grouptg_id", groupId);
        jdbcTemplate.update(sql, params);
    }

    private GroupStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupStats(
            rs.getLong("grouptg_id"),
            rs.getInt("raids_complete"),
            rs.getInt("duels_complete"),
            rs.getLong("tavern_money_spent")
        );
    }
}
