package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.RaidLevelItemConfig;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class PersonageNextRaidItemParams {
    private final RaidItemRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;
    private final ItemRandomConfig config;
    private final RaidLevelItemConfig raidLevelItemConfig;

    public PersonageNextRaidItemParams(
        RaidItemRandomPoolRepository repository,
        ItemRandomPoolRenew randomPoolRenew,
        ItemRandomConfig config,
        RaidLevelItemConfig raidLevelItemConfig
    ) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
        this.config = config;
        this.raidLevelItemConfig = raidLevelItemConfig;
    }

    public ItemParamsFull get(PersonageId personageId, int raidLevel) {
        final var raidItemRandomPool = repository.get(personageId);

        final var updatedRaidItemRandomPool = randomPoolRenew.fullRenewIfEmpty(raidItemRandomPool);
        final var params = updatedRaidItemRandomPool.next(
            config.raidModifierCountPicker().pick(RandomUtils::getWithMax),
            raidLevelItemConfig.getRarityPickerForLevel(raidLevel).pick(RandomUtils::getWithMax)
        );
        repository.save(personageId, updatedRaidItemRandomPool);

        return params;
    }
}
