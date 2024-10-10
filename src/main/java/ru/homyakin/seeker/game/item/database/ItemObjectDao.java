package ru.homyakin.seeker.game.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.infrastructure.database.ManyToManyUpdater;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemObjectDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final ManyToManyUpdater updater;

    public ItemObjectDao(DataSource dataSource, JsonUtils jsonUtils, ManyToManyUpdater updater) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.updater = updater;
        this.jsonUtils = jsonUtils;
    }

    @Transactional
    public void saveObject(SavingItemObject object) {
        final var sql = """
            INSERT INTO item_object (code, characteristics, locale)
            VALUES (:code, CAST(:characteristics AS JSON), CAST(:locale AS JSON))
            ON CONFLICT (code) DO
            UPDATE SET characteristics = CAST(:characteristics AS JSON), locale = CAST(:locale AS JSON)
            RETURNING id
            """;

        final var id = jdbcClient
            .sql(sql)
            .param("code", object.code())
            .param("characteristics", jsonUtils.mapToPostgresJson(object.characteristics()))
            .param("locale", jsonUtils.mapToPostgresJson(object.locales()))
            .query((rs, _) -> rs.getInt("id"))
            .single();

        saveObjectSlots(id, object.slots());
        saveObjectRarities(id, object.rarities());
    }

    public GenerateItemObject getRandomObject(ItemRarity rarity, PersonageSlot slot) {
        final var sql = """
            WITH random_object AS (
               SELECT id FROM item_object io
                LEFT JOIN item_object_to_item_rarity iotir on io.id = iotir.item_object_id
                LEFT JOIN item_object_to_personage_slot iotps on io.id = iotps.item_object_id
                WHERE iotir.item_rarity_id = :item_rarity_id AND iotps.personage_slot_id = :slot_id
               ORDER BY random() LIMIT 1
            )
            SELECT * FROM random_object ro
            LEFT JOIN item_object io ON io.id = ro.id
            LEFT JOIN item_object_to_personage_slot iotps on io.id = iotps.item_object_id
            WHERE io.id = ro.id
            """;
        return jdbcClient.sql(sql)
            .param("item_rarity_id", rarity.id)
            .param("slot_id", slot.id)
            .query(this::extractSingleObject);
    }

    private void saveObjectSlots(int id, Set<PersonageSlot> slots) {
        updater.update(
            "item_object_to_personage_slot",
            "item_object_id",
            "personage_slot_id",
            id,
            slots.stream().map(it -> it.id).toList()
        );
    }

    private void saveObjectRarities(int id, Set<ItemRarity> rarities) {
        updater.update(
            "item_object_to_item_rarity",
            "item_object_id",
            "item_rarity_id",
            id,
            rarities.stream().map(it -> it.id).toList()
        );
    }

    private GenerateItemObject extractSingleObject(ResultSet rs) throws SQLException {
        rs.next();
        final var id = rs.getInt("id");
        final var code = rs.getString("code");
        final var locale = jsonUtils.fromString(rs.getString("locale"), JsonUtils.ITEM_OBJECT_LOCALE);
        final var characteristics = jsonUtils.fromString(rs.getString("characteristics"), ObjectGenerateCharacteristics.class);
        final var slots = new HashSet<PersonageSlot>();
        do {
            slots.add(PersonageSlot.findById(rs.getInt("personage_slot_id")));
        } while (rs.next());

        return new GenerateItemObject(
            id,
            code,
            slots,
            characteristics,
            locale
        );
    }
}
