package ru.homyakin.seeker.game.random.item.infra.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonItemRandomPool(
    JsonRandomPool<PersonageSlot> slotRandomPool
) {
    public JsonItemRandomPool {
        if (slotRandomPool == null) {
            slotRandomPool = new JsonRandomPool<>(new LinkedList<>());
        }
    }

    public ItemRandomPool toDomain() {
        return new ItemRandomPool(
            new SlotRandomPool(slotRandomPool.pool())
        );
    }

    public static JsonItemRandomPool fromDomain(ItemRandomPool fullItemRandomPool) {
        return new JsonItemRandomPool(
            new JsonRandomPool<>(fullItemRandomPool.slotRandomPool().pool())
        );
    }
}
