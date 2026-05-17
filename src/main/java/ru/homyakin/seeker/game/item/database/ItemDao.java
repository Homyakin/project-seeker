package ru.homyakin.seeker.game.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class ItemDao {
    private final JdbcClient jdbcClient;
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;

    public ItemDao(DataSource dataSource, ItemObjectDao itemObjectDao, ItemModifierDao itemModifierDao) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
    }

    @Transactional
    public long save(PersonageItem item) {
        return jdbcClient.sql(SAVE_SQL)
            .param("item_object_id", item.objectId())
            .param("item_modifier_id", item.modifierId().orElse(null))
            .param("rarity", item.rarity().ordinal())
            .param("personage_id", item.personageId().map(PersonageId::value).orElse(null))
            .param("is_equipped", item.isEquipped())
            .query((rs, _) -> rs.getLong("id"))
            .single();
    }

    public Optional<PersonageItem> getById(long id) {
        return jdbcClient.sql(SELECT_SQL + " WHERE i.id = :id")
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    public List<PersonageItem> getByPersonageId(PersonageId personageId) {
        return jdbcClient.sql(SELECT_SQL + " WHERE i.personage_id = :personage_id")
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .list();
    }

    public Characteristics getEquippedCharacteristics(PersonageId personageId) {
        return getEquippedCharacteristicsByPersonageIds(Set.of(personageId))
            .getOrDefault(personageId, Characteristics.ZERO);
    }

    public Map<PersonageId, Characteristics> getEquippedCharacteristicsByPersonageIds(Set<PersonageId> personageIds) {
        if (personageIds.isEmpty()) {
            return Map.of();
        }
        final var result = new HashMap<PersonageId, Characteristics>();
        jdbcClient.sql(EQUIPPED_CHARACTERISTICS_SQL)
            .param("personage_ids", personageIds.stream().map(PersonageId::value).toList())
            .query((rs, _) -> Map.entry(
                PersonageId.from(rs.getLong("personage_id")),
                new Characteristics(
                    rs.getInt("health"),
                    rs.getInt("attack"),
                    rs.getInt("defense"),
                    0,
                    0,
                    0
                )
            ))
            .list()
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        for (final var personageId : personageIds) {
            result.putIfAbsent(personageId, Characteristics.ZERO);
        }
        return result;
    }

    public void invertEquip(long id) {
        jdbcClient.sql("UPDATE item SET is_equipped = NOT is_equipped WHERE id = :id")
            .param("id", id)
            .update();
    }

    public void updateEnhancement(long id, Integer modifierId, ItemRarity rarity) {
        jdbcClient.sql("""
                UPDATE item SET item_modifier_id = :item_modifier_id, rarity = :rarity WHERE id = :id
                """)
            .param("id", id)
            .param("item_modifier_id", modifierId)
            .param("rarity", rarity.ordinal())
            .update();
    }

    public void deletePersonageAndMakeEquipFalse(long id) {
        jdbcClient.sql("UPDATE item SET personage_id = null, is_equipped = false WHERE id = :id")
            .param("id", id)
            .update();
    }

    private PersonageItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var objectId = rs.getInt("item_object_id");
        final var object = itemObjectDao.getById(objectId).orElseThrow();
        final var modifierId = (Integer) rs.getObject("item_modifier_id");
        final var modifier = modifierId == null
            ? Optional.<ru.homyakin.seeker.game.item.models.Modifier>empty()
            : itemModifierDao.getById(modifierId).map(ItemModifierDao.ModifierRow::modifier);
        return new PersonageItem(
            rs.getLong("id"),
            objectId,
            object.object(),
            Optional.ofNullable(modifierId),
            modifier,
            ItemRarity.values()[rs.getInt("rarity")],
            Optional.ofNullable((Long) rs.getObject("personage_id")).map(PersonageId::from),
            rs.getBoolean("is_equipped")
        );
    }

    private static final String SAVE_SQL = """
        INSERT INTO item (item_object_id, item_modifier_id, rarity, personage_id, is_equipped)
        VALUES (:item_object_id, :item_modifier_id, :rarity, :personage_id, :is_equipped)
        RETURNING id
        """;

    private static final String SELECT_SQL = """
        SELECT i.id, i.item_object_id, i.item_modifier_id, i.rarity, i.personage_id, i.is_equipped
        FROM item i
        """;

    private static final String EQUIPPED_CHARACTERISTICS_SQL = """
        SELECT i.personage_id,
            COALESCE(SUM(io.health), 0) AS health,
            COALESCE(SUM(io.attack), 0) AS attack,
            COALESCE(SUM(io.defense), 0) AS defense
        FROM item i
        INNER JOIN item_object io ON i.item_object_id = io.id
        WHERE i.personage_id IN (:personage_ids) AND i.is_equipped = true
        GROUP BY i.personage_id
        """;
}
