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
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.modifier.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ItemDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Transactional
    public long saveItem(Item item) {
        final var insertItem = """
            INSERT INTO item (item_object_id, personage_id, is_equipped, attack, health, defense, item_rarity_id)
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
            INSERT INTO item_to_item_modifier (item_id, item_modifier_id)
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

    public Optional<Item> getById(Long id) {
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
        final var sql = "UPDATE item SET is_equipped = not is_equipped WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", id)
            .update();
    }

    public void deletePersonageAndMakeEquipFalse(long id) {
        final var sql = "UPDATE item SET personage_id = null, is_equipped = false WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", id)
            .update();
    }

    public List<Item> getByPersonageId(PersonageId personageId) {
        final var sql = SELECT_ITEMS + " WHERE i.personage_id = :personage_id";
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query(this::extractItems);
    }

    private List<Item> extractItems(ResultSet rs) throws SQLException {
        final var itemIdToItemObjectId = new HashMap<Long, Integer>();
        final var itemObjectIdToSlots = new HashMap<Integer, Set<PersonageSlot>>();
        final var itemIdToModifiers = new HashMap<Long, Set<Integer>>();
        final var modifierMap = new HashMap<Integer, Modifier>();
        final var objectMap = new HashMap<Integer, ItemObject>();
        final var itemMap = new HashMap<Long, Item>();
        while (rs.next()) {
            final var itemId = rs.getLong("real_item_id");
            final var itemObjectId = rs.getInt("item_object_id");
            if (!itemMap.containsKey(itemId)) {
                final var isEquipped = rs.getBoolean("is_equipped");
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
                final var rarity = ItemRarity.findById(rs.getInt("item_rarity_id"));
                itemMap.put(
                    itemId,
                    new Item(itemId, null, rarity, null, personageId, isEquipped, characteristics)
                );
                itemIdToItemObjectId.put(itemId, itemObjectId);
                if (!objectMap.containsKey(itemObjectId)) {
                    final var locale = jsonUtils.fromString(rs.getString("object_locale"), JsonUtils.ITEM_OBJECT_LOCALE);
                    objectMap.put(
                        itemObjectId,
                        new ItemObject(itemObjectId, null, locale)
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
                final var type = ModifierType.findById(rs.getInt("item_modifier_type_id"));
                final var locale = jsonUtils.fromString(rs.getString("modifier_locale"), JsonUtils.MODIFIER_LOCALE);
                modifierMap.put(
                    modifierId,
                    new Modifier(modifierId, type, locale)
                );
            }
        }

        final var items = new ArrayList<Item>();

        for (final var item : itemMap.values()) {
            final var object = objectMap.get(itemIdToItemObjectId.get(item.id()));
            items.add(
                new Item(
                    item.id(),
                    new ItemObject(
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
                    item.characteristics()
                )
            );
        }

        return items;
    }

    private static final String SELECT_ITEMS = """
        SELECT *,
             i.id real_item_id, -- в item_to_item_modifier тоже item_id
             io.locale object_locale,
             im.locale modifier_locale
            FROM item i
            LEFT JOIN item_object io ON i.item_object_id = io.id
            LEFT JOIN item_object_to_personage_slot iotps ON io.id = iotps.item_object_id
            LEFT JOIN item_to_item_modifier itim ON i.id = itim.item_id
            LEFT JOIN item_modifier im on itim.item_modifier_id = im.id
        """;
}
