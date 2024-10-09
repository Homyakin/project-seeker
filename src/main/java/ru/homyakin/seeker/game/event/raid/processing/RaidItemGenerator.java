package ru.homyakin.seeker.game.event.raid.processing;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.random.item.action.raid.PersonageNextRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class RaidItemGenerator {
    private final PersonageService personageService;
    private final ItemService itemService;
    private final PersonageNextRaidItemParams personageNextRaidItemParams;

    public RaidItemGenerator(
        PersonageService personageService,
        ItemService itemService,
        PersonageNextRaidItemParams personageNextRaidItemParams
    ) {
        this.personageService = personageService;
        this.itemService = itemService;
        this.personageNextRaidItemParams = personageNextRaidItemParams;
    }

    /**
     * Функция генерации предметов для персонажа
     * В основе лежит функция:
     * Если x <= 5 => y = 2*x (нужно, потому что степенные функции в начале растут очень медленно)
     * Если x > 5 => y = 10 + ((x - 5)^2) / 2.5
     * x - количество рейдов подряд без предметов
     * y - вероятность получить предмет в процентах
     */
    public Optional<GeneratedItemResult> generateItem(
        boolean isWin,
        Personage personage,
        boolean isExhausted
    ) {
        if (!isWin) {
            return Optional.empty();
        }
        if (isExhausted) {
            return Optional.empty();
        }
        final var raidsWithoutItems = personageService.countSuccessRaidsFromLastItem(personage.id());
        final int chance;
        if (raidsWithoutItems <= 5) {
            chance = raidsWithoutItems * 2;
        } else {
            chance = (int) (10 + Math.pow(raidsWithoutItems - 5, 2) / 2.5);
        }
        if (RandomUtils.processChance(chance)) {
            final var itemParams = personageNextRaidItemParams.get(personage.id());
            final var result = itemService
                .generateItemForPersonage(
                    personage,
                    new GenerateItemParams(
                        itemParams.rarity(),
                        itemParams.slot(),
                        itemParams.modifiersCount()
                    )
                )
                .fold(
                    error -> switch (error) {
                        case GenerateItemError.NotEnoughSpace notEnoughSpace ->
                            new GeneratedItemResult.NotEnoughSpaceInBag(personage, notEnoughSpace.item());
                    },
                    item -> new GeneratedItemResult.Success(personage, item)
                );
            return Optional.of(result);
        }
        return Optional.empty();
    }
}
