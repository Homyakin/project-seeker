package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.random.item.action.PersonageNextWorldRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class WorldRaidItemGenerator {
    private final ItemService itemService;
    private final PersonageService personageService;
    private final PersonageNextWorldRaidItemParams personageNextWorldRaidItemParams;

    public WorldRaidItemGenerator(
        ItemService itemService,
        PersonageService personageService,
        PersonageNextWorldRaidItemParams personageNextWorldRaidItemParams
    ) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.personageNextWorldRaidItemParams = personageNextWorldRaidItemParams;
    }

    public Optional<Item> generate(Personage personage, boolean isWin) {
        if (!isWin) {
            return Optional.empty();
        }

        // Базовый шанс 70%, с каждым мировым рейдом шанс увеличивается на 10 (не важно победа или проигрыш)
        // При учете, что на мировой рейд надо 2 попытки, то гарант будет на вторую победу
        // А если человек участвует реже, то и гарант будет реже
        // Механика призвана повысить посещаемость мировых рейдов
        final var itemChance = BASE_ITEM_CHANCE + 10 * personageService.countWorldRaidsFromLastItem(personage.id());
        if (RandomUtils.processChance(itemChance)) {
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

    private static final int BASE_ITEM_CHANCE = 70;
}
