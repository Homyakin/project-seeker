package ru.homyakin.seeker.game.stats.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.stats.entity.GroupStats;
import ru.homyakin.seeker.game.stats.entity.GroupStatsStorage;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class GroupStatsPostgresDao implements GroupStatsStorage {
    private final JdbcClient jdbcClient;

    public GroupStatsPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<GroupStats> get(GroupId groupId) {
        final var sql = """
            SELECT * FROM pgroup WHERE id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public void increaseRaidsComplete(GroupId groupId, int count) {
        final var sql = """
            UPDATE pgroup SET raids_complete = raids_complete + :count WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("count", count)
            .param("id", groupId.value())
            .update();
    }

    @Override
    public void increaseDuelsComplete(GroupId groupId, int count) {
        final var sql = """
            UPDATE pgroup SET duels_complete = duels_complete + :count WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("count", count)
            .param("id", groupId.value())
            .update();
    }

    @Override
    public void increaseTavernMoneySpent(GroupId groupId, long moneySpent) {
        final var sql = """
            UPDATE pgroup SET tavern_money_spent = tavern_money_spent + :money_spent WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("money_spent", moneySpent)
            .param("id", groupId.value())
            .update();
    }

    private GroupStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupStats(
            GroupId.from(rs.getLong("id")),
            rs.getInt("raids_complete"),
            rs.getInt("duels_complete"),
            rs.getLong("tavern_money_spent")
        );
    }
}
