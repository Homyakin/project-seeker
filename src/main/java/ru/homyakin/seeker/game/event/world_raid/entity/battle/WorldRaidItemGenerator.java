package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class WorldRaidItemGenerator {
    private final ItemService itemService;

    public WorldRaidItemGenerator(ItemService itemService) {
        this.itemService = itemService;
    }

    public Optional<Item> generate(Personage personage, boolean isWin) {
        if (!isWin) {
            return Optional.empty();
        }

        final var itemChance = RandomUtils.getInInterval(1, 100);
        if (itemChance <= NONE_ITEM_CHANCE) {
            return Optional.empty();
        }
        final ItemRarity rarity;
        if (itemChance <= EPIC_ITEM_CHANCE + NONE_ITEM_CHANCE) {
            rarity = ItemRarity.EPIC;
        } else {
            rarity = ItemRarity.LEGENDARY;
        }
        final var modifierChance = RandomUtils.getInInterval(1, 100);
        final int modifierCount;
        if (modifierChance <= NONE_MODIFIER_CHANCE) {
            modifierCount = 0;
        } else if (modifierChance <= ONE_MODIFIER_CHANCE + NONE_MODIFIER_CHANCE) {
            modifierCount = 1;
        } else {
            modifierCount = 2;
        }
        return itemService.generateItemForPersonage(
            personage,
            new GenerateItemParams(
                rarity,
                RandomUtils.getRandomElement(PersonageSlot.values()),
                modifierCount
            )
        ).fold(
            _ -> Optional.empty(),
            Optional::of
        );
    }

    private static final int NONE_ITEM_CHANCE = 20;
    private static final int EPIC_ITEM_CHANCE = 30;
    private static final int LEGENDARY_ITEM_CHANCE = 50;
    private static final int NONE_MODIFIER_CHANCE = 20;
    private static final int ONE_MODIFIER_CHANCE = 30;
    private static final int TWO_MODIFIERS_CHANCE = 50;
}
