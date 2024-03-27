package ru.homyakin.seeker.game.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemRangeCharacteristics;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingModifier;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ItemModifierDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void saveModifier(SavingModifier modifier) {
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

    public GenerateModifier getRandomModifier() {
        final var sql = """
            SELECT * FROM item_modifier
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    public GenerateModifier getRandomModifierExcludeId(int id) {
        final var sql = """
            SELECT * FROM item_modifier
            WHERE id != :id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("id", id)
            .query(this::mapRow)
            .single();
    }

    public GenerateModifier getRandomModifierWithType(ModifierType type) {
        final var sql = """
            SELECT * FROM item_modifier
            WHERE item_modifier_type_id = :item_modifier_type_id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("item_modifier_type_id", type.id)
            .query(this::mapRow)
            .single();
    }

    private GenerateModifier mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GenerateModifier(
            rs.getInt("id"),
            rs.getString("code"),
            ModifierType.findById(rs.getInt("item_modifier_type_id")),
            jsonUtils.fromString(rs.getString("characteristics"), ItemRangeCharacteristics.class),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.MODIFIER_LOCALE)
        );
    }
}
