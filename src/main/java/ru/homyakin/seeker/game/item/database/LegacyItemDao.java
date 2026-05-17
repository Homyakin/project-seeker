package ru.homyakin.seeker.game.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.item.models.LegacyItemObject;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifier;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class LegacyItemDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public LegacyItemDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Transactional
    public long saveItem(LegacyItem item) {
        final var insertItem = """
            INSERT INTO legacy_item (item_object_id, personage_id, is_equipped, attack, health, defense, item_rarity_id)
            VALUES (:item_object_id, :personage_id, :is_equipped, :attack, :health, :defense, :item_rarity_id)
            RETURNING id
            """;
        final var id = jdbcClient.sql(insertItem)
            .param("item_object_id", item.object().id())
            .param("personage_id", item.personageId().map(PersonageId::value).orElse(null))
            .param("is_equipped", item.isEquipped())
            .param("attack", item.characteristics().attack())
            .param("health", item.characteristics().health())
            .param("defense", item.characteristics().defense())
            .param("item_rarity_id", item.rarity().id)
            .query((rs, _) -> rs.getLong("id"))
            .single();

        final var insertModifier = """
            INSERT INTO legacy_item_to_item_modifier (item_id, item_modifier_id)
            VALUES (:item_id, :item_modifier_id)
            """;
        for (final var modifier : item.modifiers()) {
            jdbcClient.sql(insertModifier)
                .param("item_id", id)
                .param("item_modifier_id", modifier.id())
                .update();
        }
        return id;
    }

    @Transactional
    public void updateItem(long id, Characteristics characteristics, List<LegacyModifier> modifiers) {
        final var updateItem = """
            UPDATE legacy_item SET
                attack = :attack,
                health = :health,
                defense = :defense
            WHERE id = :id
            """;
        jdbcClient.sql(updateItem)
            .param("attack", characteristics.attack())
            .param("health", characteristics.health())
            .param("defense", characteristics.defense())
            .param("id", id)
            .update();

        final var insertModifier = """
            INSERT INTO legacy_item_to_item_modifier (item_id, item_modifier_id)
            VALUES (:item_id, :item_modifier_id)
            ON CONFLICT DO NOTHING
            """;
        for (final var modifier : modifiers) {
            jdbcClient.sql(insertModifier)
                .param("item_id", id)
                .param("item_modifier_id", modifier.id())
                .update();
        }
    }

    public void updateItem(long id, Characteristics characteristics, boolean isBroken) {
        final var updateItem = """
            UPDATE legacy_item SET
                attack = :attack,
                health = :health,
                defense = :defense,
                is_broken = :is_broken
            WHERE id = :id
            """;
        jdbcClient.sql(updateItem)
            .param("attack", characteristics.attack())
            .param("health", characteristics.health())
            .param("defense", characteristics.defense())
            .param("is_broken", isBroken)
            .param("id", id)
            .update();
    }

    public Optional<LegacyItem> getById(Long id) {
        final var sql = SELECT_ITEMS + " WHERE i.id = :id";
        final var result = jdbcClient.sql(sql)
            .param("id", id)
            .query(this::extractItems);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public void invertEquip(long id) {
        final var sql = "UPDATE legacy_item SET is_equipped = not is_equipped WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", id)
            .update();
    }

    public void deletePersonageAndMakeEquipFalse(long id) {
        final var sql = "UPDATE legacy_item SET personage_id = null, is_equipped = false WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", id)
            .update();
    }

    public List<LegacyItem> getByPersonageId(PersonageId personageId) {
        final var sql = SELECT_ITEMS + " WHERE i.personage_id = :personage_id";
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query(this::extractItems);
    }

    private List<LegacyItem> extractItems(ResultSet rs) throws SQLException {
        final var itemIdToItemObjectId = new HashMap<Long, Integer>();
        final var itemObjectIdToSlots = new HashMap<Integer, Set<PersonageSlot>>();
        final var itemIdToModifiers = new HashMap<Long, Set<Integer>>();
        final var modifierMap = new HashMap<Integer, LegacyModifier>();
        final var objectMap = new HashMap<Integer, LegacyItemObject>();
        final var itemMap = new HashMap<Long, LegacyItem>();
        while (rs.next()) {
            final var itemId = rs.getLong("real_item_id");
            final var itemObjectId = rs.getInt("item_object_id");
            if (!itemMap.containsKey(itemId)) {
                final var isEquipped = rs.getBoolean("is_equipped");
                final var isBroken = rs.getBoolean("is_broken");
                final var personageId = Optional.ofNullable((Long) rs.getObject("personage_id"))
                    .map(PersonageId::from);
                final var characteristics = new Characteristics(
                    rs.getInt("health"),
                    rs.getInt("attack"),
                    rs.getInt("defense"),
                    0,
                    0,
                    0
                );
                final var rarity = LegacyItemRarity.findById(rs.getInt("item_rarity_id"));
                itemMap.put(
                    itemId,
                    new LegacyItem(itemId, null, rarity, null, personageId, isEquipped, isBroken, characteristics)
                );
                itemIdToItemObjectId.put(itemId, itemObjectId);
                if (!objectMap.containsKey(itemObjectId)) {
                    final var locale = jsonUtils.fromString(rs.getString("object_locale"), JsonUtils.ITEM_OBJECT_LOCALE);
                    objectMap.put(
                        itemObjectId,
                        new LegacyItemObject(itemObjectId, null, locale)
                    );
                }
            }

            itemObjectIdToSlots.computeIfAbsent(itemObjectId, k -> new HashSet<>())
                .add(PersonageSlot.findById(rs.getInt("personage_slot_id")));

            final var modifierId = rs.getInt("item_modifier_id");
            if (rs.wasNull()) {
                continue;
            }
            itemIdToModifiers.computeIfAbsent(itemId, k -> new HashSet<>()).add(modifierId);
            if (!modifierMap.containsKey(modifierId)) {
                final var type = LegacyModifierType.findById(rs.getInt("item_modifier_type_id"));
                final var locale = jsonUtils.fromString(rs.getString("modifier_locale"), JsonUtils.MODIFIER_LOCALE);
                modifierMap.put(
                    modifierId,
                    new LegacyModifier(modifierId, type, locale)
                );
            }
        }

        final var items = new ArrayList<LegacyItem>();

        for (final var item : itemMap.values()) {
            final var object = objectMap.get(itemIdToItemObjectId.get(item.id()));
            items.add(
                new LegacyItem(
                    item.id(),
                    new LegacyItemObject(
                        object.id(),
                        itemObjectIdToSlots.get(object.id()),
                        object.locales()
                    ),
                    item.rarity(),
                    Optional
                        .ofNullable(itemIdToModifiers.get(item.id()))
                        .map(it -> it.stream().map(modifierMap::get).toList())
                        .orElseGet(List::of),
                    item.personageId(),
                    item.isEquipped(),
                    item.isBroken(),
                    item.characteristics()
                )
            );
        }

        return items;
    }

    private static final String SELECT_ITEMS = """
        SELECT *,
             i.id real_item_id, -- в legacy_item_to_item_modifier тоже item_id
             io.locale object_locale,
             im.locale modifier_locale
            FROM legacy_item i
            LEFT JOIN legacy_item_object io ON i.item_object_id = io.id
            LEFT JOIN legacy_item_object_to_personage_slot iotps ON io.id = iotps.item_object_id
            LEFT JOIN legacy_item_to_item_modifier itim ON i.id = itim.item_id
            LEFT JOIN legacy_item_modifier im on itim.item_modifier_id = im.id
        """;
}
