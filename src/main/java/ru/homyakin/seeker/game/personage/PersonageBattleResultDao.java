package ru.homyakin.seeker.game.personage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v3.PersonageBattleStats;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.BattleType;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class PersonageBattleResultDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonageBattleResultDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcClient = JdbcClient.create(jdbcTemplate);
        this.jsonUtils = jsonUtils;
    }

    public void saveBatch(List<PersonageBattleResult> results) {
        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var result : results) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("personage_id", result.personageId().value())
                .addValue("launched_event_id", result.launchedEventId())
                .addValue("stats", jsonUtils.mapToPostgresJson(result.stats()))
                .addValue("reward", result.reward().value())
                .addValue("generated_item_id", result.generatedItemId().orElse(null));
            parameters.add(paramSource);
        }
        jdbcTemplate.batchUpdate(SAVE_RESULT, parameters.toArray(new SqlParameterSource[0]));
    }

    public Optional<PersonageBattleResult> getLastByPersonage(
        PersonageId personageId,
        BattleType battleType
    ) {
        return jdbcClient.sql(SELECT_LAST_RESULT)
            .param("personage_id", personageId.value())
            .param("type_id", battleType.id())
            .query(this::mapRow)
            .optional();
    }

    public Optional<PersonageBattleResult> getByPersonageAndEvent(PersonageId personageId, long launchedEventId) {
        return jdbcClient.sql(SELECT_BY_PERSONAGE_AND_EVENT)
            .param("personage_id", personageId.value())
            .param("launched_event_id", launchedEventId)
            .query(this::mapRow)
            .optional();
    }

    public int countSuccessRaidsFromLastItem(PersonageId personageId) {
        return jdbcClient.sql(COUNT_BATTLE_RESULTS_WITHOUT_ITEM_IN_STATUSES)
            .param("personage_id", personageId.value())
            .param("event_statuses", List.of(EventStatus.SUCCESS.id()))
            .param("event_type_id", BattleType.RAID.id())
            .query((rs, _) -> rs.getInt("raids_count"))
            .single();
    }

    public int countWorldRaidsFromLastItem(PersonageId personageId) {
        return jdbcClient.sql(COUNT_BATTLE_RESULTS_WITHOUT_ITEM_IN_STATUSES)
            .param("personage_id", personageId.value())
            .param("event_statuses", List.of(EventStatus.SUCCESS.id(), EventStatus.FAILED.id()))
            .param("event_type_id", BattleType.WORLD_RAID.id())
            .query((rs, _) -> rs.getInt("raids_count"))
            .single();
    }

    private static final String SAVE_RESULT = """
        INSERT INTO personage_raid_result (personage_id, launched_event_id, stats, reward, generated_item_id)
        VALUES (:personage_id, :launched_event_id, CAST(:stats AS JSON), :reward, :generated_item_id)
        """;

    private static final String SELECT_LAST_RESULT = """
        SELECT prr.*
        FROM personage_raid_result prr
        INNER JOIN launched_event le ON prr.launched_event_id = le.id
        INNER JOIN event e ON e.id = le.event_id AND e.type_id = :type_id
        WHERE personage_id = :personage_id
        ORDER BY launched_event_id DESC
        LIMIT 1;
        """;

    private static final String SELECT_BY_PERSONAGE_AND_EVENT = """
        SELECT *
        FROM personage_raid_result
        WHERE personage_id = :personage_id
        and launched_event_id = :launched_event_id
        """;

    private static final String COUNT_BATTLE_RESULTS_WITHOUT_ITEM_IN_STATUSES = """
        WITH last_non_null_item AS (
            SELECT MAX(prr.launched_event_id) AS last_event_with_item -- could be null
            FROM personage_raid_result prr
            LEFT JOIN launched_event le on le.id = prr.launched_event_id
            INNER JOIN event e on le.event_id = e.id AND e.type_id = :event_type_id
            WHERE personage_id = :personage_id AND generated_item_id IS NOT NULL
        )
        SELECT SUM(
            CASE WHEN COALESCE((pte.personage_params->>'isExhausted')::boolean, false) = false THEN 1 ELSE 0 END
        ) AS raids_count
        FROM personage_raid_result prr
        LEFT JOIN personage_to_event pte ON pte.personage_id = prr.personage_id AND pte.launched_event_id = prr.launched_event_id
        LEFT JOIN launched_event le on le.id = pte.launched_event_id
        INNER JOIN event e on le.event_id = e.id AND e.type_id = :event_type_id
        WHERE prr.personage_id = :personage_id
        AND prr.launched_event_id > -- more id => newer event
            COALESCE((SELECT last_event_with_item FROM last_non_null_item), -1) -- all events id > 0
        AND le.status_id in (:event_statuses)
        AND generated_item_id IS NULL
        """;

    private PersonageBattleResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PersonageBattleResult(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getLong("launched_event_id"),
            jsonUtils.fromString(rs.getString("stats"), PersonageBattleStats.class),
            Money.from(rs.getInt("reward")),
            Optional.ofNullable(DatabaseUtils.getLongOrNull(rs, "generated_item_id"))
        );
    }
}
