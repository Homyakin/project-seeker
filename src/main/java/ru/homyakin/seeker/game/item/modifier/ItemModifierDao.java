package ru.homyakin.seeker.game.item.modifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.infrastructure.database.ManyToManyUpdater;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingModifier;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final ManyToManyUpdater updater;

    public ItemModifierDao(DataSource dataSource, JsonUtils jsonUtils, ManyToManyUpdater updater) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
        this.updater = updater;
    }

    public void saveModifier(SavingModifier modifier) {
        final var sql = """
            INSERT INTO item_modifier (code, item_modifier_type_id, characteristics, locale)
            VALUES (:code, :item_modifier_type_id, CAST(:characteristics AS JSON), CAST(:locale AS JSON))
            ON CONFLICT (code) DO
            UPDATE SET item_modifier_type_id = :item_modifier_type_id, characteristics = CAST(:characteristics AS JSON),
            locale = CAST(:locale AS JSON)
            RETURNING id
            """;

        final var id = jdbcClient
            .sql(sql)
            .param("code", modifier.code())
            .param("item_modifier_type_id", modifier.type().id)
            .param("characteristics", jsonUtils.mapToPostgresJson(modifier.characteristics()))
            .param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
            .query((rs, _) -> rs.getInt("id"))
            .single();

        saveModifierRarities(id, modifier.rarities());
    }

    public GenerateModifier getRandomModifier(ItemRarity rarity) {
        final var sql = """
            SELECT * FROM item_modifier im
             LEFT JOIN item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
             WHERE imtir.item_rarity_id = :item_rarity_id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("item_rarity_id", rarity.id)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    public GenerateModifier getRandomModifierExcludeId(int id, ItemRarity rarity) {
        final var sql = """
            SELECT * FROM item_modifier im
             LEFT JOIN item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
            WHERE id != :id
            AND imtir.item_rarity_id = :item_rarity_id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("id", id)
            .param("item_rarity_id", rarity.id)
            .query(this::mapRow)
            .single();
    }

    public GenerateModifier getRandomModifierWithType(ModifierType type, ItemRarity rarity) {
        final var sql = """
            SELECT * FROM item_modifier im
             LEFT JOIN item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
            WHERE item_modifier_type_id = :item_modifier_type_id
            AND imtir.item_rarity_id = :item_rarity_id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("item_modifier_type_id", type.id)
            .param("item_rarity_id", rarity.id)
            .query(this::mapRow)
            .single();
    }

    private void saveModifierRarities(int id, Set<ItemRarity> rarities) {
        updater.update(
            "item_modifier_to_item_rarity",
            "item_modifier_id",
            "item_rarity_id",
            id,
            rarities.stream().map(it -> it.id).toList()
        );
    }

    private GenerateModifier mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GenerateModifier(
            rs.getInt("id"),
            rs.getString("code"),
            ModifierType.findById(rs.getInt("item_modifier_type_id")),
            jsonUtils.fromString(rs.getString("characteristics"), ModifierGenerateCharacteristics.class),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.MODIFIER_LOCALE)
        );
    }
}
