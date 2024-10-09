package ru.homyakin.seeker.game.random.item.infra.raid;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.raid.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.pool.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;

@Repository
public class RaidItemRandomPoolPostgresRepository implements RaidItemRandomPoolRepository {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public RaidItemRandomPoolPostgresRepository(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public FullItemRandomPool get(PersonageId personageId) {
        final var sql = "SELECT * FROM personage_random WHERE personage_id = :personage_id";
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query(this::mapRow)
            .optional()
            .orElse(FullItemRandomPool.EMPTY);
    }

    @Override
    public void save(PersonageId personageId, FullItemRandomPool raidItemRandomPool) {
        final var sql = """
            INSERT INTO personage_random (personage_id, raid_item_random_pool)
            VALUES (:personage_id, :raid_item_random_pool)
            ON CONFLICT (personage_id) DO UPDATE SET raid_item_random_pool = :raid_item_random_pool
            """;
        jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param(
                "raid_item_random_pool",
                jsonUtils.mapToPostgresJson(JsonRaidRandomPool.fromDomain(raidItemRandomPool))
            )
            .update();
    }

    private FullItemRandomPool mapRow(ResultSet rs, int rowNum) throws SQLException {
        return jsonUtils.fromString(rs.getString("raid_item_random_pool"), JsonRaidRandomPool.class).toDomain();
    }

    private record JsonRaidRandomPool(
        JsonRandomPool<ItemRarity> rarityRandomPool,
        JsonRandomPool<PersonageSlot> slotRandomPool,
        JsonRandomPool<Integer> modifierCountRandomPool
    ) {
        public FullItemRandomPool toDomain() {
            return new FullItemRandomPool(
                new RarityRandomPool(rarityRandomPool.pool),
                new SlotRandomPool(slotRandomPool.pool),
                new ModifierCountRandomPool(modifierCountRandomPool.pool)
            );
        }

        public static JsonRaidRandomPool fromDomain(FullItemRandomPool fullItemRandomPool) {
            return new JsonRaidRandomPool(
                new JsonRandomPool<>(fullItemRandomPool.rarityRandomPool().pool()),
                new JsonRandomPool<>(fullItemRandomPool.slotRandomPool().pool()),
                new JsonRandomPool<>(fullItemRandomPool.modifierCountRandomPool().pool())
            );
        }
    }

    private record JsonRandomPool<T>(
        Queue<T> pool
    ) {
    }
}
