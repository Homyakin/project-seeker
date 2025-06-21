package ru.homyakin.seeker.game.random.item.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ShopRandomPoolRepository;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
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
    public ItemRandomPool getPool(PersonageId personageId, ShopItemType itemType) {
        final var sql = "SELECT ${column} as random_pool FROM personage_random WHERE personage_id = :personage_id";
        return jdbcClient.sql(StringNamedTemplate.format(sql, Collections.singletonMap("column", typeToColumn(itemType))))
            .param("personage_id", personageId.value())
            .query((rs, _) -> jsonUtils.fromString(rs.getString("random_pool"), JsonItemRandomPool.class).toDomain())
            .optional()
            .orElse(ItemRandomPool.EMPTY);
    }

    @Override
    public void savePool(PersonageId personageId, ShopItemType itemType, ItemRandomPool itemRandomPool) {
        final var sql = """
            INSERT INTO personage_random (personage_id, ${column})
            VALUES (:personage_id, :random_pool)
            ON CONFLICT (personage_id) DO UPDATE SET ${column} = :random_pool
            """;
        jdbcClient.sql(StringNamedTemplate.format(sql, Collections.singletonMap("column", typeToColumn(itemType))))
            .param("personage_id", personageId.value())
            .param("random_pool", jsonUtils.mapToPostgresJson(JsonItemRandomPool.fromDomain(itemRandomPool)))
            .update();
    }

    private String typeToColumn(ShopItemType itemType) {
        return switch (itemType) {
            case COMMON -> "shop_common_item_random_pool";
            case UNCOMMON -> "shop_uncommon_item_random_pool";
            case RARE -> "shop_rare_item_random_pool";
            case EPIC -> "shop_epic_item_random_pool";
            case LEGENDARY -> "shop_legendary_item_random_pool";
            case RANDOM -> "shop_random_item_random_pool";
        };
    }
}
