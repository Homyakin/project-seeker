package ru.homyakin.seeker.game.item.loadout.infra.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class EquipmentLoadoutDao {
    private final JdbcClient jdbcClient;
    private final DataSource dataSource;

    public EquipmentLoadoutDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.dataSource = dataSource;
    }

    public List<EquipmentLoadout> findByPersonageId(PersonageId personageId) {
        return jdbcClient.sql(SELECT_SQL + " WHERE personage_id = :personage_id ORDER BY id")
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .list();
    }

    public Optional<EquipmentLoadout> findById(long id) {
        return jdbcClient.sql(SELECT_SQL + " WHERE id = :id")
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    public List<EquipmentLoadout> findByPersonageIdAndItemId(PersonageId personageId, long itemId) {
        return jdbcClient.sql(SELECT_SQL + " WHERE personage_id = :personage_id AND :item_id = ANY(item_ids) ORDER BY id")
            .param("personage_id", personageId.value())
            .param("item_id", itemId)
            .query(this::mapRow)
            .list();
    }

    public int countByPersonageId(PersonageId personageId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM personage_equipment_loadout WHERE personage_id = :personage_id")
            .param("personage_id", personageId.value())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    public long insert(PersonageId personageId, String name, List<Long> itemIds) {
        return jdbcClient.sql("""
                INSERT INTO personage_equipment_loadout (personage_id, name, item_ids)
                VALUES (:personage_id, :name, :item_ids)
                RETURNING id
                """)
            .param("personage_id", personageId.value())
            .param("name", name)
            .param("item_ids", itemIdsArray(itemIds))
            .query((rs, _) -> rs.getLong("id"))
            .single();
    }

    public void updateItemIds(long id, List<Long> itemIds) {
        jdbcClient.sql("UPDATE personage_equipment_loadout SET item_ids = :item_ids WHERE id = :id")
            .param("id", id)
            .param("item_ids", itemIdsArray(itemIds))
            .update();
    }

    public void updateName(long id, String name) {
        jdbcClient.sql("UPDATE personage_equipment_loadout SET name = :name WHERE id = :id")
            .param("id", id)
            .param("name", name)
            .update();
    }

    public void delete(long id) {
        jdbcClient.sql("DELETE FROM personage_equipment_loadout WHERE id = :id")
            .param("id", id)
            .update();
    }

    private Array itemIdsArray(List<Long> itemIds) {
        final var ids = itemIds.toArray(Long[]::new);
        try (Connection connection = dataSource.getConnection()) {
            return connection.createArrayOf("bigint", ids);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create item_ids array", e);
        }
    }

    private EquipmentLoadout mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EquipmentLoadout(
            rs.getLong("id"),
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("name"),
            extractItemIds(rs.getArray("item_ids"))
        );
    }

    private List<Long> extractItemIds(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        final var raw = array.getArray();
        if (raw instanceof Long[] longs) {
            return Arrays.asList(longs);
        }
        if (raw instanceof Number[] numbers) {
            return Arrays.stream(numbers).map(Number::longValue).toList();
        }
        throw new IllegalStateException("Unexpected item_ids array type: " + raw.getClass());
    }

    private static final String SELECT_SQL = """
        SELECT id, personage_id, name, item_ids
        FROM personage_equipment_loadout
        """;
}
