package ru.homyakin.seeker.game.event.raid.processing;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.LegacyItemService;
import ru.homyakin.seeker.game.item.errors.LegacyGenerateItemError;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemParams;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.random.item.action.PersonageNextRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class RaidItemGenerator {
    private final PersonageService personageService;
    private final LegacyItemService itemService;
    private final PersonageNextRaidItemParams personageNextRaidItemParams;
    private final ContrabandService contrabandService;

    public RaidItemGenerator(
        PersonageService personageService,
        LegacyItemService itemService,
        PersonageNextRaidItemParams personageNextRaidItemParams,
        ContrabandService contrabandService
    ) {
        this.personageService = personageService;
        this.itemService = itemService;
        this.personageNextRaidItemParams = personageNextRaidItemParams;
        this.contrabandService = contrabandService;
    }

    /**
     * Функция генерации предметов для персонажа
     * В основе лежит функция:
     * Если x <= 5 => y = x
     * Если x > 5 => y = (x - 3)^(1.8)
     * x - количество рейдов подряд без предметов
     * y - вероятность получить предмет в процентах
     * <p>
     * Вместо предмета может выпасть контрабанда (определяется в ContrabandService).
     */
    /**
     * @param itemFoundChanceBonusPercent sum of {@code ItemFoundChancePercent} from personage buffs and group passives
     */
    public Optional<GeneratedItemResult> generateItem(
        boolean isWin,
        Personage personage,
        boolean isExhausted,
        int raidLevel,
        int itemFoundChanceBonusPercent
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
            chance = raidsWithoutItems;
        } else {
            chance = (int) Math.pow(raidsWithoutItems - 3, 1.8);
        }
        final var contrabandOpt = contrabandService.tryCreate(personage, raidLevel);
        if (contrabandOpt.isPresent()) {
            return Optional.of(new GeneratedItemResult.ContrabandDrop(personage, contrabandOpt.get()));
        }
        if (RandomUtils.processChance(chance + itemFoundChanceBonusPercent)) {
            final var itemParams = personageNextRaidItemParams.get(personage.id(), raidLevel);
            final var result = itemService
                .generateItemForPersonage(
                    personage,
                    new LegacyGenerateItemParams(
                        itemParams.rarity(),
                        itemParams.slot(),
                        itemParams.modifiersCount()
                    )
                )
                .fold(
                    error -> switch (error) {
                        case LegacyGenerateItemError.NotEnoughSpace notEnoughSpace ->
                            new GeneratedItemResult.NotEnoughSpaceInBag(personage, notEnoughSpace.item());
                    },
                    item -> new GeneratedItemResult.Success(personage, item)
                );
            return Optional.of(result);
        }
        return Optional.empty();
    }
}
