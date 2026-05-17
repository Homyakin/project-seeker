package ru.homyakin.seeker.game.item.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
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
