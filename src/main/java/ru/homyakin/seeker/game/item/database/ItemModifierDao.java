package ru.homyakin.seeker.game.item.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.models.CatalogModifier;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
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

    public CatalogModifier getRandomModifier(PersonageSlot slot, Set<ModifierType> compatibleTypes) {
        final var typeIds = compatibleTypes.stream().map(type -> type.id).toList();
        return jdbcClient.sql(RANDOM_MODIFIER_SQL)
            .param("slot_id", slot.id)
            .param("type_ids", typeIds)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    public Optional<CatalogModifier> getById(int id) {
        return jdbcClient.sql(GET_BY_ID_SQL)
            .param("id", id)
            .query(this::mapRow)
            .optional();
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

    private CatalogModifier mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CatalogModifier(
            rs.getInt("id"),
            new Modifier(
                rs.getString("code"),
                ActiveEnum.valueOf(rs.getString("active_enum")),
                ModifierType.findById(rs.getInt("type_id")),
                extractSlots(rs.getArray("personage_slot_ids")),
                jsonUtils.fromString(rs.getString("locale"), JsonUtils.MODIFIER_LOCALE)
            )
        );
    }

    private Set<PersonageSlot> extractSlots(Array array) throws SQLException {
        if (array == null) {
            return Set.of();
        }
        final var ids = (Integer[]) array.getArray();
        final var slots = new HashSet<PersonageSlot>();
        for (final var id : ids) {
            slots.add(PersonageSlot.findById(id));
        }
        return slots;
    }

    private static final String RANDOM_MODIFIER_SQL = """
        SELECT * FROM item_modifier
        WHERE :slot_id = ANY(personage_slot_ids)
          AND type_id IN (:type_ids)
        ORDER BY random() LIMIT 1
        """;

    private static final String GET_BY_ID_SQL = """
        SELECT * FROM item_modifier WHERE id = :id
        """;

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
