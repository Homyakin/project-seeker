package ru.homyakin.seeker.game.random.item.action.pool;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

@Component
public class ItemRandomPoolRenew {
    private final ItemRandomConfig config;

    public ItemRandomPoolRenew(ItemRandomConfig config) {
        this.config = config;
    }

    public FullItemRandomPool renewIfEmpty(FullItemRandomPool fullItemRandomPool) {
        return new FullItemRandomPool(
            renewRarityRandomPool(fullItemRandomPool.rarityRandomPool()),
            renewSlotRandomPool(fullItemRandomPool.slotRandomPool()),
            renewModifierCountRandomPool(fullItemRandomPool.modifierCountRandomPool())
        );
    }

    private RarityRandomPool renewRarityRandomPool(RarityRandomPool rarityRandomPool) {
        if (rarityRandomPool.isEmpty()) {
            return RarityRandomPool.generate(config.raritiesInPool());
        } else {
            return rarityRandomPool;
        }
    }

    private SlotRandomPool renewSlotRandomPool(SlotRandomPool slotRandomPool) {
        if (slotRandomPool.isEmpty()) {
            return SlotRandomPool.generate(config.sameSlotsInPool());
        } else {
            return slotRandomPool;
        }
    }

    private ModifierCountRandomPool renewModifierCountRandomPool(ModifierCountRandomPool modifierCountRandomPool) {
        if (modifierCountRandomPool.isEmpty()) {
            final var settings = config.modifierPoolSettings();
            return ModifierCountRandomPool.generate(
                settings.zeroModifiersInPool(),
                settings.oneModifiersInPool(),
                settings.twoModifiersInPool()
            );
        } else {
            return modifierCountRandomPool;
        }
    }
}
