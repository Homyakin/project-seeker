package ru.homyakin.seeker.game.random.item.infra.database;

import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;

public record JsonItemRandomPool(
    JsonRandomPool<ItemRarity> rarityRandomPool,
    JsonRandomPool<PersonageSlot> slotRandomPool,
    JsonRandomPool<Integer> modifierCountRandomPool
) {
    public JsonItemRandomPool {
        if (rarityRandomPool == null) {
            rarityRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
        if (slotRandomPool == null) {
            slotRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
        if (modifierCountRandomPool == null) {
            modifierCountRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
    }

    public FullItemRandomPool toDomain() {
        return new FullItemRandomPool(
            new RarityRandomPool(rarityRandomPool.pool()),
            new SlotRandomPool(slotRandomPool.pool()),
            new ModifierCountRandomPool(modifierCountRandomPool.pool())
        );
    }

    public static JsonItemRandomPool fromDomain(FullItemRandomPool fullItemRandomPool) {
        return new JsonItemRandomPool(
            new JsonRandomPool<>(fullItemRandomPool.rarityRandomPool().pool()),
            new JsonRandomPool<>(fullItemRandomPool.slotRandomPool().pool()),
            new JsonRandomPool<>(fullItemRandomPool.modifierCountRandomPool().pool())
        );
    }
}
