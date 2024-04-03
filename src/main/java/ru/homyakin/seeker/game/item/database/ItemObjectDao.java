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
import ru.homyakin.seeker.game.item.models.ItemGenerateCharacteristics;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemObjectDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ItemObjectDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
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
            .query((rs, rowNum) -> rs.getInt("id"))
            .single();

        saveObjectSlots(id, object.slots());
    }

    public GenerateItemObject getRandomObject() {
        final var sql = """
            WITH random_object AS (
               SELECT id FROM item_object
               ORDER BY random() LIMIT 1
            )
            SELECT * FROM item_object io
            LEFT JOIN item_object_to_personage_slot iotps on io.id = iotps.item_object_id
            LEFT JOIN random_object ro ON io.id = ro.id
            WHERE io.id = ro.id
            """;
        return jdbcClient.sql(sql)
            .query(this::extractSingleObject);
    }

    private void saveObjectSlots(int id, Set<PersonageSlot> slots) {
        final var selectExistingSlots = """
            SELECT * FROM item_object_to_personage_slot WHERE item_object_id = :item_object_id
            """;

        final var existingSlots = jdbcClient
            .sql(selectExistingSlots)
            .param("item_object_id", id)
            .query((rs, rowNum) -> PersonageSlot.findById(rs.getInt("personage_slot_id")))
            .set();

        final var insert = """
            INSERT INTO item_object_to_personage_slot (item_object_id, personage_slot_id) 
            VALUES (:item_object_id, :personage_slot_id)
            """;
        for (final var slot : slots) {
            if (!existingSlots.contains(slot)) {
                jdbcClient
                    .sql(insert)
                    .param("item_object_id", id)
                    .param("personage_slot_id", slot.id)
                    .update();
            }
        }

        final var delete = """
            DELETE FROM item_object_to_personage_slot
            WHERE item_object_id = :item_object_id AND personage_slot_id = :personage_slot_id
            """;
        for (final var slot : existingSlots) {
            if (!slots.contains(slot)) {
                jdbcClient
                    .sql(delete)
                    .param("item_object_id", id)
                    .param("personage_slot_id", slot.id)
                    .update();
            }
        }
    }

    private GenerateItemObject extractSingleObject(ResultSet rs) throws SQLException {
        rs.next();
        final var id = rs.getInt("id");
        final var code = rs.getString("code");
        final var locale = jsonUtils.fromString(rs.getString("locale"), JsonUtils.ITEM_OBJECT_LOCALE);
        final var characteristics = jsonUtils.fromString(rs.getString("characteristics"), ItemGenerateCharacteristics.class);
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
