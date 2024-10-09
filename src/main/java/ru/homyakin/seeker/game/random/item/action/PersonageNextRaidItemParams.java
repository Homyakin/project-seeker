package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.SlotRandomPool;
import ru.homyakin.seeker.game.random.item.entity.FullItemParams;
import ru.homyakin.seeker.game.random.item.entity.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;

@Component
public class PersonageNextRaidItemParams {
    private final RaidItemRandomPoolRepository raidItemRandomPoolRepository;
    private final ItemRandomConfig config;

    public PersonageNextRaidItemParams(RaidItemRandomPoolRepository raidItemRandomPoolRepository, ItemRandomConfig config) {
        this.raidItemRandomPoolRepository = raidItemRandomPoolRepository;
        this.config = config;
    }

    public FullItemParams get(PersonageId personageId) {
        final var raidItemRandomPool = raidItemRandomPoolRepository.get(personageId);

        final var updatedRaidItemRandomPool = new FullItemRandomPool(
            extractRarityRandomPool(raidItemRandomPool),
            extractSlotRandomPool(raidItemRandomPool),
            extractModifierCountRandomPool(raidItemRandomPool)
        );

        final var params = updatedRaidItemRandomPool.next();

        raidItemRandomPoolRepository.save(personageId, updatedRaidItemRandomPool);
        return params;
    }

    private RarityRandomPool extractRarityRandomPool(FullItemRandomPool raidItemRandomPool) {
        if (raidItemRandomPool.rarityRandomPool().isEmpty()) {
            return RarityRandomPool.generate(config.raritiesInPool());
        } else {
            return raidItemRandomPool.rarityRandomPool();
        }
    }

    private SlotRandomPool extractSlotRandomPool(FullItemRandomPool raidItemRandomPool) {
        if (raidItemRandomPool.slotRandomPool().isEmpty()) {
            return SlotRandomPool.generate(config.sameSlotsInPool());
        } else {
            return raidItemRandomPool.slotRandomPool();
        }
    }

    private ModifierCountRandomPool extractModifierCountRandomPool(FullItemRandomPool raidItemRandomPool) {
        if (raidItemRandomPool.modifierCountRandomPool().isEmpty()) {
            final var settings = config.modifierPoolSettings();
            return ModifierCountRandomPool.generate(
                settings.zeroModifiersInPool(),
                settings.oneModifiersInPool(),
                settings.twoModifiersInPool()
            );
        } else {
            return raidItemRandomPool.modifierCountRandomPool();
        }
    }
}
