package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.RaidLevelItemConfig;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class PersonageNextRaidItemParams {
    private final RaidItemRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;
    private final RaidLevelItemConfig raidLevelItemConfig;

    public PersonageNextRaidItemParams(
        RaidItemRandomPoolRepository repository,
        ItemRandomPoolRenew randomPoolRenew,
        RaidLevelItemConfig raidLevelItemConfig
    ) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
        this.raidLevelItemConfig = raidLevelItemConfig;
    }

    public ItemParamsFull get(PersonageId personageId, int raidLevel) {
        final var raidItemRandomPool = repository.get(personageId);

        final var updatedRaidItemRandomPool = randomPoolRenew.fullRenewIfEmpty(raidItemRandomPool);
        final var params = updatedRaidItemRandomPool.next(
            raidLevelItemConfig.getRarityPickerForLevel(raidLevel).pick(RandomUtils::getWithMax)
        );
        repository.save(personageId, updatedRaidItemRandomPool);

        return params;
    }
}
