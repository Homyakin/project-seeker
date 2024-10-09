package ru.homyakin.seeker.game.random.item.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;

import java.util.HashMap;

public class RarityRandomPoolTest {
    @Test
    public void When_GeneratePoolWithParams_Then_PoolContainsRequiredCount() {
        final var params = new HashMap<ItemRarity, Integer>();
        params.put(ItemRarity.COMMON, 5);
        params.put(ItemRarity.UNCOMMON, 4);
        params.put(ItemRarity.RARE, 3);
        params.put(ItemRarity.EPIC, 2);
        params.put(ItemRarity.LEGENDARY, 1);

        final var pool = RarityRandomPool.generate(params);

        final var resultCommon = pool.pool().stream().filter(it -> it == ItemRarity.COMMON).count();
        final var resultUncommon = pool.pool().stream().filter(it -> it == ItemRarity.UNCOMMON).count();
        final var resultRare = pool.pool().stream().filter(it -> it == ItemRarity.RARE).count();
        final var resultEpic = pool.pool().stream().filter(it -> it == ItemRarity.EPIC).count();
        final var resultLegendary = pool.pool().stream().filter(it -> it == ItemRarity.LEGENDARY).count();

        Assertions.assertEquals(params.get(ItemRarity.COMMON).longValue(), resultCommon);
        Assertions.assertEquals(params.get(ItemRarity.UNCOMMON).longValue(), resultUncommon);
        Assertions.assertEquals(params.get(ItemRarity.RARE).longValue(), resultRare);
        Assertions.assertEquals(params.get(ItemRarity.EPIC).longValue(), resultEpic);
        Assertions.assertEquals(params.get(ItemRarity.LEGENDARY).longValue(), resultLegendary);
    }
}
