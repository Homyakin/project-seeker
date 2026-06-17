package ru.homyakin.seeker.game.random.item.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class RaidItemRandomPoolPostgresRepository implements RaidItemRandomPoolRepository {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public RaidItemRandomPoolPostgresRepository(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public ItemRandomPool get(PersonageId personageId) {
        final var sql = "SELECT raid_item_random_pool FROM personage_random WHERE personage_id = :personage_id";
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional()
            .orElse(ItemRandomPool.EMPTY);
    }

    @Override
    public void save(PersonageId personageId, ItemRandomPool raidItemRandomPool) {
        final var sql = """
            INSERT INTO personage_random (personage_id, raid_item_random_pool)
            VALUES (:personage_id, :raid_item_random_pool)
            ON CONFLICT (personage_id) DO UPDATE SET raid_item_random_pool = :raid_item_random_pool
            """;
        jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param(
                "raid_item_random_pool",
                jsonUtils.mapToPostgresJson(JsonItemRandomPool.fromDomain(raidItemRandomPool))
            )
            .update();
    }

    private ItemRandomPool mapRow(ResultSet rs, int rowNum) throws SQLException {
        return jsonUtils.fromString(rs.getString("raid_item_random_pool"), JsonItemRandomPool.class).toDomain();
    }

}
