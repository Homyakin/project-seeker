package ru.homyakin.seeker.telegram.group.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Repository
public class GroupPersonageStatsDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupPersonageStatsDao(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void create(GroupId groupId, PersonageId personageId) {
        final var sql = "INSERT INTO grouptg_personage_stats (grouptg_id, personage_id) VALUES (:grouptg_id, :personage_id)";
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId.value());
        params.put("personage_id", personageId.value());
        jdbcTemplate.update(sql, params);
    }

    public Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId) {
        final var sql = "SELECT * FROM grouptg_personage_stats WHERE grouptg_id = :grouptg_id and personage_id = :personage_id";
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId.value());
        params.put("personage_id", personageId.value());
        return jdbcTemplate.query(sql, params, this::mapRow).stream().findFirst();
    }

    public void update(GroupPersonageStats stats) {
        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", stats.groupId().value());
        params.put("personage_id", stats.personageId().value());
        params.put("raids_success", stats.raidsSuccess());
        params.put("raids_total", stats.raidsTotal());
        params.put("duels_wins", stats.duelsWins());
        params.put("duels_total", stats.duelsTotal());
        params.put("tavern_money_spent", stats.tavernMoneySpent());
        params.put("spin_wins_count", stats.spinWinsCount());
        jdbcTemplate.update(UPDATE, params);
    }

    private GroupPersonageStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupPersonageStats(
            GroupId.from(rs.getLong("grouptg_id")),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getInt("raids_success"),
            rs.getInt("raids_total"),
            rs.getInt("duels_wins"),
            rs.getInt("duels_total"),
            rs.getLong("tavern_money_spent"),
            rs.getInt("spin_wins_count")
        );
    }
    
    private static String UPDATE = """
        UPDATE grouptg_personage_stats
        SET
            raids_success = :raids_success,
            raids_total = :raids_total,
            duels_wins = :duels_wins,
            duels_total = :duels_total,
            tavern_money_spent = :tavern_money_spent,
            spin_wins_count = :spin_wins_count
        WHERE grouptg_id = :grouptg_id AND personage_id = :personage_id;
        """;
}
