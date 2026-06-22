package ru.homyakin.seeker.game.battle.infra.database;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.battle.BattleActionLog;
import ru.homyakin.seeker.game.battle.BattleInitState;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;

@Repository
public class EventBattleLogDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonUtils jsonUtils;

    public EventBattleLogDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void save(long launchedEventId, BattleInitState init, BattleActionLog log) {
        final var params = new MapSqlParameterSource()
            .addValue("launched_event_id", launchedEventId)
            .addValue("init", jsonUtils.mapToPostgresJson(init))
            .addValue("log", jsonUtils.mapToPostgresJson(log));
        jdbcTemplate.update(SAVE, params);
    }

    private static final String SAVE = """
        INSERT INTO event_battle_log (launched_event_id, init, log)
        VALUES (:launched_event_id, :init, :log)
        """;
}
