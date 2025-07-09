package ru.homyakin.seeker.game.group.infra.database;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.v3.GroupBattleStats;
import ru.homyakin.seeker.game.group.entity.GroupBattleResultStorage;
import ru.homyakin.seeker.game.group.entity.SavedGroupBattleResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupBattleResultDao implements GroupBattleResultStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public GroupBattleResultDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcClient = JdbcClient.create(jdbcTemplate);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void saveBatch(List<SavedGroupBattleResult> results) {
        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var result : results) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("pgroup_id", result.groupId().value())
                .addValue("launched_event_id", result.launchedEventId())
                .addValue("stats", jsonUtils.mapToPostgresJson(result.stats()))
                .addValue("reward", result.reward().value());
            parameters.add(paramSource);
        }
        final var sql = """
                INSERT INTO pgroup_battle_result (pgroup_id, launched_event_id, stats, reward)
                VALUES (:pgroup_id, :launched_event_id, :stats, :reward)""";
        jdbcTemplate.batchUpdate(
            sql,
            parameters.toArray(new SqlParameterSource[0])
        );
    }

    @Override
    public Optional<SavedGroupBattleResult> getBattleResult(GroupId groupId, long launchedEventId) {
        final var sql = """
                SELECT * FROM pgroup_battle_result WHERE pgroup_id = :pgroup_id AND launched_event_id = :launched_event_id
                """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("launched_event_id", launchedEventId)
            .query(this::mapRow)
            .optional();
    }

    private SavedGroupBattleResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SavedGroupBattleResult(
            GroupId.from(rs.getLong("pgroup_id")),
            rs.getLong("launched_event_id"),
            jsonUtils.fromString(rs.getString("stats"), GroupBattleStats.class),
            Money.from(rs.getInt("reward"))
        );
    }
}
