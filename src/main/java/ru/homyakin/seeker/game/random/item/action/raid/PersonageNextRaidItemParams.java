package ru.homyakin.seeker.game.random.item.action.raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemParams;
import ru.homyakin.seeker.game.random.item.action.pool.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.entity.raid.RaidItemRandomPoolRepository;

@Component
public class PersonageNextRaidItemParams {
    private final RaidItemRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;

    public PersonageNextRaidItemParams(RaidItemRandomPoolRepository repository, ItemRandomPoolRenew randomPoolRenew) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
    }

    public FullItemParams get(PersonageId personageId) {
        final var raidItemRandomPool = repository.get(personageId);

        final var updatedRaidItemRandomPool = randomPoolRenew.fullRenewIfEmpty(raidItemRandomPool);
        final var params = updatedRaidItemRandomPool.next();
        repository.save(personageId, updatedRaidItemRandomPool);

        return params;
    }
}
