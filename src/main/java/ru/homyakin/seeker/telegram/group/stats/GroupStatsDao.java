package ru.homyakin.seeker.telegram.group.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Repository
public class GroupStatsDao {
    private final JdbcClient jdbcClient;

    public GroupStatsDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public void create(GroupTgId groupId) {
        jdbcClient.sql("INSERT INTO grouptg_stats (grouptg_id) VALUES (:grouptg_id)")
            .param("grouptg_id", groupId.value())
            .update();
    }

    public Optional<GroupStats> getById(GroupTgId groupId) {
        return jdbcClient.sql("SELECT * FROM grouptg_stats WHERE grouptg_id = :grouptg_id")
            .param("grouptg_id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    public void increaseRaidsComplete(GroupTgId groupId, int amount) {
        jdbcClient.sql("UPDATE grouptg_stats SET raids_complete = raids_complete + :amount WHERE grouptg_id = :grouptg_id")
            .param("amount", amount)
            .param("grouptg_id", groupId.value())
            .update();
    }

    public void increaseDuelsComplete(GroupTgId groupId, int amount) {
        jdbcClient.sql("UPDATE grouptg_stats SET duels_complete = duels_complete + :amount WHERE grouptg_id = :grouptg_id")
            .param("amount", amount)
            .param("grouptg_id", groupId.value())
            .update();
    }

    public void increaseTavernMoneySpent(GroupTgId groupId, long amount) {
        jdbcClient.sql("UPDATE grouptg_stats SET tavern_money_spent = tavern_money_spent + :amount WHERE grouptg_id = :grouptg_id")
            .param("amount", amount)
            .param("grouptg_id", groupId.value())
            .update();
    }

    private GroupStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupStats(
            GroupTgId.from(rs.getLong("grouptg_id")),
            rs.getInt("raids_complete"),
            rs.getInt("duels_complete"),
            rs.getLong("tavern_money_spent")
        );
    }
}
