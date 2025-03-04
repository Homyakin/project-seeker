package ru.homyakin.seeker.game.personage.settings.infra.postgres;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettingsStorage;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;

@Component
public class PersonageSettingsDao implements PersonageSettingsStorage {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonageSettingsDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public PersonageSettings getSettings(PersonageId personageId) {
        final var sql = "SELECT settings FROM personage WHERE id = :id";
        return jdbcClient.sql(sql)
            .param("id", personageId.value())
            .query(
                (rs, _) ->
                    jsonUtils.fromString(rs.getString("settings"), JsonPersonageSettings.class)
            )
            .single()
            .toDomain();
    }

    @Override
    public void setSettings(PersonageId personageId, PersonageSettings settings) {
        final var sql = "UPDATE personage SET settings = :settings WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", personageId.value())
            .param("settings", jsonUtils.mapToPostgresJson(JsonPersonageSettings.from(settings)))
            .update();
    }
}
