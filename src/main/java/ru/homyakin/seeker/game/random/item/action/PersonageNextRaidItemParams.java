package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class PersonageNextRaidItemParams {
    private final RaidItemRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;
    private final ItemRandomConfig config;

    public PersonageNextRaidItemParams(
        RaidItemRandomPoolRepository repository,
        ItemRandomPoolRenew randomPoolRenew,
        ItemRandomConfig config
    ) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
        this.config = config;
    }

    public ItemParamsFull get(PersonageId personageId) {
        final var raidItemRandomPool = repository.get(personageId);

        final var updatedRaidItemRandomPool = randomPoolRenew.fullRenewIfEmpty(raidItemRandomPool);
        final var params = updatedRaidItemRandomPool.next(
            config.raidModifierCountPicker().pick(RandomUtils::getWithMax),
            config.raidRarityPicker().pick(RandomUtils::getWithMax)
        );
        repository.save(personageId, updatedRaidItemRandomPool);

        return params;
    }
}
