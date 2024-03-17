package ru.homyakin.seeker.game.item.database;

import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.models.ItemObject;
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
    public void saveObject(ItemObject object) {
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

    public void saveObjectSlots(int id, Set<PersonageSlot> slots) {
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
        for (final var slot: slots) {
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
        for (final var slot: existingSlots) {
            if (!slots.contains(slot)) {
                jdbcClient
                    .sql(delete)
                    .param("item_object_id", id)
                    .param("personage_slot_id", slot.id)
                    .update();
            }
        }
    }
}
