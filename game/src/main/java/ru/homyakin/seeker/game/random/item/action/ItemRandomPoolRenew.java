package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

@Component
public class ItemRandomPoolRenew {
    private final ItemRandomConfig config;

    public ItemRandomPoolRenew(ItemRandomConfig config) {
        this.config = config;
    }

    public ItemRandomPool fullRenewIfEmpty(ItemRandomPool fullItemRandomPool) {
        return new ItemRandomPool(
            renewSlotRandomPool(fullItemRandomPool.slotRandomPool())
        );
    }

    private SlotRandomPool renewSlotRandomPool(SlotRandomPool slotRandomPool) {
        if (slotRandomPool.isEmpty()) {
            return SlotRandomPool.generate(config.sameSlotsInPool());
        } else {
            return slotRandomPool;
        }
    }
}
