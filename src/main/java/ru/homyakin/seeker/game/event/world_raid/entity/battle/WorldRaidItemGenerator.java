package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.random.item.action.PersonageNextWorldRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class WorldRaidItemGenerator {
    private final ItemService itemService;
    private final PersonageNextWorldRaidItemParams personageNextWorldRaidItemParams;

    public WorldRaidItemGenerator(
        ItemService itemService,
        PersonageNextWorldRaidItemParams personageNextWorldRaidItemParams
    ) {
        this.itemService = itemService;
        this.personageNextWorldRaidItemParams = personageNextWorldRaidItemParams;
    }

    public Optional<Item> generate(Personage personage, boolean isWin) {
        if (!isWin) {
            return Optional.empty();
        }

        if (RandomUtils.processChance(NONE_ITEM_CHANCE)) {
            return Optional.empty();
        }
        final var params = personageNextWorldRaidItemParams.get();
        return itemService.generateItemForPersonage(
            personage,
            new GenerateItemParams(
                params.rarity(),
                params.slot(),
                params.modifiersCount()
            )
        ).fold(
            _ -> Optional.empty(),
            Optional::of
        );
    }

    private static final int NONE_ITEM_CHANCE = 10;
}
