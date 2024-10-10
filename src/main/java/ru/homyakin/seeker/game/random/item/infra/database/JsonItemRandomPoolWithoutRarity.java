package ru.homyakin.seeker.game.random.item.infra.database;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPoolWithoutRarity;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;

public record JsonItemRandomPoolWithoutRarity(
    JsonRandomPool<PersonageSlot> slotRandomPool,
    JsonRandomPool<Integer> modifierCountRandomPool
) {
    public JsonItemRandomPoolWithoutRarity {
        if (slotRandomPool == null) {
            slotRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
        if (modifierCountRandomPool == null) {
            modifierCountRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
    }

    public ItemRandomPoolWithoutRarity toDomain() {
        return new ItemRandomPoolWithoutRarity(
            new SlotRandomPool(slotRandomPool.pool()),
            new ModifierCountRandomPool(modifierCountRandomPool.pool())
        );
    }

    public static JsonItemRandomPoolWithoutRarity fromDomain(ItemRandomPoolWithoutRarity fullItemRandomPool) {
        return new JsonItemRandomPoolWithoutRarity(
            new JsonRandomPool<>(fullItemRandomPool.slotRandomPool().pool()),
            new JsonRandomPool<>(fullItemRandomPool.modifierCountRandomPool().pool())
        );
    }
}
