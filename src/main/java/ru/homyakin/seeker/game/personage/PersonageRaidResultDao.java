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
import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class PersonageRaidResultDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonageRaidResultDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcClient = JdbcClient.create(jdbcTemplate);
        this.jsonUtils = jsonUtils;
    }

    public void saveBatch(List<PersonageRaidResult> results, LaunchedEvent launchedEvent) {
        final var parameters = new ArrayList<SqlParameterSource>();
        for (PersonageRaidResult result : results) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("personage_id", result.personage().id().value())
                .addValue("launched_event_id", launchedEvent.id())
                .addValue("stats", jsonUtils.mapToPostgresJson(result.stats()))
                .addValue("reward", result.reward().value());
            parameters.add(paramSource);
        }
        jdbcTemplate.batchUpdate(SAVE_RESULT, parameters.toArray(new SqlParameterSource[0]));
    }

    public Optional<PersonageRaidSavedResult> getLastByPersonage(PersonageId personageId) {
        return jdbcClient.sql(SELECT_LAST_RESULT)
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional();
    }

    public Optional<PersonageRaidSavedResult> getByPersonageAndEvent(PersonageId personageId, LaunchedEvent launchedEvent) {
        return jdbcClient.sql(SELECT_BY_PERSONAGE_AND_EVENT)
            .param("personage_id", personageId.value())
            .param("launched_event_id", launchedEvent.id())
            .query(this::mapRow)
            .optional();
    }

    private static final String SAVE_RESULT = """
        INSERT INTO personage_raid_result (personage_id, launched_event_id, stats, reward)
        VALUES (:personage_id, :launched_event_id, CAST(:stats AS JSON), :reward)
        """;

    private static final String SELECT_LAST_RESULT = """
        SELECT *
        FROM personage_raid_result
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

    private PersonageRaidSavedResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PersonageRaidSavedResult(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getLong("launched_event_id"),
            jsonUtils.fromString(rs.getString("stats"), PersonageBattleStats.class),
            Money.from(rs.getInt("reward"))
        );
    }
}
