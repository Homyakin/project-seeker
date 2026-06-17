package ru.homyakin.seeker.game.item.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemObjectDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final DataSource dataSource;

    public ItemObjectDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
        this.dataSource = dataSource;
    }

    public CatalogItemObject getRandomObject(PersonageSlot slot) {
        return jdbcClient.sql(RANDOM_OBJECT_SQL)
            .param("slot_id", slot.id)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    public Optional<CatalogItemObject> getById(int id) {
        return jdbcClient.sql(GET_BY_ID_SQL)
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    public List<CatalogItemObject> listBySlot(PersonageSlot slot) {
        return jdbcClient.sql(LIST_BY_SLOT_SQL)
            .param("slot_id", slot.id)
            .query(this::mapRow)
            .list();
    }

    @Transactional
    public void save(ItemObjectsToml.SavingItemObject object) {
        jdbcClient.sql(SAVE_SQL)
            .param("code", object.code())
            .param("health", object.health())
            .param("crit_chance", object.critChance())
            .param("dodge_chance", object.dodgeChance())
            .param("crit_multiplier", object.critMultiplier())
            .param("speed", object.speed())
            .param("base_threat", object.baseThreat())
            .param("attack_type", object.attack().map(a -> a.attackType().name()).orElse(null))
            .param("attack_range", object.attack().map(ItemObjectsToml.SavingItemAttack::range).orElse(null))
            .param("attack", object.attack().map(ItemObjectsToml.SavingItemAttack::attack).orElse(null))
            .param("defense_type", object.defense().map(d -> d.defenseType().name()).orElse(null))
            .param("defense", object.defense().map(ItemObjectsToml.SavingItemDefense::defense).orElse(null))
            .param("locale", jsonUtils.mapToPostgresJson(object.locales()))
            .param("personage_slot_ids", personageSlotIdsArray(object.slots()))
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

    private CatalogItemObject mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var attackType = rs.getString("attack_type");
        final var defenseType = rs.getString("defense_type");
        return new CatalogItemObject(
            rs.getInt("id"),
            new ItemObject(
                rs.getString("code"),
                extractSlots(rs.getArray("personage_slot_ids")),
                attackType == null
                    ? Optional.empty()
                    : Optional.of(new ItemAttack(
                        AttackType.valueOf(attackType),
                        rs.getInt("attack_range"),
                        rs.getInt("attack")
                    )),
                defenseType == null
                    ? Optional.empty()
                    : Optional.of(new ItemDefense(
                        DefenseType.valueOf(defenseType),
                        rs.getInt("defense")
                    )),
                rs.getInt("health"),
                rs.getInt("crit_chance"),
                rs.getInt("dodge_chance"),
                rs.getDouble("crit_multiplier"),
                rs.getInt("speed"),
                rs.getInt("base_threat"),
                jsonUtils.fromString(rs.getString("locale"), JsonUtils.ITEM_OBJECT_LOCALE)
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

    private static final String RANDOM_OBJECT_SQL = """
        SELECT * FROM item_object
        WHERE :slot_id = ANY(personage_slot_ids)
        ORDER BY random() LIMIT 1
        """;

    private static final String GET_BY_ID_SQL = """
        SELECT * FROM item_object WHERE id = :id
        """;

    private static final String LIST_BY_SLOT_SQL = """
        SELECT * FROM item_object
        WHERE :slot_id = ANY(personage_slot_ids)
        ORDER BY id
        """;

    private static final String SAVE_SQL = """
        INSERT INTO item_object (
            code, health, crit_chance, dodge_chance, crit_multiplier, speed, base_threat,
            attack_type, attack_range, attack, defense_type, defense, locale, personage_slot_ids
        ) VALUES (
            :code, :health, :crit_chance, :dodge_chance, :crit_multiplier, :speed, :base_threat,
            :attack_type, :attack_range, :attack, :defense_type, :defense,
            CAST(:locale AS JSONB), :personage_slot_ids
        )
        ON CONFLICT (code) DO UPDATE SET
            health = EXCLUDED.health,
            crit_chance = EXCLUDED.crit_chance,
            dodge_chance = EXCLUDED.dodge_chance,
            crit_multiplier = EXCLUDED.crit_multiplier,
            speed = EXCLUDED.speed,
            base_threat = EXCLUDED.base_threat,
            attack_type = EXCLUDED.attack_type,
            attack_range = EXCLUDED.attack_range,
            attack = EXCLUDED.attack,
            defense_type = EXCLUDED.defense_type,
            defense = EXCLUDED.defense,
            locale = EXCLUDED.locale,
            personage_slot_ids = EXCLUDED.personage_slot_ids
        """;
}
