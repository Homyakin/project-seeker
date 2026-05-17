package ru.homyakin.seeker.game.item.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final DataSource dataSource;

    public ItemModifierDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
        this.dataSource = dataSource;
    }

    @Transactional
    public void save(ItemModifiersToml.SavingModifier modifier) {
        jdbcClient.sql(SAVE_SQL)
            .param("code", modifier.code())
            .param("active_enum", modifier.activeEnum().name())
            .param("type_id", modifier.type().id)
            .param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
            .param("personage_slot_ids", personageSlotIdsArray(modifier.slots()))
            .update();
    }

    private Array personageSlotIdsArray(Set<PersonageSlot> slots) {
        final var ids = slots.stream().map(slot -> slot.id).toArray(Integer[]::new);
        try (Connection connection = dataSource.getConnection()) {
            return connection.createArrayOf("integer", ids);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create personage_slot_ids array", e);
        }
    }

    private static final String SAVE_SQL = """
        INSERT INTO item_modifier (
            code, active_enum, type_id, locale, personage_slot_ids
        ) VALUES (
            :code, :active_enum, :type_id, CAST(:locale AS JSONB), :personage_slot_ids
        )
        ON CONFLICT (code) DO UPDATE SET
            active_enum = EXCLUDED.active_enum,
            type_id = EXCLUDED.type_id,
            locale = EXCLUDED.locale,
            personage_slot_ids = EXCLUDED.personage_slot_ids
        """;
}
