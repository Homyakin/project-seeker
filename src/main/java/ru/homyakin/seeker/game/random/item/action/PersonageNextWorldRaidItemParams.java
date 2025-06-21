package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class PersonageNextWorldRaidItemParams {
    private final ItemRandomConfig config;

    public PersonageNextWorldRaidItemParams(ItemRandomConfig config) {
        this.config = config;
    }

    public ItemParamsFull get() {
        return new ItemParamsFull(
            config.worldRaidRarityPicker().pick(RandomUtils::getWithMax),
            RandomUtils.getRandomElement(PersonageSlot.values()),
            config.worldRaidModifierCountPicker().pick(RandomUtils::getWithMax)
        );
    }
}
