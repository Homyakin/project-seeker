package ru.homyakin.seeker.game.random.item.infra.database.shop;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPoolWithoutRarity;
import ru.homyakin.seeker.game.random.item.entity.shop.ShopRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.infra.database.JsonItemRandomPool;
import ru.homyakin.seeker.game.random.item.infra.database.JsonItemRandomPoolWithoutRarity;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import javax.sql.DataSource;
import java.util.Collections;

@Repository
public class ShopRandomPoolPostgresRepository implements ShopRandomPoolRepository {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ShopRandomPoolPostgresRepository(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public FullItemRandomPool getRandomPool(PersonageId personageId) {
        final var sql = "SELECT shop_random_item_random_pool FROM personage_random WHERE personage_id = :personage_id";
        return jdbcClient
            .sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, _) -> jsonUtils.fromString(rs.getString("shop_random_item_random_pool"), JsonItemRandomPool.class).toDomain())
            .optional()
            .orElse(FullItemRandomPool.EMPTY);
    }

    @Override
    public void saveRandomPool(PersonageId personageId, FullItemRandomPool itemRandomPool) {
        final var sql = """
            INSERT INTO personage_random (personage_id, shop_random_item_random_pool)
            VALUES (:personage_id, :random_pool)
            ON CONFLICT (personage_id) DO UPDATE SET shop_random_item_random_pool = :random_pool
            """;

        jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("random_pool", jsonUtils.mapToPostgresJson(JsonItemRandomPool.fromDomain(itemRandomPool)))
            .update();
    }

    @Override
    public ItemRandomPoolWithoutRarity getRarityPool(PersonageId personageId, ItemRarity rarity) {
        @Language("SQL")
        final var sql = "SELECT ${column} as random_pool FROM personage_random WHERE personage_id = :personage_id";
        return jdbcClient.sql(StringNamedTemplate.format(sql, Collections.singletonMap("column", rarityToColumn(rarity))))
            .param("personage_id", personageId.value())
            .query((rs, _) -> jsonUtils.fromString(rs.getString("random_pool"), JsonItemRandomPoolWithoutRarity.class).toDomain())
            .optional()
            .orElse(ItemRandomPoolWithoutRarity.EMPTY);
    }

    @Override
    public void saveRarityPool(PersonageId personageId, ItemRarity rarity, ItemRandomPoolWithoutRarity itemRandomPool) {
        final var sql = """
            INSERT INTO personage_random (personage_id, ${column})
            VALUES (:personage_id, :random_pool)
            ON CONFLICT (personage_id) DO UPDATE SET ${column} = :random_pool
            """;
        jdbcClient.sql(StringNamedTemplate.format(sql, Collections.singletonMap("column", rarityToColumn(rarity))))
            .param("personage_id", personageId.value())
            .param("random_pool", jsonUtils.mapToPostgresJson(JsonItemRandomPoolWithoutRarity.fromDomain(itemRandomPool)))
            .update();
    }

    private String rarityToColumn(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> "shop_common_item_random_pool";
            case UNCOMMON -> "shop_uncommon_item_random_pool";
            case RARE -> "shop_rare_item_random_pool";
            case EPIC -> "shop_epic_item_random_pool";
            case LEGENDARY -> "shop_legendary_item_random_pool";
        };
    }
}
