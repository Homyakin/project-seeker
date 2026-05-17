package ru.homyakin.seeker.game.item.modifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.infrastructure.database.ManyToManyUpdater;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.LegacySavingModifier;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class LegacyItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final ManyToManyUpdater updater;

    public LegacyItemModifierDao(DataSource dataSource, JsonUtils jsonUtils, ManyToManyUpdater updater) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
        this.updater = updater;
    }

    public void saveModifier(LegacySavingModifier modifier) {
        final var sql = """
            INSERT INTO legacy_item_modifier (code, item_modifier_type_id, characteristics, locale)
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

    public LegacyGenerateModifier getRandomModifier(LegacyItemRarity rarity) {
        final var sql = """
            SELECT * FROM legacy_item_modifier im
             LEFT JOIN legacy_item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
             WHERE imtir.item_rarity_id = :item_rarity_id
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .param("item_rarity_id", rarity.id)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    public LegacyGenerateModifier getRandomModifierExcludeId(int id, LegacyItemRarity rarity) {
        final var sql = """
            SELECT * FROM legacy_item_modifier im
             LEFT JOIN legacy_item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
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

    public LegacyGenerateModifier getRandomModifierWithType(LegacyModifierType type, LegacyItemRarity rarity) {
        final var sql = """
            SELECT * FROM legacy_item_modifier im
             LEFT JOIN legacy_item_modifier_to_item_rarity imtir on im.id = imtir.item_modifier_id
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

    public LegacyGenerateModifier getById(int id) {
        final var sql = """
            SELECT * FROM legacy_item_modifier im WHERE im.id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", id)
            .query(this::mapRow)
            .single();
    }

    public List<LegacyGenerateModifier> getByIds(List<Integer> ids) {
        final var sql = """
            SELECT * FROM legacy_item_modifier im WHERE im.id in (:ids)
            """;
        return jdbcClient.sql(sql)
            .param("ids", ids)
            .query(this::mapRow)
            .list();
    }

    private void saveModifierRarities(int id, Set<LegacyItemRarity> rarities) {
        updater.update(
            "legacy_item_modifier_to_item_rarity",
            "item_modifier_id",
            "item_rarity_id",
            id,
            rarities.stream().map(it -> it.id).toList()
        );
    }

    private LegacyGenerateModifier mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LegacyGenerateModifier(
            rs.getInt("id"),
            rs.getString("code"),
            LegacyModifierType.findById(rs.getInt("item_modifier_type_id")),
            jsonUtils.fromString(rs.getString("characteristics"), ModifierGenerateCharacteristics.class),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.MODIFIER_LOCALE)
        );
    }
}
