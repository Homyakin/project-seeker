package ru.homyakin.seeker.game.personage.event;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.EventPersonageParams;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.models.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PersonageEventDao {
    private static final String SAVE_USER_EVENT = """
        INSERT INTO personage_to_event (personage_id, launched_event_id, personage_params, spent_energy)
        VALUES (:personage_id, :launched_event_id, :personage_params, :spent_energy);
        """;

    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonageEventDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void save(AddPersonageToEventRequest request) {
        jdbcClient.sql(SAVE_USER_EVENT)
            .param("personage_id", request.personageId().value())
            .param("launched_event_id", request.launchedEventId())
            .param("personage_params", request.personageParams().map(jsonUtils::mapToPostgresJson).orElse(null))
            .param("spent_energy", request.spentEnergy())
            .update();
    }

    public Map<PersonageId, Optional<EventPersonageParams>> getPersonageParamsByLaunchedEvent(long launchedEventId) {
        final var sql = """
            SELECT *
            FROM personage_to_event
            WHERE launched_event_id = :launched_event_id
            """;

        return jdbcClient.sql(sql)
            .param("launched_event_id", launchedEventId)
            .query(this::mapRow)
            .list()
            .stream()
            .collect(Collectors.toMap(Pair::first, Pair::second));
    }

    private Pair<PersonageId, Optional<EventPersonageParams>> mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Pair.of(
            PersonageId.from(rs.getLong("personage_id")),
            Optional.ofNullable(rs.getString("personage_params")).map(it -> jsonUtils.fromString(it, EventPersonageParams.class))
        );
    }
}
