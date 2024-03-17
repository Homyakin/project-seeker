package ru.homyakin.seeker.game.item.database;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ItemModifierDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void saveModifier(Modifier modifier) {
        final var sql = """
            INSERT INTO item_modifier (code, item_modifier_type_id, characteristics, locale)
            VALUES (:code, :item_modifier_type_id, CAST(:characteristics AS JSON), CAST(:locale AS JSON))
            ON CONFLICT (code) DO
            UPDATE SET item_modifier_type_id = :item_modifier_type_id, characteristics = CAST(:characteristics AS JSON),
            locale = CAST(:locale AS JSON)
            """;

        jdbcClient
            .sql(sql)
            .param("code", modifier.code())
            .param("item_modifier_type_id", modifier.type().id)
            .param("characteristics", jsonUtils.mapToPostgresJson(modifier.characteristics()))
            .param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
            .update();
    }
}
