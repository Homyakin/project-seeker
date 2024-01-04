package ru.homyakin.seeker.game.personage;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.models.PersonageRaidResult;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class PersonageRaidResultDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonUtils jsonUtils;

    public PersonageRaidResultDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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

    private static final  String SAVE_RESULT = """
        INSERT INTO personage_raid_result (personage_id, launched_event_id, stats, reward)
        VALUES (:personage_id, :launched_event_id, CAST(:stats AS JSON), :reward)
        """;
}
